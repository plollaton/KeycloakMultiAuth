# Plan: Extração do domínio de Acesso Cruzado a Aplicação Externa

## Stack and structure

Aplicação Spring Boot 4.1.x / Java 21 / Jakarta EE 11, conforme `AGENTS.md`, com um pacote
Java por domínio sob `com.aplicacaosegura` (`jwks`, `oauth2client`, `resourceserver`, `web` para a
`SecurityFilterChain` transversal). O novo domínio segue essa mesma convenção: pacote
`com.aplicacaosegura.crossaccess`, recebendo `CrossAccessController`, `CrossAccessResponse` e
`CrossAccessRestClientConfig`, hoje em `com.aplicacaosegura.resourceserver`, sem alteração de
lógica, imports internos ou assinaturas — apenas a declaração `package` e os imports de quem as
referencia (nenhuma outra classe do projeto as referencia hoje).

A pasta `.agents/specs/005-servidor-recursos-oauth2-cross-chamada-api-externa/` é renomeada para
`.agents/specs/005-acesso-cruzado-api-externa/`, seguindo a mesma convenção já usada pelas specs
fundadoras dos outros três domínios (`001-gestao-chaves-rsa-jwks`, `002-cliente-oauth2-client-assertion`,
`003-servidor-recursos-oauth2`: número + slug do próprio domínio, sem sufixo de feature). Dentro da
pasta renomeada:

- `spec.md` — a seção "Domain" passa a apontar para `acesso-cruzado-api-externa` (slug e link para a
  nova skill, ver abaixo) em vez de `servidor-recursos-oauth2`; a lista "Belongs to other domains"
  passa a citar `servidor-recursos-oauth2` como o domínio que mantém `GET /api/cross` público na sua
  `SecurityFilterChain`, no lugar da referência anterior a ele como domínio dono.
- `plan.md` — a seção "Stack and structure" passa a citar o pacote `com.aplicacaosegura.crossaccess`
  no lugar de `com.aplicacaosegura.resourceserver`; a seção "Impact on the authoritative
  documentation" passa a apontar para a skill `acesso-cruzado-api-externa` (a skill que este conteúdo
  passa a documentar) no lugar de `servidor-recursos-oauth2`.
- `research.md` e `test-cases.md` — mantidos como registro das decisões e da cobertura de validação
  já tomadas para `GET /api/cross`; nenhum conteúdo técnico muda, pois nenhuma decisão registrada ali
  (reaproveitar o `OAuth2AuthorizedClientManager`, usar `RestClient`, a propriedade
  `app.cross.external-base-url`, o tratamento `502`) é alterada por esta unidade.
- `tasks.md` — registro histórico do que já foi executado; permanece como está (referências ao
  pacote `resourceserver` e à skill `servidor-recursos-oauth2` nas tarefas já concluídas descrevem o
  estado no momento em que foram executadas, não o estado atual do domínio).

Nenhuma dependência Maven nova. Nenhuma mudança na `SecurityFilterChain` (`SecurityConfig`, pacote
`web`): `GET /api/cross` continua na mesma lista `permitAll()`.

## Technical decisions

