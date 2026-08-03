---
name: gestao-jwks-local
description: >
  Esta é a documentação autoritativa do domínio Gestão do JWKS local. Cobre como o
  resource server mantém o conjunto de chaves públicas (JWKS) do Keycloak em arquivo
  local para validação offline de assinatura: leitura do arquivo, cache em memória,
  recarga automática (hot-reload) por mudança de mtime, fail-fast na subida, rastreio
  de há quanto tempo o JWKS foi carregado (staleness) e o contrato com o processo
  externo de sincronização. Carregue em tarefas sobre JWKS local, chaves públicas do
  Keycloak, rotação de chave, kid desconhecido, arquivo jwks.json, caminho do JWKS
  (APP_B_JWKS_PATH), recarga/hot-reload do JWKS, JWKS desatualizado/stale ou o script
  de sincronização do JWKS.
metadata:
  author: clovis-cli
  type: domain-skill
---

# Gestão do JWKS local

> **Manutenção desta skill**
>
> Atualize este documento sempre que o comportamento do domínio mudar de propósito,
> mantendo a skill fiel ao comportamento implementado. Uma mudança deliberada de regra
> (novo critério de recarga, nova política de falha, novo campo do contrato) é
> maintenance e deve ser refletida aqui. Uma divergência semântica entre skill e código
> sem decisão registrada que a resolva é escalada para decisão humana — nunca ajuste a
> skill nem o código por conta própria.

## Visão geral do domínio

Este domínio é responsável por **manter disponível localmente o conjunto de chaves
públicas (JWKS) do Keycloak** usado para verificar a assinatura dos access tokens (JWT).
Ele é o domínio-base do sistema: sem o material de chaves carregado, nenhuma assinatura
pode ser verificada e nenhuma requisição autentica.

A decisão central é **validação offline**: as chaves são lidas de um **arquivo local**,
nunca buscadas por chamada HTTP ao Keycloak em tempo de requisição. A motivação de
negócio declarada é que o resource server roda em um segmento de rede **sem saída direta
para o Keycloak**; um processo separado sincroniza o arquivo e a aplicação apenas o lê.

O escopo deste domínio termina em **fornecer as chaves**: carregar, cachear, recarregar e
expor a idade do carregamento. A verificação criptográfica da assinatura, a validação de
`iss`/`aud`/tempo e a exposição de qualquer endpoint pertencem a outros domínios e não são
descritas aqui.

## Regras de negócio

As regras abaixo usam numeração própria desta skill, apenas para referência de leitura.

1. **Origem exclusiva em arquivo local.** O JWKS é sempre lido de um arquivo no sistema
   de arquivos local, apontado por configuração. É **proibido** buscar o JWKS por rede
   (endpoint de certs do Keycloak) durante a validação de tokens. O caminho padrão do
   arquivo é `/etc/app-b/jwks/jwks.json`, sobrescrevível por configuração.

2. **Cache em memória.** Após a leitura, o conjunto de chaves fica cacheado em memória e
   é servido a partir dela em cada verificação, sem tocar o disco na maioria das
   requisições. As seleções de chave (por exemplo, por `kid`) são resolvidas contra esse
   cache.

3. **Fail-fast na subida.** Na inicialização da aplicação, o arquivo é carregado
   imediatamente. Se o arquivo **não existir**, não puder ser lido ou tiver conteúdo
   inválido (não for um JWKS parseável), a aplicação **falha ao subir** — não há
   inicialização em estado degradado sem chaves.

4. **Recarga automática por mudança do arquivo (hot-reload).** A cada uso do material de
   chaves, verifica-se o *last modified time* (mtime) do arquivo. Se o mtime mudou em
   relação ao último carregamento bem-sucedido, o arquivo é **recarregado
   automaticamente**. Consequência de negócio: **após uma rotação de chave no Keycloak
   não é necessário reiniciar a aplicação**, desde que o processo de sincronização
   atualize o arquivo a tempo.

