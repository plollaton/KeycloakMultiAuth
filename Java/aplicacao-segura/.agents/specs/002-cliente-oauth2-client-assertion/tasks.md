# Tasks: Cliente OAuth2 com Client Assertion JWT

Esta POC não implementa testes automatizados (`discovery-answers.md`); a validação de cada tarefa
é manual, pelos casos correspondentes em `test-cases.md`.

- [x] **T1. Atualização da skill de domínio para registrar o endpoint de diagnóstico**
  - Depends on: none
  - Atualiza `.agents/skills/cliente-oauth2-client-assertion/SKILL.md`: a seção "Visão geral do
    domínio" passa a registrar a existência de `GET /diagnostics/oauth2-client-assertion` como
    endpoint exclusivamente diagnóstico (não é uma funcionalidade de negócio e nunca devolve o
    `access_token`), e a seção "Restrições e validações" deixa de afirmar que o domínio não
    integra a documentação OpenAPI/Swagger do projeto, já que esse endpoint passa a integrá-la.
  - Origem: História 4 e os critérios de aceite correspondentes do `spec.md`, decisão registrada
    no gap `mecanismo-disparo-validacao-manual-client-assertion`.

- [x] **T2. Endpoint de diagnóstico da troca client_assertion → access_token**
  - Depends on: T1
  - Precondition: um client desta aplicação configurado no Keycloak para autenticação via JWT
    assinado (`private_key_jwt`), com a fonte de validação apontando para `GET /oauth2/jwks`
    desta aplicação — configuração externa ao Keycloak, fora do escopo desta tarefa.
  - Prerequisito: dependência `spring-boot-starter-oauth2-client` no `pom.xml`, ainda não
    presente.
  - `ClientRegistrationRepository` (via `application.yml`) com uma registration para o grant
    `client_credentials` e `client-authentication-method: private_key_jwt` (`client-id` e
    `token-uri`/`issuer-uri` como propriedades a preencher por ambiente), com o `client_assertion`
    (claims `iss`, `sub`, `aud`, `exp`, `iat`, `jti`) montado e assinado via
    `NimbusJwtClientAuthenticationParametersConverter`, resolvendo a chave de assinatura a partir
    do bean `RSAKey activeRsaKey()` já publicado pelo domínio "Gestão de Chaves RSA e Publicação
    JWKS".
  - `OAuth2AuthorizedClientManager` para obter e reutilizar o `access_token` dessa troca,
    consumido por um endpoint `GET /diagnostics/oauth2-client-assertion` — público na
    `SecurityFilterChain` existente, documentado via OpenAPI/Swagger — que responde com a
    confirmação de sucesso ou falha da troca, sem devolver o `access_token`.
  - Cobre as Histórias 1 a 4 do `spec.md` (`TC-1` a `TC-7`).
