# Tasks: Correção da falha de inicialização do cliente HTTP da App B

- [x] **T1. Corrigir a construção do bean `appBRestClient` para a aplicação inicializar**
  - Ajustar o bean `appBRestClient` em `com.dbfinanceira.appa.config.OAuth2RestClientConfig`
    para que o contexto Spring inicialize sem depender do bean `RestClient.Builder` ausente
    (causa da falha `APPLICATION FAILED TO START`), conforme a abordagem já decidida em
    [`plan.md`](./plan.md) e [`research.md`](./research.md).
  - Preservar integralmente o comportamento atual do cliente de saída: `baseUrl` a partir de
    `app.app-b.base-url`, o `OAuth2ClientHttpRequestInterceptor` e o resolvedor fixo do
    registration `keycloak-client-credentials`.
  - Não alterar os demais beans — os do domínio de Autenticação
    (`authorizedClientService`, `authorizedClientManager`) permanecem intactos — nem o
    contrato dos endpoints (`GET /demo/chamar-app-b`, `GET /api/protegido`).
  - Projeto tratado como POC, sem testes automatizados: não há testes a incluir nesta tarefa;
    a verificação de ponta a ponta é manual, pelo runbook [`test-cases.md`](./test-cases.md),
    executado no checkpoint de implementação (fora desta lista).
