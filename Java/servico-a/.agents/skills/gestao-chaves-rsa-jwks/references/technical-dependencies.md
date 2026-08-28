---
name: gestao-chaves-rsa-jwks-technical-dependencies
description: Dependências técnicas do domínio Gestão de Chaves RSA e Publicação JWKS — o que cada uma é e o que fica comprometido na sua ausência.
metadata:
  author: clovis-cli
  type: domain-skill-reference
---

# Dependências técnicas — Gestão de Chaves RSA e Publicação JWKS

> **Mantendo este documento**
>
> Atualizar sempre que uma dependência técnica deste domínio for adicionada,
> removida ou substituída. Um refactor que preserva a mesma dependência (troca
> de versão sem mudança de contrato, por exemplo) não exige atualização.

- **Nimbus JOSE+JWT** — biblioteca que constrói o par de chaves RSA e o `JWKSet`
  e serializa a chave pública no formato esperado pelo Keycloak; sem ela não há
  como montar o endpoint no contrato exigido.
- **Endpoint HTTP público, fora de qualquer regra de autenticação da
  `SecurityFilterChain`** — sem essa liberação, o Keycloak não consegue obter a
  chave pública para validar o client_assertion, quebrando todo o fluxo RFC
  7523.
- **OpenAPI/Swagger** — documenta o contrato do endpoint `/oauth2/jwks`; sem ela
  o endpoint fica sem descrição formal para quem for integrá-lo.
