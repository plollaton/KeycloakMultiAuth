# Tasks: Gestão de Chaves RSA e Publicação JWKS

Esta POC não implementa testes automatizados (`discovery-answers.md`); a validação de cada tarefa
é manual, pelos casos correspondentes em `test-cases.md`.

- [x] **T1. Carregamento da chave RSA e endpoint público de publicação JWKS**
  - Depends on: none
  - Prerequisito: scaffold Maven do projeto — `pom.xml` (`groupId` `com.aplicacaosegura`,
    `artifactId` `aplicacao-segura`, parent `spring-boot-starter-parent` na linha 4.1.x,
    dependências `spring-boot-starter-web`, `spring-boot-starter-security` e
    `com.nimbusds:nimbus-jose-jwt`), classe de aplicação `@SpringBootApplication` e layout
    `src/main/java/com/aplicacaosegura` — nenhum desses artefatos existe ainda no repositório.
  - Endpoint `GET /oauth2/jwks` retornando um `JWKSet` somente com a chave pública ativa, nos
    campos `kty`/`kid`/`use`/`alg`/`n`/`e`.
  - `SecurityFilterChain` liberando `GET /oauth2/jwks` de autenticação, com os demais caminhos
    exigindo autenticação por padrão.
  - Cobre a Story 1 e a rotação por substituição dos arquivos de chave da Story 2, do `spec.md`
    (`TC-1` a `TC-5`).

- [x] **T2. Carregamento do par de chaves RSA a partir de arquivo fixo**
  - Depends on: T1
  - Componente, no pacote `com.aplicacaosegura.jwks`, que carrega o par de chaves RSA ativo
    (2048 bits, RS256) do par fixo de arquivos `api-a-private.pem` (chave privada, PKCS8) e
    `api-a-cert.pem` (certificado X.509 com a chave pública), ambos em `src/main/resources`, com
    `kid` igual ao número de série do certificado.
  - Cobre o carregamento fixo da Story 3, do `spec.md` (`TC-6`, `TC-7`).

- [x] **T3. Documentação OpenAPI/Swagger do endpoint JWKS**
  - Depends on: T1
  - Dependência `springdoc-openapi-starter-webmvc-ui` (linha 2.x compatível com Spring Boot
    4.1.x).
  - Anotações `@Tag`, `@Operation` e `@ApiResponse` no controller de T1, e `@Schema` nos campos
    de resposta (`kty`/`kid`/`use`/`alg`/`n`/`e`).
  - Extensão da `SecurityFilterChain` de T1, liberando `GET /v3/api-docs/**`, `GET /swagger-ui/**`
    e `GET /swagger-ui.html` de autenticação.
  - Cobre a Story 4 do `spec.md` (`TC-9`, `TC-10`).
