# Spec: Extração do domínio de Acesso Cruzado a Aplicação Externa

## Overview

Extração das classes e da documentação de acesso cruzado a uma aplicação externa (`GET /api/cross`,
hoje mantidas dentro do domínio Servidor de Recursos OAuth2) para um domínio próprio, sem alterar o
comportamento observável do endpoint.

## Domains involved

- **`servidor-recursos-oauth2`** (existente) — [`SKILL.md`](../../skills/servidor-recursos-oauth2/SKILL.md).
  Deixa de ser dono da regra de negócio de `GET /api/cross` e das classes `CrossAccess*`; continua
  dono da `SecurityFilterChain` (`SecurityConfig`), que mantém `GET /api/cross` público como
  pré-requisito do novo domínio — o mesmo tratamento que hoje já dá a `GET /oauth2/jwks`
  (regra 6 daquela skill).
- **`acesso-cruzado-api-externa`** (novo, criado por esta unidade) — ainda sem skill própria; esta
  unidade estabelece seu boundary inicial. Passa a ser dono da regra de negócio de `GET /api/cross`:
  a classificação do endpoint como público e a chamada de saída a `GET /api/protected` de uma
  aplicação externa (cenário `servico-a` → `api-b` descrito em `.agents/context/business-input.md`,
  linhas 5-12 e 14-33), usando o `access_token` obtido junto ao Keycloak.

## Estado atual do código e da documentação

- Pacote `com.aplicacaosegura.resourceserver` reúne, hoje, tanto as classes do domínio Servidor de
  Recursos OAuth2 (`ResourceServerExampleController`, `ResourceServerJwtDecoderConfig`,
  `AccessClassificationResponse`, `AudienceValidator`) quanto as três classes de acesso cruzado
  (`CrossAccessController`, `CrossAccessResponse`, `CrossAccessRestClientConfig`).
- `SecurityConfig` (pacote `web`) já libera `GET /api/cross` como público na mesma
  `SecurityFilterChain` que protege `GET /api/protected`; esta spec não altera essa liberação.
- A skill `servidor-recursos-oauth2` documenta `GET /api/cross` como regra de negócio própria
  (regra 4) e como linha da tabela de classificação de endpoints em "Entidades e dados", junto dos
  demais endpoints daquele domínio.
- A spec `.agents/specs/005-servidor-recursos-oauth2-cross-chamada-api-externa/` (spec, plan,
  research, test-cases e tasks) documenta a introdução de `GET /api/cross` como unidade do domínio
  Servidor de Recursos OAuth2.
- `.agents/maps/functional-map.md` lista três domínios e não reconhece acesso cruzado a aplicação
  externa como domínio próprio.

## Scope

**In:**

- Reclassificação da regra de negócio de `GET /api/cross` (endpoint público, chamada de saída a
  `GET /api/protected` de uma aplicação externa usando um `access_token`) como pertencente a um
  domínio próprio, distinto do Servidor de Recursos OAuth2.
- Relocação do código das classes `CrossAccessController`, `CrossAccessResponse` e
  `CrossAccessRestClientConfig` para um pacote próprio desse novo domínio.
- Relocação de toda a documentação de acesso cruzado — o conteúdo hoje em
  `.agents/specs/005-servidor-recursos-oauth2-cross-chamada-api-externa/` e os trechos
  correspondentes da skill `servidor-recursos-oauth2` — para artefatos (skill e spec) próprios do
  novo domínio.

**Out:**

- Qualquer mudança no comportamento observável de `GET /api/cross`: continua público, continua
  chamando `GET /api/protected` da aplicação externa configurada em
  `app.cross.external-base-url`, e continua respondendo `200`/`502` nos mesmos critérios já
  vigentes.
- Qualquer mudança nos endpoints `GET /api/protected` e `GET /api/public` ou na
  `SecurityFilterChain` além de preservar `GET /api/cross` como público.
- Qualquer mudança no domínio Cliente OAuth2 com Client Assertion JWT (`OAuth2AuthorizedClientManager`)
  — consumido sem alteração.
- Qualquer mudança de comportamento do lado da aplicação externa (`api-b`), fora do controle deste
  repositório.

## Domain boundary

**This spec implements:**

- Novo pacote Java (nome definido em `plan.md`) para `CrossAccessController`, `CrossAccessResponse`
  e `CrossAccessRestClientConfig`, movidas de `com.aplicacaosegura.resourceserver`, sem alteração de
  lógica.
- Nova skill de domínio `.agents/skills/acesso-cruzado-api-externa/SKILL.md`, documentando a regra
  de negócio de `GET /api/cross` (classificação pública, chamada de saída, uso do `access_token`,
  propriedade `app.cross.external-base-url`, respostas `200`/`502`) — conteúdo hoje disperso entre a
  regra 4 e a tabela de "Entidades e dados" da skill `servidor-recursos-oauth2`.
- Spec própria do novo domínio, com o conteúdo hoje em
  `.agents/specs/005-servidor-recursos-oauth2-cross-chamada-api-externa/` (spec, plan, research,
  test-cases, tasks) relocado para fora da pasta prefixada por `servidor-recursos-oauth2`.
- Atualização da skill `servidor-recursos-oauth2` para remover a regra 4 e a linha correspondente da
  tabela de classificação de endpoints, substituindo-as por uma referência cruzada ao novo domínio —
  mesmo tratamento hoje dado a `GET /oauth2/jwks` (regra 6).