5. **Recarga segura sob concorrência.** A detecção de "arquivo mudou" é feita sem bloqueio
   (leitura barata do mtime); ao detectar mudança, apenas **uma** recarga acontece por vez
   e o mtime é reconferido antes de recarregar (double-check), para que múltiplas
   requisições simultâneas não disparem recargas redundantes.

6. **Tolerância a falha transitória de leitura.** Se a **checagem do mtime** falhar por
   erro transitório de I/O (arquivo momentaneamente indisponível, bloqueado ou sem
   permissão naquele instante), a aplicação **mantém em memória a última versão válida** e
   registra um aviso — a requisição não é derrubada por isso. A última versão válida
   continua servindo até que uma nova leitura tenha sucesso.

7. **Falha ao recarregar um arquivo já detectado como alterado.** Se o mtime indicou
   mudança mas o novo conteúdo do arquivo estiver **inválido/corrompido** (não parseável),
   a recarga falha e o erro é propagado. Por isso o contrato com o processo de
   sincronização (escrita atômica + validação de conteúdo antes de publicar — ver
   Integrações) é o que **previne** que um arquivo parcial ou corrompido chegue a ser lido
   em operação normal.

8. **Rastreio de idade do carregamento (staleness).** A aplicação registra o instante do
   **último carregamento bem-sucedido** do JWKS e o expõe internamente. O valor inicial,
   antes de qualquer carga, é a época zero (`1970-01-01T00:00:00Z`); a cada recarga
   bem-sucedida ele passa a ser o instante corrente. O propósito é permitir monitorar
   "staleness" — há quanto tempo as chaves não são atualizadas.

9. **Nunca registrar material sensível.** Os logs deste domínio registram apenas
   metadados não sensíveis: caminho do arquivo e **quantidade** de chaves carregadas. É
   proibido logar o conteúdo das chaves, tokens ou qualquer segredo.

## Fluxos e ciclo de vida

**Carga inicial (subida da aplicação):** ao inicializar, o domínio lê o arquivo, faz o
parse do JWKS, popula o cache, grava o mtime e marca o instante do carregamento. Falha
aqui aborta a subida (regra 3).

**Verificação em requisição (caminho quente):**
1. Chega um pedido de chave(s) (disparado pela validação de um token).
2. Lê-se o mtime atual do arquivo.
3. **Se o mtime não mudou** → serve as chaves diretamente do cache.
4. **Se o mtime mudou** → adquire o lock, reconfere o mtime e, se ainda diferente,
   recarrega o arquivo (atualiza cache, mtime e instante de carga); em seguida serve as
   chaves.
5. **Se a checagem do mtime falhar por I/O transitório** → mantém e serve a última versão
   válida do cache (regra 6).

**Rotação de chave no Keycloak (fluxo operacional):** o Keycloak passa a assinar com uma
nova chave (novo `kid`) → o processo externo de sincronização busca o JWKS atualizado e
regrava o arquivo → o mtime muda → na próxima verificação o domínio recarrega
automaticamente e passa a reconhecer o novo `kid`. Nenhum reinício é necessário.

**Cenário de risco — JWKS desatualizado (stale):** se o processo de sincronização
**parar ou falhar** e o Keycloak rotacionar a chave de assinatura, o arquivo local fica
desatualizado; tokens novos, assinados com um `kid` que não está no arquivo, passam a ser
**rejeitados** mesmo sendo legítimos. O rastreio de idade do carregamento (regra 8) existe
justamente para permitir alarmar sobre esse cenário.

## Entidades e dados

**Arquivo JWKS (`jwks.json`).** É o contrato de dados do domínio: um documento JSON no
formato JWKS padrão, com um campo `keys` contendo uma lista **não vazia** de chaves
públicas (JWK). Cada chave carrega, entre outros, seu identificador `kid`, usado depois
para selecionar a chave correta ao verificar a assinatura de um token. O arquivo é
produzido pelo processo externo de sincronização a partir do endpoint de certs do Keycloak.

