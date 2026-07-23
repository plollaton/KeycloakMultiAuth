---
name: functional-map
description: Mapa dos domínios de negócio do app-a-client — Autenticação Máquina-a-Máquina (client_credentials no Keycloak, secret mantido no yml por ora) e Integração com a App B (consumo da API protegida, documentada via OpenAPI) — com fronteiras, dependências e ordem sugerida. Sem lacunas abertas.
metadata:
  author: clovis-cli
  responsibility: "Map of identification of the business domains (bounded contexts), their boundaries, dependencies and suggested implementation order. Index of domains for skill generation and the spec-driven flow; it does not detail business rules nor duplicate the cross-cutting decisions, which live in the discovery-answers.md."
---

# Mapa Funcional — app-a-client

Aplicação cliente (App A) de uma POC de integração autenticada com Keycloak. Dois contextos de negócio se distinguem no código: **obter a credencial de máquina** e **usá-la para consumir a App B**.

---

## Domínio 1 — Autenticação Máquina-a-Máquina (Client Credentials)

- **Objetivo de negócio:** autenticar a própria aplicação junto ao Keycloak pelo fluxo OAuth2 `client_credentials` (sem usuário/sessão), obtendo e renovando automaticamente um access token (JWT) que autoriza chamadas a serviços protegidos a jusante.
- **Evidence in the code:**
  - `src/main/java/com/dbfinanceira/appa/config/OAuth2RestClientConfig.java` — `AuthorizedClientServiceOAuth2AuthorizedClientManager`, provider `clientCredentials()`, `InMemoryOAuth2AuthorizedClientService`.
  - `src/main/resources/application.yml` — `spring.security.oauth2.client.registration.keycloak-client-credentials` (`client-id`, `client-secret`, `authorization-grant-type: client_credentials`, `scope: app-b.invoke`) e `provider.keycloak.issuer-uri`.
  - `pom.xml` — `spring-boot-starter-oauth2-client`.
- **Dependências entre domínios:** nenhuma (é o domínio fundacional).
- **Regras inferidas (nível de identificação):** descoberta automática de `token-uri`/`jwks-uri` via `issuer-uri`; token guardado em memória e renovado antes de cada chamada; escopo `app-b.invoke` restringe o que o token permite. O `client-secret` é mantido no `application.yml` por enquanto (decisão validada, ver `discovery-answers.md`), com evolução futura para uma solução mais segura; o fluxo `client_credentials` é preservado sem outros ajustes nesta rodada. Não há testes automatizados neste domínio (projeto tratado como POC).
- **Domain technical dependencies:**
  - **Keycloak** — provedor de identidade que emite o access token via `client_credentials` e expõe a descoberta OpenID Connect (`/.well-known/openid-configuration`); sem ele a aplicação não obtém credencial e nenhuma chamada a jusante é autorizada.
  - **Spring Security OAuth2 Client** (`spring-boot-starter-oauth2-client`) — implementa o gerenciamento do client autorizado e a obtenção/renovação do token; sem ele o fluxo M2M não existe.
  - **Configuração externalizada por variáveis de ambiente** (`APP_A_CLIENT_ID`, `APP_A_CLIENT_SECRET`, `KEYCLOAK_ISSUER_URI`) — define credenciais e o realm/issuer alvo; se ausente, cai nos defaults do `application.yml` (`app-a`/`changeit`/`keycloak.example.com`), inadequados fora de desenvolvimento.
- **Relevant external dependencies:** `Keycloak`, `Spring Security OAuth2 Client`.
- **Confiança:** high.
- **Lacunas abertas:** nenhuma.

---

## Domínio 2 — Integração com a App B (consumo de API protegida)

- **Objetivo de negócio:** consumir o endpoint protegido da App B (`/api/protegido`) em nome da própria aplicação, anexando automaticamente o bearer token obtido no domínio de Autenticação, e expor um endpoint de demonstração (`/demo/chamar-app-b`) para acionar e validar o fluxo ponta a ponta.
- **Evidence in the code:**
  - `src/main/java/com/dbfinanceira/appa/client/AppBClient.java` — `RestClient` que chama `GET /api/protegido`.
  - `src/main/java/com/dbfinanceira/appa/web/DemoController.java` — `GET /demo/chamar-app-b`.
  - `src/main/java/com/dbfinanceira/appa/config/OAuth2RestClientConfig.java` — bean `appBRestClient` com `baseUrl` e `OAuth2ClientHttpRequestInterceptor` (resolve sempre o registration `keycloak-client-credentials`).
  - `src/main/resources/application.yml` — `app.app-b.base-url` e `app.app-b.registration-id`.
- **Dependências entre domínios:** depende de **Autenticação Máquina-a-Máquina** (precisa do token para autorizar a chamada).
- **Regras inferidas (nível de identificação):** o interceptor anexa `Authorization: Bearer <jwt>` a cada requisição; o alvo (base-url) e o registration são parametrizáveis; o endpoint de demo é apenas gatilho manual do fluxo. O contrato REST exposto (endpoint de demonstração) passa a ser documentado via OpenAPI (`springdoc-openapi`), decisão validada — ver a skill técnica `documentacao-api-openapi` no `discovery-answers.md`.
- **Domain technical dependencies:**
  - **App B** — serviço REST a jusante que expõe `/api/protegido` e valida o JWT via JWKS; sem ele não há destino para a integração.
  - **Domínio de Autenticação Máquina-a-Máquina** — fornece o token anexado pelo interceptor; sem ele as chamadas à App B seriam rejeitadas (401).
  - **Spring Web `RestClient`** (`spring-boot-starter-web`) — cliente HTTP usado para chamar a App B e para expor o endpoint de demonstração.
  - **Configuração externalizada** (`APP_B_BASE_URL`) — define o endereço da App B; se ausente, usa o default `http://localhost:8082`.
- **Relevant external dependencies:** `App B` (serviço REST a jusante), `Spring Web RestClient`, `springdoc-openapi` (documentação OpenAPI do endpoint exposto), `Keycloak` (indiretamente, via token do Domínio 1).
- **Confiança:** high.
- **Lacunas abertas:** nenhuma específica deste domínio.

---

## Ordem sugerida de implementação

1. **Autenticação Máquina-a-Máquina** (fundacional; nenhuma dependência).
2. **Integração com a App B** (depende da Autenticação).
