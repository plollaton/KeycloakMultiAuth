# Test cases: Extração do domínio de Acesso Cruzado a Aplicação Externa

Esta POC não implementa testes automatizados (`discovery-answers.md`); este catálogo serve como
roteiro de validação manual desta unidade e como referência de cobertura na fase de implementação.

## Preconditions

- Repositório com o código e a documentação desta unidade já relocados (pacote Java, skills,
  `functional-map.md` e pasta de spec).
- Aplicação em execução (`mvn spring-boot:run`), com `app.cross.external-base-url` informado na
  linha de comando de inicialização, apontando para uma aplicação externa real e acessível, que
  expõe `GET /api/protected` como resource server, exceto quando o caso indicar o contrário.
- Troca `client_credentials`/`client_assertion` junto ao Keycloak já configurada e funcional
  (domínio "Cliente OAuth2 com Client Assertion JWT"), exceto quando o caso indicar o contrário.
- Cliente HTTP capaz de enviar requisições sem o cabeçalho `Authorization` (ex.: `curl`).
- Acesso ao código-fonte e aos artefatos de `.agents/` do repositório, para inspeção de pacote e
  documentação.

## História 1 — Relocação do código

### TC-1 (mandatory) — Classes de acesso cruzado no novo pacote

1. Inspecione o código-fonte em `src/main/java/com/aplicacaosegura/crossaccess/`.

**Expected:** `CrossAccessController`, `CrossAccessResponse` e `CrossAccessRestClientConfig`
existem nesse pacote, com a declaração `package com.aplicacaosegura.crossaccess;`.

### TC-2 (mandatory) — Classes de acesso cruzado ausentes do pacote resourceserver

1. Inspecione o código-fonte em `src/main/java/com/aplicacaosegura/resourceserver/`.

**Expected:** nenhuma das classes `CrossAccessController`, `CrossAccessResponse` ou
`CrossAccessRestClientConfig` existe nesse pacote; `ResourceServerExampleController`,
`ResourceServerJwtDecoderConfig`, `AccessClassificationResponse` e `AudienceValidator` continuam
presentes ali.

### TC-3 (mandatory) — Lógica interna preservada na relocação

1. Compare o conteúdo de `CrossAccessController`, `CrossAccessResponse` e
   `CrossAccessRestClientConfig` no novo pacote com o comportamento descrito no `plan.md` desta
   unidade (injeção do `OAuth2AuthorizedClientManager`, `RestClient` configurado por
   `app.cross.external-base-url`, respostas `200`/`502`).

**Expected:** nenhuma diferença de lógica em relação ao que essas classes já faziam em
`resourceserver` — apenas a declaração `package` e os imports decorrentes da mudança de pacote.

## História 2 — Skill própria do novo domínio

### TC-4 (mandatory) — Skill acesso-cruzado-api-externa existe e documenta a regra

1. Abra `.agents/skills/acesso-cruzado-api-externa/SKILL.md`.

**Expected:** o arquivo existe e documenta a classificação pública de `GET /api/cross`, a chamada
de saída a `GET /api/protected` da aplicação externa, o uso do `access_token` obtido via
`OAuth2AuthorizedClientManager`, a propriedade `app.cross.external-base-url` e as respostas
`200`/`502`.

### TC-5 (mandatory) — technical-dependencies.md da nova skill

1. Abra `.agents/skills/acesso-cruzado-api-externa/references/technical-dependencies.md`.

**Expected:** o arquivo existe e lista `cliente-oauth2-client-assertion`,
`servidor-recursos-oauth2`, `spring-boot-starter-web` (`RestClient`) e OpenAPI/Swagger, conforme
`plan.md`.

### TC-6 (mandatory) — Skill servidor-recursos-oauth2 sem GET /api/cross como regra própria

1. Abra `.agents/skills/servidor-recursos-oauth2/SKILL.md`.

**Expected:** a regra de negócio antes numerada como 4 e a linha correspondente de `GET /api/cross`
na tabela de "Entidades e dados" não descrevem mais o endpoint como regra própria deste domínio;
ambas passam a referenciar o domínio `acesso-cruzado-api-externa` como dono da regra, com a
liberação do caminho na `SecurityFilterChain` tratada como pré-requisito daquele domínio — mesma
redação já usada para `GET /oauth2/jwks`.

## História 3 — Spec própria do novo domínio

### TC-7 (mandatory) — Pasta da spec 005 renomeada

1. Verifique a existência de `.agents/specs/005-acesso-cruzado-api-externa/`.
2. Verifique a ausência de `.agents/specs/005-servidor-recursos-oauth2-cross-chamada-api-externa/`.

**Expected:** a pasta renomeada existe, contendo `spec.md`, `plan.md`, `research.md`,
`test-cases.md` e `tasks.md`; a pasta original não existe mais.

### TC-8 (mandatory) — Conteúdo da spec 005 relocada aponta para o novo domínio

1. Abra `.agents/specs/005-acesso-cruzado-api-externa/spec.md`, seção "Domain".
2. Abra `.agents/specs/005-acesso-cruzado-api-externa/plan.md`, seção "Stack and structure".

**Expected:** o `spec.md` cita o slug `acesso-cruzado-api-externa` e o link para a skill
`acesso-cruzado-api-externa/SKILL.md`; o `plan.md` cita o pacote `com.aplicacaosegura.crossaccess`
no lugar de `com.aplicacaosegura.resourceserver`.

### TC-9 (mandatory) — functional-map.md reflete o novo domínio

1. Abra `.agents/maps/functional-map.md`.

**Expected:** existe uma quarta entrada de domínio, "Acesso Cruzado a Aplicação Externa", no mesmo
formato das três já existentes (objetivo de negócio, evidência, dependências, regras inferidas,
dependências externas, dependências técnicas, nível de confiança); a entrada do domínio "Cliente
OAuth2 com Client Assertion JWT" não afirma mais que a chamada a uma API específica (`api-b`) está
fora do escopo implementado, e sim que essa chamada é implementada pelo domínio
`acesso-cruzado-api-externa`.

## História 4 — Comportamento inalterado de GET /api/cross

### TC-10 (mandatory) — Processado sem access_token do chamador

1. Sem enviar o cabeçalho `Authorization`, chame `GET /api/cross`.

**Expected:** a requisição é processada sem exigir autenticação do chamador (não retorna `401` por
ausência de credenciais).

### TC-11 (mandatory) — Sucesso do acesso cruzado após a relocação

1. Com `app.cross.external-base-url` apontando para a aplicação externa configurada e alcançável,
   sem enviar o cabeçalho `Authorization`, chame `GET /api/cross`.

**Expected:** a chamada de saída a `GET /api/protected` da aplicação externa é aceita e
`GET /api/cross` responde `200` com o corpo `CrossAccessResponse` (`status: "ok"`).

### TC-12 (mandatory) — Falha do acesso cruzado após a relocação

1. Configure a aplicação externa para rejeitar o `access_token` emitido para esta aplicação, ou
   torne-a temporariamente inacessível.
2. Chame `GET /api/cross`.

**Expected:** resposta `502`; o corpo da resposta não contém o `access_token` nem material de
chave privada.

### TC-13 (recommended) — Contrato OpenAPI continua descrevendo GET /api/cross

1. Sem enviar nenhuma credencial, chame `GET /v3/api-docs`.
2. Localize, no contrato retornado, a operação correspondente a `GET /api/cross`.

**Expected:** resposta `200`; a operação continua descrita no contrato, sem mudança de path,
parâmetros ou respostas declaradas em relação ao que já era descrito antes desta unidade.
