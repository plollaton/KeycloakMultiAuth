# Spec: Validação da claim iat do access_token em GET /api/protected

## Overview

Extensão da validação do `access_token` apresentado a `GET /api/protected` para incluir a claim `iat`, rejeitando um token sem essa claim ou com `iat` posterior ao instante atual, além das verificações de assinatura, `iss`, `exp` e `aud` já aplicadas por este domínio.

## Domain

- Slug: `servidor-recursos-oauth2`
- Skill: [`.agents/skills/servidor-recursos-oauth2/SKILL.md`](../../skills/servidor-recursos-oauth2/SKILL.md)

## Estado atual do código

`ResourceServerJwtDecoderConfig` (`src/main/java/com/aplicacaosegura/resourceserver/ResourceServerJwtDecoderConfig.java`) monta o `JwtDecoder` usado por `GET /api/protected`, combinando em um `DelegatingOAuth2TokenValidator` a validação padrão da autoconfiguração do `spring-boot-starter-oauth2-resource-server` (`JwtValidators.createDefaultWithIssuer(issuerUri)` — assinatura contra o JWKS do Keycloak, `exp` e `iss`, este último confirmado contra `spring.security.oauth2.resourceserver.jwt.issuer-uri`) com o `AudienceValidator` próprio deste domínio, que valida `aud` contra `app.security.resource-server.expected-audience`. A claim `iat` não é verificada por nenhum desses validadores: `JwtValidators.createDefaultWithIssuer` compõe apenas um validador de timestamp que verifica `exp` (e `nbf`, quando presente), sem checar `iat`.

## Scope

**In:**

- Validação da claim `iat` do `access_token` apresentado a `GET /api/protected`, rejeitando um token sem `iat` ou com `iat` posterior ao instante atual da validação.

**Out:**

- Validação de assinatura, `iss`, `exp` e `aud` do `access_token` — já implementada por este mesmo domínio (spec `003-servidor-recursos-oauth2`), sem alteração por esta unidade.
- Definição de uma idade máxima aceitável para o `access_token` com base em `iat` (uma janela além de "não estar no futuro") — não fixada pelo material de negócio nem pedida pelo requerente.
- Validação de `access_tokens` em outros endpoints — `GET /api/protected` é o único endpoint protegido desta aplicação.

## Domain boundary

**This spec implements:**

- Validador de claim `iat` do `access_token`, incorporado ao `DelegatingOAuth2TokenValidator` composto em `ResourceServerJwtDecoderConfig` (bean `jwtDecoder`), ao lado da validação padrão (assinatura/`iss`/`exp`) e do `AudienceValidator` existente.

**Belongs to other domains (cross-domain, does not become a task here):**

- Validação de assinatura, `iss`, `exp` e `aud` do `access_token` recebido em `GET /api/protected` → já implementada por este mesmo domínio na spec `003-servidor-recursos-oauth2`, preservada sem alteração.

## User stories

1. Como operador desta aplicação, quero que um `access_token` sem a claim `iat` ou com `iat` posterior ao instante atual seja rejeitado em `GET /api/protected`, para reforçar a validade temporal do token além do que já é garantido pela verificação de `exp`.

## Acceptance criteria

**História 1 — Validação de iat:**

- Dado um `access_token` com assinatura, `iss`, `exp` e `aud` válidos e a claim `iat` presente com valor não posterior ao instante atual, quando o chamador executa `GET /api/protected` apresentando esse `access_token`, então a aplicação responde `200`.
- Dado um `access_token` sem a claim `iat`, quando o chamador executa `GET /api/protected`, então a aplicação responde `401` e a requisição não alcança a lógica do endpoint.
- Dado um `access_token` com a claim `iat` posterior ao instante atual, quando o chamador executa `GET /api/protected`, então a aplicação responde `401` e a requisição não alcança a lógica do endpoint.

## Current → new behavior

- Atualmente `GET /api/protected` exige um `access_token` com assinatura válida contra o JWKS do Keycloak e valida `iss` (contra `issuer-uri`), `exp` e `aud` (contra `expected-audience`); essas quatro verificações permanecem exatamente como estão — nenhuma delas é alterada por esta unidade, inclusive a confirmação de `iss` contra o valor configurado, que já é o comportamento atual.
- Atualmente a claim `iat` do `access_token` não é verificada em nenhum ponto do fluxo de `GET /api/protected`; passa a ser verificada: um token sem `iat` ou com `iat` no futuro é rejeitado com `401`, antes de alcançar a lógica do endpoint, do mesmo modo que já ocorre hoje para falhas de assinatura, `iss`, `exp` ou `aud`.

## Cross-domain dependencies

- **Keycloak** (sistema externo) — emite o `access_token` validado por este domínio, incluindo a claim `iat`; esta unidade não altera essa emissão, apenas passa a verificar essa claim no momento da validação.

## Risks and observations

- Esta POC não implementa testes automatizados (unitários, integração ou e2e); a verificação deste fluxo depende de um `access_token` real emitido por um Keycloak configurado para o ambiente.
- A regra 7 da skill autoritativa deste domínio fixa hoje apenas assinatura, `iss`, `exp` e `aud` como verificações obrigatórias de um `access_token`; a inclusão de `iat` é uma extensão de escopo pedida pelo requerente desta unidade, não derivada do material de negócio original — o ajuste da skill para refletir essa extensão é registrado como impacto na documentação autoritativa na fase de plano desta unidade, não nesta spec.
- O material de negócio não fixa uma tolerância de relógio (clock skew) para comparar `iat` com o instante atual da validação; a escolha de um valor de tolerância é um detalhe técnico a resolver na fase de plano, não uma decisão de negócio em aberto.
