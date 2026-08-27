---
name: servidor-recursos-oauth2-technical-dependencies
description: Dependências técnicas do domínio Servidor de Recursos OAuth2 — o que cada uma é e o que fica comprometido na sua ausência.
metadata:
  author: clovis-cli
  type: domain-skill-reference
---

# Dependências técnicas — Servidor de Recursos OAuth2

> **Mantendo este documento**
>
> Atualizar sempre que uma dependência técnica deste domínio for adicionada,
> removida ou substituída. Um refactor que preserva a mesma dependência (troca
> de versão sem mudança de contrato, por exemplo) não exige atualização.

- **Keycloak** — publica o JWKS usado para validar assinatura e claims dos
  `access_tokens` recebidos; sem ele não há chave pública para validar nada.
- **`spring-boot-starter-oauth2-resource-server`** — starter que fornece o
  suporte de framework para validação de JWT como resource server (busca do
  JWKS do emissor, verificação de assinatura e das claims `iss`/`exp` na
  `SecurityFilterChain`); as claims `aud` e `iat` não são cobertas pela
  autoconfiguração do starter e são validadas por validadores próprios deste
  domínio (`AudienceValidator` e `IssuedAtValidator`), compostos ao validador
  padrão via `DelegatingOAuth2TokenValidator`. Sem o starter, toda essa
  validação — inclusive a padrão — precisaria ser implementada manualmente,
  fora da convenção de stack fixada para o projeto.
- **Spring Boot Actuator** — expõe `/actuator/health` como endpoint público
  de exemplo; sem ele esse endpoint de exemplo não existe.
- **Gestão de Chaves RSA e Publicação JWKS** — define que `/oauth2/jwks` deve
  permanecer público dentro da mesma cadeia de segurança deste domínio.
- **OpenAPI/Swagger** — documenta o contrato de `/api/public` e
  `/api/protected`; sem ela esses endpoints ficam sem descrição formal para
  quem for integrá-los. Além da documentação em si, essa convenção exige que
  os caminhos do Swagger UI e do contrato OpenAPI (`/v3/api-docs/**` e
  `/swagger-ui/**`) também sejam liberados como públicos na mesma
  `SecurityFilterChain` deste domínio; sem essa liberação, a documentação
  fica inacessível mesmo para os endpoints que não exigem autenticação.
