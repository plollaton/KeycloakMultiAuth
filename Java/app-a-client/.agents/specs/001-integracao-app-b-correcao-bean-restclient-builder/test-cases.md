# Test cases: Correção da falha de inicialização do cliente HTTP da App B

> Catálogo derivado estritamente de [`spec.md`](./spec.md) e [`plan.md`](./plan.md).
> Projeto tratado como POC, sem testes automatizados: este é o **runbook de validação
> manual** a ser executado no checkpoint de implementação, após aplicada a correção
> (construção do `appBRestClient` via `RestClient.builder()`).

## Pré-condições

- Maven e JDK 21 instalados; comandos executados a partir da raiz do projeto (não há `mvnw`).
- Variáveis de ambiente definidas por ambiente ou usando os defaults do `application.yml`:
  `APP_A_CLIENT_ID`, `APP_A_CLIENT_SECRET`, `KEYCLOAK_ISSUER_URI`, `APP_B_BASE_URL`.
- **Keycloak** acessível no `KEYCLOAK_ISSUER_URI`, com o cliente correspondente ao
  `APP_A_CLIENT_ID` habilitado para `client_credentials` e com o client scope `app-b.invoke`
  associado (pré-condição no lado do Keycloak) — necessário para os casos da História 2 que
  dependem de token real.
- **App B** acessível em `APP_B_BASE_URL`, expondo `GET /api/protegido` — necessária para os
  casos de sucesso da História 2. Onde um caso exige inspecionar a requisição recebida pela
  App B, usar a App B instrumentada para registrar as requisições (ou um substituto que ecoe os
  cabeçalhos recebidos).

## História 1 — Inicialização da aplicação sem erro de contexto

### TC-1 (obrigatório) — Aplicação inicializa sem a falha de bean e escuta na porta 8081

1. A partir da raiz do projeto, iniciar a aplicação com `mvn spring-boot:run`.
2. Acompanhar o log de inicialização até a aplicação reportar que subiu.

**Expected:** o contexto Spring carrega com sucesso; o log **não** contém
`APPLICATION FAILED TO START` nem
`Parameter 0 of method appBRestClient ... required a bean of type
'org.springframework.web.client.RestClient$Builder' that could not be found`; a aplicação
fica disponível escutando na porta `8081`. (A construção correta do `appBRestClient` — com
`baseUrl` e interceptor — é confirmada de forma observável pelos casos TC-2 e TC-3 da
História 2.)

## História 2 — Chamada à App B pelo endpoint de demonstração

### TC-2 (obrigatório) — Sucesso: repasse verbatim do corpo da App B com bearer token anexado

1. Com a aplicação no ar (TC-1) e a App B disponível em `APP_B_BASE_URL`, fazer uma requisição
   `GET /demo/chamar-app-b` na aplicação (porta `8081`).
2. Inspecionar a requisição que a App B recebeu (por exemplo, nos logs da App B).

**Expected:** a aplicação emite `GET {APP_B_BASE_URL}/api/protegido` com o cabeçalho
`Authorization: Bearer <jwt>` anexado automaticamente; a resposta de `GET /demo/chamar-app-b`
devolve, como texto, o corpo respondido pela App B **verbatim** (sem interpretação nem
transformação). Confirma também, de forma observável, que o `appBRestClient` foi construído com
o `baseUrl` de `app.app-b.base-url` e com o `OAuth2ClientHttpRequestInterceptor` (critério de
aceite da História 1).

### TC-3 (obrigatório) — Credencial sempre do registration `keycloak-client-credentials`

1. Com a aplicação no ar, fazer `GET /demo/chamar-app-b` **sem** enviar nenhuma credencial,
   sessão ou atributo de autenticação na requisição de entrada.
2. Inspecionar a requisição recebida pela App B.

**Expected:** a chamada de saída à App B carrega o bearer token obtido para o registration fixo
`keycloak-client-credentials`, independentemente de qualquer atributo da requisição de entrada;
não há seleção dinâmica de credencial por requisição.

### TC-4 (obrigatório) — Falha da App B propaga sem tradução para erro de negócio

1. Colocar a App B em um estado de falha declarado pelo spec: rejeição por credencial inválida
   ou escopo insuficiente (`401`/`403`), erro do serviço a jusante (`5xx`) ou App B
   indisponível no `APP_B_BASE_URL`.
2. Com a aplicação no ar, fazer `GET /demo/chamar-app-b`.

**Expected:** a falha da chamada à App B propaga como falha da requisição de demonstração, sem
tradução para uma resposta de erro de negócio própria da aplicação (comportamento atual
preservado — não há tratamento de erro especializado nesta integração).
