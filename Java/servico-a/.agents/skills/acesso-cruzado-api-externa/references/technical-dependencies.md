---
name: acesso-cruzado-api-externa-technical-dependencies
description: Dependências técnicas do domínio Acesso Cruzado a Aplicação Externa — o que cada uma é e o que fica comprometido na sua ausência.
metadata:
  author: clovis-cli
  type: domain-skill-reference
---

# Dependências técnicas — Acesso Cruzado a Aplicação Externa

> **Mantendo este documento**
>
> Atualizar sempre que uma dependência técnica deste domínio for adicionada,
> removida ou substituída. Um refactor que preserva a mesma dependência (troca
> de versão sem mudança de contrato, por exemplo) não exige atualização.

- **Cliente OAuth2 com Client Assertion JWT** — fornece o
  `OAuth2AuthorizedClientManager` já configurado, usado para obter o
  `access_token` apresentado na chamada de saída a `GET /api/protected` da
  aplicação externa; sem ele este domínio não tem como se autenticar perante
  essa aplicação.
- **Servidor de Recursos OAuth2** — mantém `GET /api/cross` público na
  `SecurityFilterChain` que possui; sem essa liberação, o endpoint deste domínio
  ficaria inacessível sem um `access_token` do próprio chamador, quebrando a
  classificação pública fixada pela regra 1 da skill.
- **`spring-boot-starter-web`** (`RestClient`) — fornece o cliente HTTP usado na
  chamada de saída a `GET /api/protected` da aplicação externa configurada em
  `app.cross.external-base-url`; sem ele não há como montar essa chamada na
  convenção de stack fixada para o projeto.
- **OpenAPI/Swagger** — documenta o contrato de `GET /api/cross`; sem ela o
  endpoint fica sem descrição formal para quem for integrá-lo.
