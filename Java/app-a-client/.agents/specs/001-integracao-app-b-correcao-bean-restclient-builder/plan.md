# Plan: Correção da falha de inicialização do cliente HTTP da App B

## Estado atual do código

- Stack conforme `AGENTS.md`: Java 21, Spring Boot `4.0.6` (parent `spring-boot-starter-parent`),
  build Maven. Starters em uso: `spring-boot-starter-web`, `spring-boot-starter-oauth2-client`,
  `spring-boot-starter-actuator`.
- `com.dbfinanceira.appa.config.OAuth2RestClientConfig` declara três beans: `authorizedClientService`
  e `authorizedClientManager` (domínio Autenticação Máquina-a-Máquina) e `appBRestClient` (este
  domínio, Integração com a App B). O `appBRestClient` recebe hoje um `RestClient.Builder` por
  injeção de parâmetro e constrói o `RestClient` com `.baseUrl(app.app-b.base-url)`,
  `.requestInterceptor(OAuth2ClientHttpRequestInterceptor)` e resolvedor fixo do registration
  `keycloak-client-credentials`.
- `AppBClient` e `DemoController` consomem o `appBRestClient` para chamar `GET /api/protegido` e
  expor `GET /demo/chamar-app-b`.
- **Falha atual:** o container Spring não encontra um bean do tipo
  `org.springframework.web.client.RestClient$Builder` para o parâmetro 0 de `appBRestClient`, e a
  aplicação aborta a inicialização (`APPLICATION FAILED TO START`). A caminhada desta correção parte
  desse estado até o contexto subir novamente, tocando apenas a construção do `appBRestClient`.

## Decisões técnicas

- **Construir o `RestClient` sem depender do bean `RestClient.Builder` autoconfigurado.** O parâmetro
  `RestClient.Builder builder` do método `appBRestClient` é substituído pela fábrica estática
  `RestClient.builder()` (API do próprio `spring-web`), mantendo `.baseUrl(...)`,
  `.requestInterceptor(...)` e `.build()`. Remove a dependência ausente que impede a subida do
  contexto, sem introduzir biblioteca, framework ou dependência nova. Alternativas consideradas e a
  fundamentação/procedência em [`research.md`](./research.md).
- **Preservação de comportamento.** Mantêm-se intactos o `OAuth2ClientHttpRequestInterceptor`, o
  resolvedor fixo do registration `keycloak-client-credentials` e o `baseUrl` via
  `app.app-b.base-url`. Nenhum outro bean é alterado — em particular, os beans do domínio de
  Autenticação (`authorizedClientService`, `authorizedClientManager`) permanecem inalterados, em
  linha com a constraint global de preservar a autenticação `client_credentials`.

## Modelo de dados

Não se aplica — o domínio não possui persistência nem entidades (o access token é mantido em memória
pelo domínio de Autenticação; não há banco de dados). `data-model.md` dispensado.

## Contratos externos

Nem o endpoint exposto `GET /demo/chamar-app-b` nem o consumido `GET /api/protegido` mudam nesta
correção; não há contrato novo ou alterado. Pasta `contracts/` dispensada, e a documentação OpenAPI
existente permanece válida sem ajuste.

## Interface

Não se aplica — domínio backend, sem interface de usuário. `ui/` dispensado.

## Estratégia de testes

Sem testes automatizados: decisão registrada (`discovery-answers.md`, `AGENTS.md`) de tratar o projeto
como POC; a dependência `spring-boot-starter-test` permanece no `pom.xml`, mas não há suíte a manter e
nenhuma infra de teste a introduzir. Validação **manual**, cobrindo os critérios de aceite do `spec.md`:

1. `mvn spring-boot:run` — o contexto carrega sem o erro de bean `RestClient.Builder` e a aplicação
   escuta na porta `8081`.
2. Com a App B disponível no endereço de `APP_B_BASE_URL`, acionar `GET /demo/chamar-app-b` e conferir
   que a chamada de saída para `GET /api/protegido` sai com `Authorization: Bearer <jwt>` anexado e que
   o corpo respondido pela App B é repassado verbatim.

## Artefatos opcionais

- `data-model.md` — dispensado (domínio sem modelo de dados/persistência).
- `research.md` — **gerado** (decisão não trivial: origem da dependência ausente e escolha da
  abordagem de correção).
- `contracts/` — dispensado (contrato REST inalterado por esta correção).
- `ui/` — dispensado (domínio sem interface).

## Impacto na documentação autoritativa

Sem impacto. A skill `integracao-app-b` já descreve o comportamento correto (o cliente HTTP de saída
existe no contexto, anexa a credencial de máquina e consome a App B). Esta correção faz o código voltar
a atender o que a doc descreve — não há drift deliberado e, portanto, nenhuma tarefa de atualização de
documentação nasce daqui.
