# Spec: Servidor de Recursos OAuth2

## Overview

Proteção dos endpoints HTTP desta aplicação atuando como OAuth2 Resource Server: classificação de cada endpoint em público ou protegido, e validação — para os protegidos — do `access_token` apresentado, contra o JWKS do Keycloak e as claims `iss`, `exp` e `aud`.

## Domain

- Slug: `servidor-recursos-oauth2`
- Skill: [`.agents/skills/servidor-recursos-oauth2/SKILL.md`](../../skills/servidor-recursos-oauth2/SKILL.md)

## Estado atual do código

O `pom.xml` não contém `spring-boot-starter-oauth2-resource-server` nem `spring-boot-starter-actuator`. O `SecurityConfig` (`src/main/java/com/aplicacaosegura/web/SecurityConfig.java`) define a única `SecurityFilterChain` da aplicação, hoje limitada a liberar como públicos `GET /oauth2/jwks` e os caminhos do Swagger/OpenAPI, exigindo autenticação para qualquer outra requisição (`anyRequest().authenticated()`) sem nenhum mecanismo de autenticação configurado — nenhuma requisição a um endpoint fora da lista de liberados pode ser atendida com sucesso hoje. Não existem os endpoints `GET /api/protected`, `GET /api/public` nem `GET /actuator/health`, e a aplicação não valida JWTs em nenhum momento. Este domínio ainda não tem nenhuma parte implementada.

## Scope

**In:**

- Classificação de endpoints desta aplicação em públicos e protegidos.
- Endpoint de exemplo protegido, que exige `access_token` válido.
- Endpoints de exemplo públicos, incluindo a verificação de saúde da aplicação.
- Validação de `access_token` apresentado a um endpoint protegido: assinatura contra o JWKS do Keycloak, e claims `iss`, `exp` e `aud`.
- Rejeição de requisições a endpoints protegidos sem `access_token` válido, antes de alcançar a lógica de negócio do endpoint.

**Out:**

- Geração, rotação e publicação do par de chaves RSA e do JWKS próprio da aplicação — domínio "Gestão de Chaves RSA e Publicação JWKS".
- Validação de `access_tokens` feita por outras aplicações do cenário (ex.: api-b atuando como seu próprio resource server).
- Qualquer lógica de negócio específica dentro dos endpoints de exemplo além de confirmar se a requisição foi autenticada — o material de negócio fixa apenas a classificação de acesso, não um contrato de resposta.

## Domain boundary

**This spec implements:**

- `SecurityFilterChain` (`SecurityConfig`, hoje sem configuração de resource server) configurada com validação de JWT via `spring-boot-starter-oauth2-resource-server`, usando o JWKS do Keycloak (nunca o JWKS próprio publicado pelo domínio "Gestão de Chaves RSA e Publicação JWKS").
- Validador de claims composto (`iss`, `exp` e `aud`) do `access_token`, aplicado na mesma `SecurityFilterChain`.
- Endpoint `GET /api/protected`, exigindo `access_token` válido.
- Endpoint `GET /api/public`, liberado na `SecurityFilterChain` sem exigir autenticação.
- Dependência `spring-boot-starter-actuator` e liberação de `GET /actuator/health` na `SecurityFilterChain` — infraestrutura transversal que este domínio cria por ser o primeiro a precisar dela, já que o `pom.xml` atual não contém essa dependência.
- Dependência `spring-boot-starter-oauth2-resource-server` — infraestrutura transversal que este domínio cria por ser o primeiro a precisar dela.
- Documentação via OpenAPI/Swagger de `GET /api/protected` e `GET /api/public`, seguindo a convenção já em uso no projeto (`.agents/skills/documentacao-api-openapi/SKILL.md`).

**Belongs to other domains (cross-domain, does not become a task here):**

- Geração, rotação e publicação do par de chaves RSA e de `GET /oauth2/jwks` → domínio "Gestão de Chaves RSA e Publicação JWKS"; este domínio apenas preserva a liberação pública já existente desse endpoint na `SecurityFilterChain`.
- Emissão de `access_tokens` e publicação do JWKS do emissor → responsabilidade do Keycloak, sistema externo.

## User stories

