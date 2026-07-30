---
name: autenticacao-maquina-a-maquina
description: >
  Esta é a documentação autoritativa do domínio Autenticação Máquina-a-Máquina.
  Descreve como a aplicação autentica a si mesma no Keycloak pelo fluxo OAuth2
  client_credentials (sem usuário ou sessão), autenticando o cliente por Signed JWT
  (private_key_jwt) com um par de chaves gerado, rotacionado e persistido pela própria
  aplicação e publicado via JWKS, obtendo e renovando um access token JWT que autoriza
  chamadas a serviços protegidos a jusante, incluindo o escopo app-b.invoke, a descoberta
  OpenID Connect via issuer-uri e a manutenção do token em memória. Carregue esta skill em
  tarefas sobre autenticação de serviço, credencial máquina-a-máquina (M2M),
  client_credentials, autenticação por Signed JWT/private_key_jwt, client_assertion,
  geração/rotação/persistência do par de chaves, JWKS da aplicação, obtenção ou renovação
  de token, access token JWT, client-id, escopo do token, issuer-uri ou realm do Keycloak.
metadata:
  author: clovis-cli
  type: domain-skill
---

# Autenticação Máquina-a-Máquina

> **Manutenção desta skill**
>
> Atualize este documento sempre que o comportamento do domínio mudar de propósito
> (novo passo do fluxo, novo critério de obtenção/renovação do token, mudança de
> escopo, credencial ou emissor), mantendo a skill fiel ao comportamento
> efetivamente implementado. Refatoração técnica que preserva a regra (renomear,
> extrair configuração, trocar biblioteca equivalente) não exige alteração. Quando a
> skill afirmar X e o código fizer Y sem uma decisão registrada que resolva a
> divergência, trate como lacuna e escale para decisão humana — não ajuste a skill
> nem o código por conta própria.

## Visão geral do domínio

Este domínio autentica **a própria aplicação** (não um usuário final) junto ao
Keycloak, usando o fluxo OAuth2 **`client_credentials`** (máquina-a-máquina, M2M).
Não há usuário, login interativo nem sessão HTTP: a aplicação apresenta a sua
identidade de cliente (um **client-id** mais um **par de chaves** próprio) e, autenticando-se
por **Signed JWT (`private_key_jwt`)**, recebe do Keycloak um **access token** no formato
**JWT**. Esse token é a credencial que autoriza a aplicação a invocar serviços protegidos a
jusante em seu próprio nome.

É o domínio **fundacional** da aplicação: não depende de nenhum outro domínio, e a
credencial que ele produz é o pré-requisito para qualquer integração autenticada
com serviços protegidos.

O escopo deste domínio termina na **produção e disponibilização** do token válido.
O ato de anexar o token a uma requisição concreta a um serviço a jusante e o
consumo desse serviço pertencem ao domínio consumidor, não a este.

## Regras de negócio

1. **Fluxo exclusivamente `client_credentials`.** A concessão (grant) usada é
   `client_credentials`. Não há fluxo de código de autorização, senha de usuário nem
   refresh token baseado em sessão de usuário — a aplicação se autentica apenas com a
   própria identidade de cliente, agora por Signed JWT (`private_key_jwt`; ver regra 2).
   Preservar este grant é uma restrição do projeto; qualquer mudança de fluxo exige
   decisão humana.

2. **Identidade de cliente = identificador + par de chaves.** A aplicação se apresenta
   ao Keycloak com um **client-id** (identificador público do cliente) e autentica-se por
   **Signed JWT (`private_key_jwt`)**: assina uma `client_assertion` com a **chave privada
   corrente** do seu par de chaves, sem enviar segredo compartilhado. O client-id é
   parametrizável por ambiente; o par de chaves é gerado, rotacionado e persistido pela
   própria aplicação (ver "Variáveis de ambiente do domínio", "Entidades e contratos" e a
   regra 8).

3. **Descoberta automática de endpoints (OpenID Connect).** Os endereços do endpoint
   de token e do conjunto de chaves públicas (JWKS) **não** são configurados
   manualmente: são resolvidos automaticamente a partir do **issuer-uri** do realm,
   pelo documento de descoberta padrão OpenID Connect publicado em
   `<issuer-uri>/.well-known/openid-configuration`. Configurar esses endpoints à mão
   contraria a convenção do domínio.

4. **Escopo do token restrito a `app-b.invoke`.** A obtenção do token solicita o
   escopo **`app-b.invoke`**. Esse escopo (definido como client scope no próprio
   Keycloak) delimita o que o token concede — isto é, para quais operações a
   credencial de máquina é autorizada. O token não deve ser emitido com escopo mais
   amplo que o necessário para a integração pretendida.

5. **Token obtido sob demanda e reutilizado enquanto válido.** O token é obtido na
   primeira vez em que uma chamada autenticada a jusante precisa dele e mantido para
   reúso. Enquanto continuar válido, o mesmo token é reaproveitado; não se solicita um
   token novo a cada chamada desnecessariamente.

