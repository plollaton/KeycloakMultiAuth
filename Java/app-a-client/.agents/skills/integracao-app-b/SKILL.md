---
name: integracao-app-b
description: >
  Esta é a documentação autoritativa do domínio Integração com a App B. Descreve
  como a aplicação consome o endpoint protegido da App B (`GET /api/protegido`) em
  seu próprio nome, anexando automaticamente o access token (bearer JWT) obtido na
  autenticação máquina-a-máquina, e como expõe o endpoint de demonstração
  (`GET /demo/chamar-app-b`) que aciona e valida o fluxo ponta a ponta. Carregue
  esta skill em tarefas sobre integração com a App B, consumo de API protegida a
  jusante, cliente HTTP RestClient da App B, anexo de bearer token via interceptor,
  base-url da App B, endpoint de demonstração ou o contrato REST exposto pela
  aplicação.
metadata:
  author: clovis-cli
  type: domain-skill
---

# Integração com a App B

> **Manutenção desta skill**
>
> Atualize este documento sempre que o comportamento do domínio mudar de propósito
> (novo endpoint consumido ou exposto, nova forma de anexar a credencial, nova regra
> de repasse da resposta, mudança de alvo ou de tratamento de falha), mantendo a
> skill fiel ao comportamento efetivamente implementado. Refatoração técnica que
> preserva a regra (renomear, extrair configuração, trocar biblioteca equivalente)
> não exige alteração. Quando a skill afirmar X e o código fizer Y sem uma decisão
> registrada que resolva a divergência, trate como lacuna e escale para decisão
> humana — não ajuste a skill nem o código por conta própria.

## Visão geral do domínio

Este domínio consome um serviço REST protegido a jusante — a **App B** — em nome da
**própria aplicação** (não de um usuário final), e expõe um endpoint de demonstração
para acionar e validar o fluxo de ponta a ponta.

A aplicação cliente (App A) precisa invocar o endpoint protegido `GET /api/protegido`
da App B. Como esse endpoint exige uma credencial válida, toda chamada de saída
carrega automaticamente um **access token (JWT)** no cabeçalho `Authorization: Bearer
<jwt>`. Esse token é produzido pelo domínio de autenticação máquina-a-máquina; este
domínio apenas o **anexa e consome** — é o domínio consumidor da credencial.

O domínio depende da autenticação máquina-a-máquina: sem a credencial de máquina
válida, a App B rejeita a chamada. A fronteira deste domínio começa onde a credencial
já está disponível (anexá-la à requisição concreta e consumir a App B) e termina no
repasse da resposta obtida.

## Regras de negócio

1. **Consumo do endpoint protegido da App B.** A aplicação chama `GET /api/protegido`
   na App B em seu próprio nome. É a operação central do domínio: acionar o serviço
   protegido a jusante e obter sua resposta.

2. **Anexação automática do bearer token.** Toda requisição de saída para a App B
   recebe automaticamente o cabeçalho `Authorization: Bearer <jwt>`, com o access
   token (JWT) obtido e renovado pelo domínio de autenticação máquina-a-máquina. A
   aplicação **não** monta esse cabeçalho manualmente nem gerencia o token na camada
   de integração: um interceptor de saída resolve a credencial de máquina e a injeta
   antes de cada chamada. A obtenção/renovação do token é transparente para este
   domínio.

3. **Credencial sempre a mesma (registration fixo).** A credencial usada para
   autorizar as chamadas à App B é sempre o registration de cliente
   `keycloak-client-credentials` — resolvido de forma fixa, sem depender de nenhum
   atributo da requisição de entrada. Não há seleção dinâmica de credencial por
   chamada.

4. **Alvo (endereço da App B) parametrizável.** O endereço-base da App B é
   parametrizável por ambiente (ver "Variáveis de ambiente do domínio"); o caminho
   consumido (`/api/protegido`) é fixo. Isso permite apontar a integração para
   diferentes instâncias da App B sem alterar o código.

5. **Resposta repassada verbatim.** O corpo devolvido pela App B é tratado como
   **texto opaco** e repassado como está pelo endpoint de demonstração. A aplicação
   não interpreta, valida nem transforma o conteúdo retornado pela App B — o
   significado do corpo pertence ao contrato da App B, não a este domínio.

6. **Endpoint de demonstração como gatilho manual.** O endpoint `GET /demo/chamar-app-b`
   existe exclusivamente para **acionar e validar manualmente** o fluxo ponta a ponta
   (App A obtém o token e chama a App B com ele). Não é uma API de negócio de produção:
   não recebe parâmetros e apenas dispara a chamada a jusante, devolvendo o que a App B
   respondeu.

7. **Sem tratamento de erro especializado.** Não há tratamento de erro específico na
   integração: uma falha na chamada à App B — rejeição por credencial inválida ou
   escopo insuficiente (401/403), erro do serviço a jusante (5xx) ou indisponibilidade
   da App B — propaga como falha da requisição de demonstração, sem tradução para uma
   resposta de erro de negócio própria. Este é o comportamento atual (projeto tratado
   como POC); qualquer política de erro dedicada é decisão humana futura.