- Atualização de `.agents/maps/functional-map.md` incluindo o novo domínio como quarta entrada e
  ajustando a descrição do domínio Servidor de Recursos OAuth2 para não mais citar `GET /api/cross`
  como regra própria.

**Belongs to other domains (cross-domain, does not become a task here):**

- `SecurityConfig` e a `SecurityFilterChain` como um todo → domínio `servidor-recursos-oauth2`; esta
  unidade não altera a lista de caminhos públicos, apenas a documentação de quem é dono da regra por
  trás da liberação de `GET /api/cross`.
- Obtenção do `access_token` via `client_assertion` (`OAuth2AuthorizedClientManager`) →
  domínio `cliente-oauth2-client-assertion`; consumido sem alteração pelo novo domínio.
- Validação do `access_token` apresentado por `GET /api/cross` → responsabilidade da aplicação
  externa (`api-b`), fora do controle deste repositório.

## User stories

1. Como mantenedor do código, quero que as classes de acesso cruzado fiquem em um pacote próprio, e
   não misturadas ao pacote do Servidor de Recursos OAuth2, para que a responsabilidade de acessar a
   aplicação externa não fique acoplada à validação de tokens recebidos.
2. Como mantenedor da documentação, quero uma skill própria para o domínio de acesso cruzado, para
   consultar suas regras sem misturá-las às regras de classificação de endpoints do Servidor de
   Recursos OAuth2.
3. Como mantenedor da documentação, quero que a spec de `GET /api/cross` viva nos artefatos do novo
   domínio, para que o histórico de specs reflita a fronteira de domínios atual.
4. Como consumidor de `GET /api/cross`, quero que o endpoint continue respondendo exatamente como
   antes da reorganização, para que a mudança seja transparente para quem já o utiliza.

## Acceptance criteria

**História 1 — Relocação do código:**

- Dado o pacote `com.aplicacaosegura.resourceserver` com as três classes `CrossAccess*`, quando esta
  unidade é executada, então essas três classes passam a existir no pacote próprio do novo domínio e
  deixam de existir em `resourceserver`.
- Dado o pacote de destino, quando as classes são movidas, então nenhuma delas tem sua lógica interna
  alterada (mesma assinatura de `GET /api/cross`, mesmo uso do `OAuth2AuthorizedClientManager`, mesma
  leitura de `app.cross.external-base-url`).

**História 2 — Skill própria:**

- Dado que a skill `acesso-cruzado-api-externa` ainda não existe, quando esta unidade é executada,
  então `.agents/skills/acesso-cruzado-api-externa/SKILL.md` passa a existir, documentando a
  classificação pública de `GET /api/cross`, a chamada de saída e as respostas `200`/`502`.
- Dado o conteúdo atual da skill `servidor-recursos-oauth2` (regra 4 e linha de `GET /api/cross` na
  tabela de "Entidades e dados"), quando esta unidade é executada, então esse conteúdo deixa de
  aparecer ali como regra própria e passa a ser referenciado como pertencente ao novo domínio, na
  mesma forma como `GET /oauth2/jwks` já é referenciado (regra 6).

**História 3 — Spec própria:**

- Dado o conteúdo hoje em `.agents/specs/005-servidor-recursos-oauth2-cross-chamada-api-externa/`,
  quando esta unidade é executada, então esse conteúdo é relocado para uma pasta de spec do novo
  domínio, sem o prefixo `servidor-recursos-oauth2`, e deixa de existir na pasta original.

**História 4 — Comportamento inalterado:**

- Dado um `GET /api/cross` sem `access_token` do chamador, antes e depois desta unidade, quando a
  requisição é processada, então em ambos os casos ela é aceita sem exigir autenticação do chamador.
- Dado que a chamada de saída a `GET /api/protected` da aplicação externa é aceita, quando o
  chamador executa `GET /api/cross` após esta unidade, então a resposta continua `200` com o mesmo
  corpo (`CrossAccessResponse`).
- Dado que a chamada de saída falha (token rejeitado, aplicação externa inacessível, ou falha na
  obtenção do `access_token`), quando o chamador executa `GET /api/cross` após esta unidade, então a
  resposta continua `502`, sem expor `access_token` nem material de chave privada.

## Cross-domain dependencies

- **`servidor-recursos-oauth2`** — mantém `GET /api/cross` público na sua `SecurityFilterChain`;
  o novo domínio depende dessa liberação continuar existindo, sem precisar alterá-la.
- **`cliente-oauth2-client-assertion`** — fornece o `access_token` via
  `OAuth2AuthorizedClientManager` já configurado; o novo domínio consome essa capacidade sem
  alterá-la.
- **Aplicação externa (`api-b`)** — expõe `GET /api/protected` e valida o `access_token`
  apresentado; fora do controle deste repositório.

## Risks and observations

- O nome exato do pacote Java de destino e a numeração/slug final da pasta de spec do novo domínio
  (se reaproveita "005" ou recebe outra numeração) são decisões mecânicas de `plan.md` e da execução
  de tasks, não fixadas por esta spec.
- Esta POC não implementa testes automatizados (`discovery-answers.md`); a verificação do
  comportamento inalterado de `GET /api/cross` (História 4) é manual, comparando requisições antes e
  depois da reorganização.
- A extração cria o quarto domínio da aplicação, até então não previsto em
  `.agents/maps/functional-map.md`; a atualização desse mapa é parte do boundary desta unidade (ver
  "Domain boundary"), não uma rediscoberta completa dos três domínios já existentes.
