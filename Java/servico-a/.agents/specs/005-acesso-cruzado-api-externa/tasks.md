# Tasks: Endpoint Público de Acesso Cruzado a Aplicação Externa (GET /api/cross)

Esta POC não implementa testes automatizados (`discovery-answers.md`); a validação de cada tarefa
é manual, pelos casos correspondentes em `test-cases.md`.

- [x] **T1. Atualizar a skill `servidor-recursos-oauth2` com o novo endpoint público**
  - Depends on: none
  - Alvo: `.agents/skills/servidor-recursos-oauth2/SKILL.md`.
  - Seções afetadas: "Regras de negócio" (lista de endpoints classificados, regras 1 a 5) e
    "Entidades e dados" (tabela de classificação de endpoints).
  - Mudança esperada: incluir `GET /api/cross` como endpoint público desta aplicação, ao lado dos
    já documentados (`GET /api/public`, `GET /actuator/health`, `GET /oauth2/jwks`), sem exigência
    de `access_token` do chamador.
  - Origem: decisão humana registrada no gap `g1` da fase de spec desta unidade (`GET /api/cross`
    é público).

- [x] **T2. Endpoint GET /api/cross com acesso cruzado à aplicação externa**
  - Depends on: T1
  - `SecurityConfig` (pacote `web`) passa a incluir `GET /api/cross` na lista de caminhos com
    `permitAll()`, ao lado dos demais endpoints públicos já liberados na mesma
    `SecurityFilterChain`.
  - `RestClient` configurado com a base URL lida de `app.cross.external-base-url` (sem valor
    padrão em nenhum `application.yml`, resolvida pela variável informada na linha de comando de
    inicialização).
  - `CrossAccessController` (pacote `resourceserver`) expõe `GET /api/cross`: obtém o
    `access_token` através do `OAuth2AuthorizedClientManager` já existente (registration
    `keycloak`), chama `GET /api/protected` da aplicação externa configurada apresentando esse
    token, e responde `200` (`CrossAccessResponse`) quando a chamada de saída é aceita ou `502`
    quando a obtenção do token falha, o token é rejeitado pela aplicação externa, ou a aplicação
    externa está inacessível — sem incluir o `access_token` nem material de chave privada na
    resposta.
  - `GET /api/cross` documentado via OpenAPI/Swagger (`@Tag`/`@Operation`/`@ApiResponse`), seguindo
    a convenção já em uso nos demais endpoints do projeto.
  - Cobre as Histórias 1, 2 e 3 do `spec.md` (`TC-1` a `TC-8`).
