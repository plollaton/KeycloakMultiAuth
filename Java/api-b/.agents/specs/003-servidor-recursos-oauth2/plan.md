# Plan: Servidor de Recursos OAuth2

## Stack and structure

Aplicação Spring Boot 4.1.x / Java 21 / Jakarta EE 11, conforme `AGENTS.md`. O scaffold Maven e a
`SecurityFilterChain` já existem, criados pelo domínio "Gestão de Chaves RSA e Publicação JWKS"
(`com.aplicacaosegura.web.SecurityConfig`); este domínio é dono dessa `SecurityFilterChain` (skill
`servidor-recursos-oauth2`) e a modifica no lugar, sem migrá-la de pacote. Segue a convenção de
pacote por domínio de negócio já registrada no `research.md` do domínio 1, recebendo o pacote
próprio `com.aplicacaosegura.resourceserver` (endpoints de exemplo e validador de `aud`).

Novas dependências Maven: `spring-boot-starter-oauth2-resource-server` e
`spring-boot-starter-actuator`, que o `research.md` do domínio 1 já registrava como explicitamente
pendentes para quando os domínios que as configuram fossem implementados — este é esse domínio
para ambas.

## Technical decisions

Dependências técnicas do domínio (`.agents/skills/servidor-recursos-oauth2/references/technical-dependencies.md`)
que este spec cria, e como cada uma é montada:

- **`spring-boot-starter-oauth2-resource-server`**: configurado via
  `spring.security.oauth2.resourceserver.jwt.issuer-uri` (propriedade `KEYCLOAK_ISSUER_URI` por
  ambiente), habilitando a descoberta automática do JWKS do Keycloak pelo `JwtDecoder`
  autoconfigurado e a validação padrão de assinatura, `exp` e `iss` que essa autoconfiguração já
  entrega. Um `OAuth2TokenValidator<Jwt>` adicional injeta a validação de `aud` (claim não coberta
  pela autoconfiguração), combinado ao validador padrão via `DelegatingOAuth2TokenValidator` em um
  bean `JwtDecoder` explícito. Alternativas e confirmação em
  [`research.md`](./research.md#mecanismo-de-validacao-do-access_token).
- **Valor esperado de `aud`**: propriedade própria da aplicação (`app.security.resource-server.expected-audience`,
  variável de ambiente `RESOURCE_SERVER_EXPECTED_AUDIENCE`) lida pelo validador de claim
  customizado — a skill de domínio já registra que esse valor é específico do ambiente de
  implantação e não é fixado pelo material de negócio; este plano não o fixa, apenas define onde
  vive.
- **Classificação de endpoints na `SecurityFilterChain`**: `SecurityConfig` (pacote `web`, já
  existente) ganha `GET /api/public` e `GET /actuator/health` na lista de caminhos com
  `permitAll()`, ao lado do caminho já liberado pelo domínio "Gestão de Chaves RSA e Publicação
  JWKS" (`/oauth2/jwks`, Swagger/OpenAPI). `GET /api/protected`
  não entra nessa lista: permanece coberto por `anyRequest().authenticated()`, que passa a ter
  efeito prático pela primeira vez com a adição de `.oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))`
  à mesma cadeia — o `JwtDecoder` customizado acima é detectado automaticamente pelo configurador,
  sem precisar ser referenciado explicitamente no `SecurityConfig`. Rationale em
  [`research.md`](./research.md#endpoints-de-exemplo-e-corpo-de-resposta).
- **`spring-boot-starter-actuator`**: dependência direta; `GET /actuator/health` já é exposto
  publicamente pela configuração padrão do starter (nenhuma propriedade
  `management.endpoints.web.exposure.include` adicional é necessária além de manter o caminho
  liberado na `SecurityFilterChain`, item anterior).
- **Endpoints de exemplo**: `@RestController` no pacote `com.aplicacaosegura.resourceserver` expõe
  `GET /api/protected` e `GET /api/public`, cada um respondendo `200` com um corpo mínimo que só
  evidencia a classificação de acesso (ex.: `{"acesso": "protegido"}` / `{"acesso": "publico"}`) —
  o material de negócio não fixa um contrato de resposta além da classificação (`spec.md`). Ambos
  recebem `@Tag`, `@Operation` e `@ApiResponse` do springdoc-openapi, seguindo o padrão da skill
  técnica `documentacao-api-openapi`, já em uso nos domínios 1 e 2.

## Data model

Não se aplica: este domínio não persiste nenhuma entidade. `data-model.md` dispensado.

## External contracts

O contrato de `GET /api/protected` e `GET /api/public` já está totalmente especificado nos
critérios de aceite do `spec.md` (`200`, corpo mínimo evidenciando a classificação de acesso,
`401` nas falhas de validação do `access_token`). Como nos domínios 1 e 2, a especificação OpenAPI
é gerada em tempo de execução pelo springdoc-openapi a partir das anotações dos controllers —
pasta `contracts/` dispensada pelo mesmo motivo já registrado nos planos daqueles domínios:
duplicar o mesmo contrato em um arquivo mantido à mão não reduziria ambiguidade adicional para as
tasks. `GET /actuator/health` segue o contrato padrão do Spring Boot Actuator, fora do escopo de
documentação OpenAPI deste projeto.

## Interface

Não se aplica — domínio puramente backend, sem interface própria. `ui/` dispensada.

## Testing strategy

Esta POC não implementa testes automatizados (unitários, integração ou e2e), conforme decisão
registrada em `.agents/context/discovery-answers.md`. Nenhuma infraestrutura de teste é introduzida
por esta unidade; a verificação do fluxo depende de um `access_token` real emitido por um Keycloak
configurado com `issuer-uri` e `aud` correspondentes ao ambiente, conforme já registrado nos riscos
do `spec.md`.

## Impact on the authoritative documentation

Nenhum impacto. A skill `servidor-recursos-oauth2` já descreve corretamente o comportamento de
negócio que este plano implementa (classificação de endpoints, validação de assinatura contra o
JWKS do Keycloak e das claims `iss`/`exp`/`aud`, rejeição antes da lógica do endpoint). Não há
divergência entre a skill, o `spec.md` e as decisões técnicas deste plano.