1. Como chamador autorizado pelo Keycloak, quero acessar `GET /api/protected` apresentando um `access_token` válido, para consumir um endpoint protegido desta aplicação.
2. Como chamador sem credenciais, quero acessar `GET /api/public` e `GET /actuator/health` sem apresentar `access_token`, para consumir endpoints públicos desta aplicação.
3. Como operador desta aplicação, quero que requisições a `GET /api/protected` sem `access_token` válido sejam rejeitadas antes de alcançar a lógica do endpoint, para impedir acesso não autorizado.

## Acceptance criteria

**História 1 — Acesso protegido:**

- Dado um `access_token` emitido pelo Keycloak, com assinatura válida contra o JWKS do Keycloak e claims `iss`, `exp` (não expirada) e `aud` correspondentes aos valores configurados para o ambiente, quando o chamador executa `GET /api/protected` apresentando esse `access_token`, então a aplicação responde `200`.
- Dado um `access_token` com assinatura que não corresponde ao JWKS do Keycloak, quando o chamador executa `GET /api/protected`, então a aplicação responde `401` e a requisição não alcança a lógica do endpoint.
- Dado um `access_token` com claim `exp` expirada, quando o chamador executa `GET /api/protected`, então a aplicação responde `401` e a requisição não alcança a lógica do endpoint.
- Dado um `access_token` com claim `iss` ou `aud` diferente do valor configurado para o ambiente, quando o chamador executa `GET /api/protected`, então a aplicação responde `401` e a requisição não alcança a lógica do endpoint.
- Dado nenhum `access_token` apresentado, quando o chamador executa `GET /api/protected`, então a aplicação responde `401` e a requisição não alcança a lógica do endpoint.

**História 2 — Acesso público:**

- Dado nenhum `access_token` apresentado, quando o chamador executa `GET /api/public`, então a aplicação responde `200`.
- Dado nenhum `access_token` apresentado, quando o chamador executa `GET /actuator/health`, então a aplicação responde `200`.
- Dado nenhum `access_token` apresentado, quando o chamador executa `GET /oauth2/jwks`, então a aplicação continua respondendo `200`, sem exigir autenticação (comportamento já implementado pelo domínio "Gestão de Chaves RSA e Publicação JWKS", preservado por esta `SecurityFilterChain`).

**História 3 — Rejeição sem impacto na lógica de negócio:**

- Dado qualquer uma das falhas de validação da História 1 (assinatura, `iss`, `exp` ou `aud` inválidos, ou ausência de `access_token`), quando o chamador executa `GET /api/protected`, então nenhum efeito da lógica de negócio do endpoint é observável na resposta ou em qualquer estado da aplicação.
- Dado o contrato OpenAPI, quando `GET /v3/api-docs` é consultado, então as operações correspondentes a `GET /api/protected` e `GET /api/public` aparecem descritas.

## Cross-domain dependencies

- **"Gestão de Chaves RSA e Publicação JWKS"** — mantém `GET /oauth2/jwks` público na mesma `SecurityFilterChain` que este domínio configura; este domínio preserva essa liberação sem alterá-la.
- **Keycloak** (sistema externo) — publica o JWK Set consultado para validar a assinatura dos `access_tokens` recebidos, e é a fonte dos valores de `iss` e `aud` esperados pelo ambiente.

## Risks and observations

- Esta POC não implementa testes automatizados (unitários, integração ou e2e); a verificação deste fluxo depende de um `access_token` real emitido por um Keycloak configurado para o ambiente.
- Os valores concretos de `iss` e `aud` esperados são específicos do ambiente de implantação e não são fixados pelo material de negócio (regra 6 da skill do domínio); a implementação os trata como configuráveis, sem fixar valores.
- `GET /api/protected` e `GET /api/public` são endpoints de exemplo: o material de negócio fixa apenas sua classificação de acesso, não um contrato de corpo de resposta; a implementação é livre para definir um corpo mínimo que apenas evidencie a classificação (protegido/público).
- Este domínio é o dono da `SecurityFilterChain` única da aplicação; qualquer alteração futura em outro domínio que precise liberar ou proteger um novo caminho passa por este mesmo arquivo (`SecurityConfig`).