- **Nome do pacote do novo domínio**: `com.aplicacaosegura.crossaccess`, seguindo a convenção já em
  uso (um pacote por domínio, nome técnico em inglês sem separadores: `jwks`, `oauth2client`,
  `resourceserver`). Confirmação em [`research.md`](./research.md#nome-do-pacote-do-novo-domínio).
- **Renomeação da pasta da spec 005**: `005-acesso-cruzado-api-externa`, sem sufixo de feature, pela
  mesma convenção já usada pelas specs fundadoras dos domínios 1 a 3. Confirmação em
  [`research.md`](./research.md#renomeação-da-pasta-da-spec-005).
- **`references/technical-dependencies.md` para a nova skill**: criado, replicando o padrão já
  seguido pelos três domínios existentes (todos têm esse arquivo). Lista: `cliente-oauth2-client-assertion`
  (fornece o `OAuth2AuthorizedClientManager` usado para obter o `access_token`), `servidor-recursos-oauth2`
  (mantém `GET /api/cross` público na `SecurityFilterChain` que possui), `spring-boot-starter-web`
  (fornece o `RestClient` da chamada de saída) e OpenAPI/Swagger (documenta `GET /api/cross`).
  Confirmação em [`research.md`](./research.md#technical-dependenciesmd-para-a-nova-skill).

## Data model

Não se aplica: nenhuma entidade é persistida por este domínio. `data-model.md` dispensado.

## External contracts

Sem mudança de contrato: `GET /api/cross` mantém as mesmas anotações OpenAPI (`@Tag`, `@Operation`,
`@ApiResponse`) já existentes, apenas em outro pacote; o springdoc-openapi continua gerando a
especificação em tempo de execução a partir delas. `contracts/` dispensado, mesmo motivo já
registrado no plano da spec 005: duplicar o contrato em um arquivo mantido à mão não reduz
ambiguidade adicional para as tasks.

## Interface

Não se aplica — domínio puramente backend. `ui/` dispensado.

## Testing strategy

Esta POC não implementa testes automatizados (`discovery-answers.md`); nenhuma infraestrutura de
teste é introduzida por esta unidade. A verificação é manual, por comparação de requisições
`GET /api/cross` antes e depois da relocação de pacote (mesmos códigos de resposta `200`/`502` e
mesmo corpo de sucesso), reaproveitando o roteiro já descrito no `test-cases.md` relocado para
`005-acesso-cruzado-api-externa`; a fase de test-cases desta unidade (006) cobre especificamente a
verificação de que a relocação não alterou o comportamento observável.

## Optional artifacts

- `data-model.md` — dispensado (sem entidades).
- `research.md` — gerado: registra a confirmação das três decisões mecânicas desta unidade (nome do
  pacote, renomeação da pasta 005, criação do `technical-dependencies.md`).
- `contracts/` — dispensado (contrato OpenAPI gerado em tempo de execução, sem mudança).
- `ui/` — dispensado (sem interface).

## Impact on the authoritative documentation

Drift deliberado, decidido pela própria descrição desta unidade (`spec.md`), afetando três
documentos:

- **Skill `servidor-recursos-oauth2`** — a regra de negócio 4 ("`GET /api/cross` é um endpoint
  público") e a linha correspondente na tabela de "Entidades e dados" deixam de descrever
  `GET /api/cross` como regra própria deste domínio. Em ambos os pontos, passam a referenciar o
  domínio `acesso-cruzado-api-externa` como dono da regra, com a liberação do caminho na
  `SecurityFilterChain` tratada como pré-requisito daquele domínio implementado por este — mesma
  redação já usada para `GET /oauth2/jwks` (regra 6). Uma task na fase `tasks` faz essa edição.
- **Skill `acesso-cruzado-api-externa` (nova)** — criada por esta unidade, documentando a regra de
  negócio de `GET /api/cross` hoje dispersa na skill `servidor-recursos-oauth2`: classificação
  pública, chamada de saída a `GET /api/protected` da aplicação externa, uso do `access_token`
  obtido via `OAuth2AuthorizedClientManager`, propriedade `app.cross.external-base-url` e respostas
  `200`/`502`. Uma task na fase `tasks` cria essa skill e o `references/technical-dependencies.md`
  descrito em "Technical decisions".
- **`functional-map.md`** — ganha uma quarta entrada de domínio ("Acesso Cruzado a Aplicação
  Externa"), no mesmo formato das três já existentes (objetivo de negócio, evidência, dependências,
  regras inferidas, dependências externas, dependências técnicas, nível de confiança). A entrada do
  domínio 2 (Cliente OAuth2 com Client Assertion JWT) tem sua observação "o descritivo não inclui...
  a implementação dessa chamada a uma API específica (ex.: api-b) — trata-se apenas do cenário de
  contexto, fora do escopo obrigatório desta aplicação" atualizada: essa chamada está implementada,
  pelo domínio `acesso-cruzado-api-externa`. Uma task na fase `tasks` faz as duas edições.

Nenhum impacto em `references/technical-dependencies.md` da skill `servidor-recursos-oauth2`: esse
arquivo nunca citou as classes `CrossAccess*` nem `GET /api/cross`, então não há remoção a fazer ali.