**Estado interno de carregamento.** O domínio mantém, em memória: o conjunto de chaves
cacheado, o último mtime observado do arquivo e o instante do último carregamento
bem-sucedido (exposto para fins de monitoração de staleness).

Este domínio **não expõe endpoints HTTP próprios**. O material de chaves é consumido
internamente pelo domínio de autenticação/validação do token.

## Restrições e validações

- O caminho do arquivo JWKS é **obrigatório** e deve apontar para um arquivo legível e
  parseável na subida (regra 3).
- O conteúdo deve ser um JWKS válido com pelo menos uma chave; a garantia de conteúdo
  válido e não vazio é responsabilidade do processo de sincronização, que valida antes de
  publicar o arquivo (ver Integrações).
- A recarga é acionada **apenas** por mudança de mtime — não há polling por conteúdo nem
  intervalo fixo de expiração do cache dentro da aplicação; a periodicidade de atualização
  do material de chaves é definida pela cadência do processo externo de sincronização.
- O domínio **não** decide sobre validade do token nem sobre qual chave é a correta para
  um dado token; apenas disponibiliza o conjunto de chaves para seleção.

## Variáveis de ambiente do domínio

- **`APP_B_JWKS_PATH`** (propriedade `app.security.jwt.jwks-path`) — caminho do arquivo
  local de onde o JWKS é lido. Valor padrão: `/etc/app-b/jwks/jwks.json`. É o ponto de
  integração com o processo de sincronização: precisa ser o mesmo arquivo/pasta que esse
  processo grava. Se o arquivo apontado não existir ou for inválido na subida, a aplicação
  não inicia (regra 3).

As variáveis `KEYCLOAK_ISSUER_URI` e o diretório de saída consumidos pelo **processo
externo de sincronização** não são configuração da aplicação e sim do script de sync; são
descritas em Integrações e no arquivo de dependências técnicas, não como variáveis deste
domínio.

## Integrações e dependências externas

- **Keycloak** — fonte original das chaves públicas, exposta em
  `GET {issuer}/protocol/openid-connect/certs`. É consumido **pelo processo de
  sincronização**, nunca pela aplicação diretamente.
- **Processo externo de sincronização do JWKS** — busca o JWKS no endpoint de certs do
  Keycloak e grava o arquivo local de forma **atômica** (escrita em arquivo temporário no
  mesmo diretório seguida de `mv`), somente após **validar** que o conteúdo é um JSON com
  a lista `keys` não vazia. Em falha (HTTP diferente de 200 ou conteúdo inválido), aborta e
  **preserva o arquivo anterior**. É executado de forma agendada (por exemplo, a cada 5–15
  minutos) e/ou após rotação manual de chave. Esse contrato de escrita atômica + validação
  é o que garante que a aplicação nunca leia um arquivo parcial ou corrompido durante o
  hot-reload (ver regra 7).
- **Sistema de arquivos / volume do caminho do JWKS** — a pasta/arquivo apontada por
  `APP_B_JWKS_PATH` precisa existir e ser legível pela aplicação; é o contrato físico de
  integração com o processo de sincronização.

O detalhamento de cada dependência técnica e o impacto de sua ausência estão em
`references/technical-dependencies.md`.

## Débito técnico conhecido

- **Health indicator de "JWKS desatualizado" não implementado.** O instante do último
  carregamento do JWKS é **intencionalmente exposto** no código para virar um health
  indicator customizado (em `/actuator/health`) que alarme quando o JWKS estiver "velho"
  demais. Essa ligação com a observabilidade **ainda não foi implementada/conectada** —
  trata-se de melhoria conhecida e deliberadamente adiada. Risco: enquanto não existir, o
  cenário de risco de staleness (processo de sync parado + rotação de chave no Keycloak,
  levando à rejeição de tokens legítimos) **não é sinalizado automaticamente** pela
  aplicação, dependendo de monitoração externa (idade do arquivo, falhas do processo de
  sync).

## Referências

- `references/technical-dependencies.md` — dependências técnicas do domínio e o efeito da
  ausência de cada uma.
