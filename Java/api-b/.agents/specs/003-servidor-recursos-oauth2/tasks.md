# Tasks: Servidor de Recursos OAuth2

Esta POC não implementa testes automatizados (`discovery-answers.md`); a validação de cada tarefa
é manual, pelos casos correspondentes em `test-cases.md`.

- [x] **T1. Validação de access_token e classificação de endpoints na SecurityFilterChain**
  - Depends on: none
  - Prerequisito: dependências `spring-boot-starter-oauth2-resource-server` e
    `spring-boot-starter-actuator` no `pom.xml`, ainda não presentes.
  - `JwtDecoder` configurado via `spring.security.oauth2.resourceserver.jwt.issuer-uri`, com um
    `OAuth2TokenValidator<Jwt>` adicional que valida a claim `aud` contra
    `app.security.resource-server.expected-audience`, combinado ao validador padrão de
    assinatura/`exp`/`iss` via `DelegatingOAuth2TokenValidator`.
  - `SecurityConfig` (pacote `web`) passa a incluir `GET /api/public` e `GET /actuator/health` na
    lista de caminhos com `permitAll()`, ao lado dos já liberados pelo domínio "Gestão de Chaves
    RSA e Publicação JWKS", e a aplicar
    `.oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))` na mesma cadeia, cobrindo
    `GET /api/protected` por `anyRequest().authenticated()`.
  - `@RestController` no pacote `com.aplicacaosegura.resourceserver` expõe `GET /api/protected` e
    `GET /api/public`, cada um documentado via OpenAPI/Swagger (`@Tag`/`@Operation`/`@ApiResponse`).
  - Cobre as Histórias 1, 2 e 3 do `spec.md` (`TC-1` a `TC-11`).