6. **Renovação automática ao expirar.** Quando o token vigente está expirado (ou
   prestes a expirar), um novo token é obtido automaticamente junto ao Keycloak antes
   da próxima chamada, de forma transparente. Não há renovação manual nem intervenção
   externa: a expiração é detectada e um novo `client_credentials` é executado.

7. **Token mantido apenas em memória; par de chaves persistido.** A credencial (access
   token) obtida é guardada **em memória** no processo da aplicação, associada à identidade
   de cliente; ao reiniciar a aplicação, um novo token é obtido no primeiro uso. O **par de
   chaves** da aplicação, ao contrário, é **persistido** (keystore em arquivo) para
   sobreviver a reinícios — de modo que o `kid` publicado no JWKS não muda após reiniciar e
   o Keycloak continua validando a `client_assertion`. Não introduza persistência do token
   sem decisão humana; a persistência do par de chaves é decisão humana registrada nesta
   unidade.

8. **Autenticação por Signed JWT com par de chaves gerado e rotacionado pela aplicação.**
   A aplicação não usa mais `client-secret`. Ela gera um par de chaves (RSA), persiste-o
   para sobreviver a reinícios, assina a `client_assertion` (`private_key_jwt`) com a chave
   privada corrente e publica as chaves públicas em um endereço JWKS que o Keycloak busca a
   cada renovação. A rotação do par é acionada manualmente por um endpoint POST dedicado.
   Ver "Fluxo e ciclo de vida do token", "Entidades e contratos" e "Débito técnico
   conhecido".

## Fluxo e ciclo de vida do token

1. **Gatilho.** Uma operação que exige chamada autenticada a um serviço a jusante
   demanda a credencial.
2. **Verificação da credencial em memória.** Existe token válido em memória? Se sim,
   ele é reutilizado (passo 5).
3. **Obtenção do token.** Não havendo token válido, a aplicação executa o
   `client_credentials` contra o endpoint de token do Keycloak (resolvido por
   descoberta), autenticando-se por `private_key_jwt`: envia o client-id, uma
   `client_assertion` (JWT assinado com a chave privada corrente, com o `kid` no
   cabeçalho) e o escopo `app-b.invoke`. Não é enviado `client-secret`.
4. **Emissão e armazenamento.** O Keycloak valida a `client_assertion` contra as chaves
   públicas correntes da aplicação (buscadas no endereço JWKS, "Use JWKS URL") e emite o
   access token JWT com seu prazo de validade. O token é armazenado em memória, associado à
   identidade de cliente.
5. **Disponibilização.** O token válido fica disponível para o domínio consumidor
   anexá-lo à requisição a jusante.
6. **Expiração e renovação.** Quando o prazo do token vence, ele é considerado
   inválido; a próxima demanda repete a partir do passo 3, obtendo um token novo.

O documento de descoberta e o conjunto de chaves públicas (JWKS) publicados pelo
**issuer** resolvem tanto o endpoint de token quanto o de chaves. Este domínio usa o
endpoint de token para **obter** a credencial; a **validação** da assinatura do access
token via JWKS do issuer é responsabilidade do serviço protegido a jusante (resource
server), não desta aplicação. Não confundir com o **JWKS da própria aplicação**
(`GET /.well-known/jwks.json`), que publica as chaves públicas do par da App A para o
Keycloak validar a `client_assertion` — este sim é responsabilidade deste domínio (ver
"Entidades e contratos").

## Entidades e contratos

- **Identidade de cliente (client registration).** Registro lógico da aplicação como
  cliente OAuth2 do realm, reunindo: identificador do cliente (client-id), método de
  autenticação do cliente `private_key_jwt` (sem `client-secret`), tipo de concessão
  `client_credentials`, escopo `app-b.invoke` e o emissor (issuer-uri) do realm. É a
  configuração que descreve "quem" a aplicação é perante o Keycloak.
- **Par de chaves da aplicação.** Par de chaves (RSA) gerado e rotacionado pela própria
  aplicação e persistido para sobreviver a reinícios. A chave **privada corrente** assina
  a `client_assertion`; as chaves **públicas** (corrente e, após rotação, a anterior) são
  publicadas no JWKS da aplicação. Cada chave tem um identificador (`kid`).
- **`client_assertion` (`private_key_jwt`).** JWT assinado com a chave privada corrente
  (`kid` no cabeçalho) que a aplicação envia ao endpoint de token para autenticar o
  cliente, no lugar do segredo compartilhado.
- **Access token (JWT).** Credencial emitida pelo Keycloak, com prazo de validade,
  que carrega o escopo concedido. É a saída deste domínio.
- **JWKS da aplicação (`GET /.well-known/jwks.json`).** Endereço exposto pela App A que
  publica as chaves públicas correntes (sem qualquer material privado) para o Keycloak
  buscar a cada renovação ("Use JWKS URL") e validar a `client_assertion`.