8. **Contrato REST exposto é documentado.** O endpoint de entrada exposto por este
   domínio compõe o contrato REST documentado da aplicação. Por decisão validada na
   descoberta, o contrato REST é mantido documentado via OpenAPI, gerado a partir do
   próprio código; ao criar ou alterar o endpoint exposto, o contrato documentado deve
   acompanhar a mudança.

## Fluxo e ciclo de vida

1. **Gatilho.** Uma requisição chega a `GET /demo/chamar-app-b` (ou qualquer operação
   que use o cliente da App B), demandando o consumo do serviço protegido.
2. **Credencial disponível.** O interceptor de saída assegura um access token (JWT)
   válido para o registration `keycloak-client-credentials` — reutilizando o token
   vigente ou obtendo um novo junto ao domínio de autenticação, de forma transparente.
3. **Chamada a jusante.** A aplicação emite `GET {base-url da App B}/api/protegido`,
   com o cabeçalho `Authorization: Bearer <jwt>` já anexado.
4. **Processamento pela App B.** A App B valida a assinatura do JWT (via JWKS) e o
   escopo, e responde. A validação do token é responsabilidade da App B (resource
   server), não desta aplicação.
5. **Repasse da resposta.** Em caso de sucesso, o corpo devolvido pela App B é
   repassado verbatim ao chamador do endpoint de demonstração.
6. **Falha.** Se a App B rejeitar (401/403), falhar (5xx) ou estiver indisponível, a
   falha propaga como falha da requisição de demonstração (ver regra 7).

## Entidades e contratos

- **`GET /demo/chamar-app-b`** — endpoint de **entrada** exposto pela aplicação. Sem
  parâmetros. Aciona o fluxo e retorna, como texto, o corpo que a App B devolveu.
  Endpoint de demonstração/gatilho manual, não uma API de negócio de produção.
- **`GET /api/protegido`** — endpoint **consumido** na App B. Exige o bearer token no
  cabeçalho `Authorization`. Seu contrato (formato do corpo, códigos de resposta)
  pertence à App B; esta aplicação o consome de forma opaca.
- **Cliente HTTP da App B** — configuração lógica do cliente de saída: o endereço-base
  da App B mais o interceptor que anexa a credencial de máquina resolvida pelo
  registration `keycloak-client-credentials`. É o que liga o alvo (App B) à credencial
  (domínio de autenticação).

## Restrições e validações

- Toda chamada à App B **deve** carregar o bearer token válido; sem credencial válida
  a App B rejeita a chamada (401). O anexo é automático e não deve ser contornado.
- A credencial usada é **sempre** o registration `keycloak-client-credentials`; não há
  troca de credencial por requisição.
- O endereço-base da App B é externalizado por configuração; o default existente serve
  apenas a desenvolvimento e deve ser sobrescrito por ambiente.
- **Fronteira de domínio:** a produção e a renovação do access token pertencem ao
  domínio de autenticação máquina-a-máquina, não a este; a validação da assinatura do
  JWT (via JWKS) é responsabilidade da App B (resource server), não desta aplicação.
  Este domínio cobre apenas anexar a credencial já disponível, chamar a App B e
  repassar a resposta.
- Não há suíte de testes automatizados neste domínio (projeto tratado como POC); não
  escreva/exija testes salvo decisão humana em contrário.

## Variáveis de ambiente do domínio

A variável abaixo está diretamente ligada ao comportamento de negócio deste domínio
(o alvo da integração). O valor default é um **placeholder de desenvolvimento** e deve
ser sobrescrito por ambiente.

- **`APP_B_BASE_URL`** — endereço-base da App B a ser consumida. Define para qual
  instância da App B as chamadas de saída são direcionadas. Default de desenvolvimento:
  `http://localhost:8082`, inadequado fora de desenvolvimento.

O registration de credencial usado nas chamadas à App B é uma configuração fixa
(`keycloak-client-credentials`), não uma variável de ambiente: ele associa a
integração à identidade de cliente definida no domínio de autenticação.

## Integrações e dependências externas

- **App B** — serviço REST a jusante que expõe `GET /api/protegido` e valida o JWT via
  JWKS. É o destino da integração: sem ela, não há o que consumir.
- **Keycloak** (indiretamente) — provedor de identidade que emite o access token
  consumido nas chamadas; a obtenção do token pertence ao domínio de autenticação
  máquina-a-máquina, e este domínio depende dessa credencial para autorizar as chamadas
  à App B.

As demais pré-condições técnicas do domínio (cliente HTTP, dependência da credencial de
máquina e configuração externalizada do endereço da App B) estão detalhadas em
`references/technical-dependencies.md`.

## Referências

- `references/technical-dependencies.md` — pré-condições técnicas do domínio (o que
  cada dependência é e o que deixa de funcionar na sua ausência).
