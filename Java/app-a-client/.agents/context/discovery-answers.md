---
name: discovery-answers
description: Memória durável da descoberta do app-a-client (cliente OAuth2 client_credentials para Keycloak que chama a App B), com objetivo, escopo, restrições declaradas, decisões transversais validadas (documentação via OpenAPI, sem testes automatizados na POC, secret mantido no yml) e o log das decisões humanas resolvidas.
metadata:
  author: clovis-cli
  responsibility: "Durable memory of the functional-discovery context and decisions: objective, scope, declared restrictions, validated cross-cutting decisions (with destination), documentation forms to maintain and the log of human decisions resolved in the gap loop. Source re-read by the following stages; its restrictions take precedence over later inferences."
---

# Memória de Descoberta — app-a-client

## Objetivo

Backend de uma POC de integração autenticada com Keycloak. Este projeto é a **aplicação cliente (App A)**: ela se autentica no Keycloak pelo fluxo OAuth2 `client_credentials` (máquina-a-máquina), obtém um access token (JWT) e o utiliza para fazer requisições a uma **aplicação servidora (App B)**, que valida a assinatura da chave via JWKS.

O trabalho é de **manutenção** sobre o projeto existente, com escopo em todo o projeto.

## Escopo

- Tipo: `whole_project` (projeto inteiro).
- Modo de trabalho: `maintenance` (não há reescrita; não há repositório legado).
- Idioma dos artefatos: português do Brasil (`pt`).

## Restrições declaradas pelo usuário

- **Preservar a autenticação atual, mas serão necessários ajustes.** O fluxo OAuth2 `client_credentials` contra o Keycloak deve ser mantido como base; a natureza exata dos ajustes ainda precisa de validação humana (ver lacuna `auth-adjustments`).

## Stack alvo (evidência no código)

- Java 21, Spring Boot `4.0.6` (parent `spring-boot-starter-parent`), build via Maven (`pom.xml`).
- `spring-boot-starter-web` (API REST), `spring-boot-starter-oauth2-client` (cliente OAuth2), `spring-boot-starter-actuator` (observabilidade), `spring-boot-starter-test` (testes, presente mas sem testes escritos).
- O `package.json` na raiz contém dependências (`latest`, `nvm`) sem relação com a aplicação Java — ruído, não faz parte da stack alvo.

## Formas de documentação a manter

**Swagger/OpenAPI via `springdoc-openapi`.** Decisão validada pelo usuário (lacuna `doc-forms`). Nenhuma forma estava adotada hoje; passa a ser mantida a documentação do contrato REST via OpenAPI. Por ser um how-to reutilizável, tem destino em `technicalSkills` como a skill técnica `documentacao-api-openapi`. Coleção Postman e ADRs não serão mantidos.

## Decisões transversais validadas por evidência

- **Estratégia de persistência:** não há banco de dados. O token de acesso é mantido em memória via `InMemoryOAuth2AuthorizedClientService` (`OAuth2RestClientConfig`). Adequado a uma POC. Destino: convenção no `AGENTS.md` e refletida no domínio de Autenticação.
- **Observabilidade:** Spring Boot Actuator habilitado (`spring-boot-starter-actuator`) e logging de `org.springframework.security` em nível `INFO` (`application.yml`). Destino: convenção no `AGENTS.md`.
- **Fluxo de autenticação M2M:** OAuth2 `client_credentials` com descoberta automática de `token-uri`/`jwks-uri` via `issuer-uri` (`/.well-known/openid-configuration`); token anexado automaticamente pelo `OAuth2ClientHttpRequestInterceptor`. Por carregar regra de negócio própria da POC, é tratado como **domínio** (ver `functional-map.md`), não como skill técnica.
- **Configuração externalizada:** endpoints e credenciais parametrizados por variáveis de ambiente (`APP_A_CLIENT_ID`, `APP_A_CLIENT_SECRET`, `KEYCLOAK_ISSUER_URI`, `APP_B_BASE_URL`) com defaults no `application.yml`. Destino: convenção no `AGENTS.md`.
- **Gestão do client-secret:** decisão validada (lacuna `auth-adjustments`) de **manter o `client-secret` no `application.yml` por enquanto**, evoluindo depois para uma solução mais segura (ex.: cofre de segredos). Destino: convenção no `AGENTS.md` e refletida no domínio de Autenticação. Este é, por ora, o único ajuste confirmado à autenticação; o restante do fluxo `client_credentials` é preservado como está.
- **Estratégia de testes:** decisão validada (lacuna `testing-strategy`) de **não manter testes automatizados**, tratando o projeto como POC. A dependência `spring-boot-starter-test` permanece no `pom.xml`, mas não há suíte a manter. Destino: convenção no `AGENTS.md`.

## Framework spec-driven concorrente

Não detectado. Não existem diretórios `.specify/`, `openspec/` nem artefatos concorrentes de spec/plan/tasks. Nenhuma lacuna aberta a esse respeito.

## Log de decisões humanas resolvidas no loop de lacunas

- **`doc-forms`** — Qual forma de documentação adicional manter (nenhuma adotada hoje). **Decisão:** manter Swagger/OpenAPI via `springdoc-openapi`. Vira a skill técnica `documentacao-api-openapi`. Postman e ADRs descartados.
- **`testing-strategy`** — Qual estratégia de testes automatizados adotar (nenhum teste existente). **Decisão:** sem testes automatizados; manter como POC. Convenção no `AGENTS.md`.
- **`auth-adjustments`** — Quais ajustes são necessários na autenticação atual. **Decisão:** manter o `client-secret` no `application.yml` por enquanto, evoluindo depois para uma solução mais segura. Nenhum outro ajuste confirmado nesta rodada; o fluxo `client_credentials` é preservado.
