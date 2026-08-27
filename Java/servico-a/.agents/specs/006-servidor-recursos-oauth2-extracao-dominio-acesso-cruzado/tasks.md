# Tasks: Extração do domínio de Acesso Cruzado a Aplicação Externa

Esta POC não implementa testes automatizados (`discovery-answers.md`); a validação de cada tarefa
é manual, pelos casos correspondentes em `test-cases.md`.

- [x] **T1. Mover a documentação de GET /api/cross para a nova skill de domínio**
  - Depends on: none
  - Alvo: `.agents/skills/servidor-recursos-oauth2/SKILL.md` (correção), `.agents/skills/acesso-cruzado-api-externa/SKILL.md`
    (criação), `.agents/skills/acesso-cruzado-api-externa/references/technical-dependencies.md`
    (criação) e `.agents/maps/functional-map.md` (correção).
  - Seções afetadas: em `servidor-recursos-oauth2/SKILL.md`, "Regras de negócio" (regra 4) e a linha
    de `GET /api/cross` na tabela de "Entidades e dados"; em `functional-map.md`, a lista de
    domínios e a entrada do domínio "Cliente OAuth2 com Client Assertion JWT".
  - Mudança esperada: a regra 4 e a linha da tabela deixam de descrever `GET /api/cross` como regra
    própria de `servidor-recursos-oauth2` e passam a referenciar `acesso-cruzado-api-externa` como
    dono da regra, com a liberação do caminho tratada como pré-requisito daquele domínio na
    `SecurityFilterChain` — mesma redação já usada para `GET /oauth2/jwks` (regra 6). A nova skill
    `acesso-cruzado-api-externa/SKILL.md` passa a existir, documentando a classificação pública de
    `GET /api/cross`, a chamada de saída a `GET /api/protected` da aplicação externa, o uso do
    `access_token` obtido via `OAuth2AuthorizedClientManager` e a propriedade
    `app.cross.external-base-url`; seu `references/technical-dependencies.md` lista
    `cliente-oauth2-client-assertion`, `servidor-recursos-oauth2`, `spring-boot-starter-web`
    (`RestClient`) e OpenAPI/Swagger. `functional-map.md` ganha uma quarta entrada de domínio
    ("Acesso Cruzado a Aplicação Externa"), no formato das três já existentes, e a entrada do
    domínio "Cliente OAuth2 com Client Assertion JWT" deixa de afirmar que a chamada a uma API
    específica (`api-b`) está fora do escopo implementado, passando a apontar que essa chamada é
    implementada pelo domínio `acesso-cruzado-api-externa`.
  - Origem: Histórias 2 e 3 do `spec.md` desta unidade (critérios de aceite sobre a skill própria e
    sobre a reclassificação da regra na skill `servidor-recursos-oauth2`).
  - Cobre TC-4, TC-5, TC-6 e TC-9 do `test-cases.md`.

- [x] **T2. Relocação das classes de acesso cruzado para o novo pacote**
  - Depends on: none
  - `CrossAccessController`, `CrossAccessResponse` e `CrossAccessRestClientConfig` movidas de
    `com.aplicacaosegura.resourceserver` para `com.aplicacaosegura.crossaccess`, sem alteração de
    lógica (mesma injeção do `OAuth2AuthorizedClientManager`, mesmo `RestClient` configurado por
    `app.cross.external-base-url`, mesmas respostas `200`/`502`, mesmas anotações OpenAPI).
  - Cobre a História 1 (TC-1 a TC-3) e a História 4 (TC-10 a TC-13) do `test-cases.md`: o
    comportamento observável de `GET /api/cross` (classificação pública, chamada de saída, corpo de
    sucesso, resposta de falha, contrato OpenAPI) permanece idêntico ao já validado antes desta
    unidade.

- [x] **T3. Relocação da spec fundadora do novo domínio**
  - Depends on: T1
  - Pasta `.agents/specs/005-servidor-recursos-oauth2-cross-chamada-api-externa/` renomeada para
    `.agents/specs/005-acesso-cruzado-api-externa/`, preservando `spec.md`, `plan.md`, `research.md`,
    `test-cases.md` e `tasks.md`.
  - Dentro da pasta renomeada: a seção "Domain" do `spec.md` passa a citar o slug
    `acesso-cruzado-api-externa` e o link para `acesso-cruzado-api-externa/SKILL.md` (criada em T1);
    a seção "Stack and structure" do `plan.md` passa a citar o pacote
    `com.aplicacaosegura.crossaccess` no lugar de `com.aplicacaosegura.resourceserver`.
  - Precondition: a skill `acesso-cruzado-api-externa` referenciada pelo `spec.md` relocado precisa
    já existir — entregue em T1.
  - Cobre TC-7 e TC-8 do `test-cases.md`.