- **Endpoint de rotação (`POST /credencial/rotacionar-chave`).** Endereço exposto pela
  App A que gera/rotaciona o par de chaves, promovendo o novo par a corrente e mantendo o
  anterior publicado no JWKS; retorna o `kid` gerado e o `criadaEm`, sem expor material
  privado. Acionado manualmente e **aberto** (sem autenticação, coerente com a POC).
- **Endpoint de token do Keycloak.** Endereço (resolvido por descoberta a partir do
  issuer-uri) onde o `client_credentials` é executado para emitir o token.
- **Documento de descoberta OpenID Connect.** `<issuer-uri>/.well-known/openid-configuration`,
  publicado pelo realm; fonte dos endereços de token e de JWKS do issuer.

## Restrições e validações

- O fluxo `client_credentials` contra o Keycloak é a base do projeto e **deve ser
  preservado**; ajustes ao fluxo dependem de decisão humana.
- Os endpoints de token/JWKS **não** devem ser configurados manualmente — apenas o
  issuer-uri, do qual são derivados.
- O token **não** é persistido fora da memória do processo; o **par de chaves** da
  aplicação, sim, é persistido (keystore) para sobreviver a reinícios.
- Segredos reais e **material privado** (chave privada/keystore) **nunca** são versionados;
  os defaults de configuração servem apenas a desenvolvimento e devem ser sobrescritos por
  ambiente.
- Não há suíte de testes automatizados neste domínio (projeto tratado como POC); não
  escreva/exija testes salvo decisão humana em contrário.

## Variáveis de ambiente do domínio

As variáveis abaixo estão diretamente ligadas ao comportamento de negócio deste
domínio (identidade de cliente, material de credencial e emissor da credencial). Os
valores default existentes são **placeholders de desenvolvimento** e devem ser
sobrescritos por ambiente; nunca registre segredos nem material privado reais.

- **`APP_A_CLIENT_ID`** — identificador do cliente (client-id) usado para a aplicação
  se apresentar ao Keycloak. Default de desenvolvimento: `app-a`.
- **`APP_A_KEYSTORE_LOCATION`** — caminho do arquivo de keystore (PKCS12) onde o par de
  chaves da aplicação é persistido e recarregado no boot. Se ausente na primeira execução,
  um par inicial é gerado e persistido nesse caminho. Default de desenvolvimento aponta
  para um arquivo local, inadequado fora de desenvolvimento.
- **`APP_A_KEYSTORE_PASSWORD`** — senha que protege o keystore e o material privado nele
  guardado. O default é um placeholder inseguro, válido apenas em desenvolvimento, e
  **deve** ser substituído fora dele. Nunca versione o valor real.
- **`KEYCLOAK_ISSUER_URI`** — emissor (issuer) do realm alvo do Keycloak, a partir do
  qual os endpoints de token e JWKS são descobertos. Default de desenvolvimento aponta
  para um host de exemplo, inadequado fora de desenvolvimento.

## Integrações e dependências externas

- **Keycloak** — provedor de identidade que emite o access token via
  `client_credentials` e publica a descoberta OpenID Connect. Passa a validar a
  `client_assertion` da aplicação buscando as chaves públicas no JWKS da App A ("Use JWKS
  URL"). É a dependência externa central deste domínio: sem ele a aplicação não obtém
  credencial e nenhuma chamada a jusante é autorizada.

As demais pré-condições técnicas do domínio (biblioteca cliente OAuth2, configuração
externalizada e configuração do cliente no realm) estão detalhadas em
`references/technical-dependencies.md`.

## Débito técnico conhecido

- **Segredo do cliente em configuração externalizada — encerrado.** O débito anterior
  (manter o `client-secret` em configuração externalizada) foi **encerrado** nesta unidade:
  a autenticação do cliente passou a `private_key_jwt` e o
  `client-secret`/`APP_A_CLIENT_SECRET` foi removido. Registro histórico; não há mais
  segredo compartilhado a evoluir.
- **Material privado persistido a proteger.** O par de chaves é persistido em keystore
  (arquivo). O material privado deve ser protegido por permissões do arquivo e pela senha do
  keystore; o material real nunca deve ser versionado e os defaults servem apenas a
  desenvolvimento. Evoluir, fora da POC, para um armazenamento mais seguro (ex.: cofre de
  segredos/HSM).
- **Endpoint de rotação aberto (POC).** O `POST /credencial/rotacionar-chave` é **aberto**
  (sem autenticação), coerente com a POC e com o `GET /demo/chamar-app-b`. Risco conhecido:
  qualquer um pode forçar rotações e provocar janelas de falha de autenticação (negação de
  serviço). Reavaliar proteção fora da POC. Decisão humana registrada nesta unidade.

## Referências

- `references/technical-dependencies.md` — pré-condições técnicas do domínio (o que
  cada dependência é e o que deixa de funcionar na sua ausência).
