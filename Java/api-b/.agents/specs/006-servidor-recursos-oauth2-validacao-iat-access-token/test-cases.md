# Test cases: Validação da claim iat do access_token em GET /api/protected

Esta POC não implementa testes automatizados (`discovery-answers.md`); este catálogo serve como
roteiro de validação manual desta unidade e como referência de cobertura na fase de implementação.

## Preconditions

- Aplicação em execução (`mvn spring-boot:run`), com o `IssuedAtValidator` composto no
  `DelegatingOAuth2TokenValidator` do bean `jwtDecoder` (`ResourceServerJwtDecoderConfig`), ao lado
  do validador padrão (assinatura/`iss`/`exp`) e do `AudienceValidator` existente.
- `spring.security.oauth2.resourceserver.jwt.issuer-uri` e
  `app.security.resource-server.expected-audience` configurados como já usado pela spec
  `003-servidor-recursos-oauth2`, apontando para um Keycloak/realm acessível pela aplicação.
- Um client no Keycloak desse realm capaz de obter um `access_token` válido (assinatura, `iss`,
  `exp` e `aud` corretos para o ambiente), exceto quando o caso indicar o contrário.
- Cliente HTTP capaz de enviar requisições com o cabeçalho `Authorization` (ex.: `curl`).
- Capacidade de obter ou montar um `access_token` com a claim `iat` ausente ou com um valor
  diferente do instante de emissão real (ex.: um client de testes do Keycloak que permita
  sobrescrever `iat`, ou um token remontado a partir de um `access_token` real trocando apenas essa
  claim e reassinado por um meio que a aplicação aceite como válido).

## História 1 — Validação de iat

### TC-1 (mandatory) — Acesso concedido com iat presente e não posterior ao instante atual

1. Obtenha um `access_token` válido do Keycloak configurado, com assinatura, `iss`, `exp` e `aud`
   corretos para o ambiente, e com a claim `iat` presente e igual ao instante real de emissão
   (não posterior ao instante atual).
2. Chame `GET /api/protected` com o cabeçalho `Authorization: Bearer <access_token>`.

**Expected:** resposta `200`.

### TC-2 (mandatory) — Ausência da claim iat rejeitada

1. Obtenha um `access_token` como em TC-1, mas sem a claim `iat`.
2. Chame `GET /api/protected` com o cabeçalho `Authorization: Bearer <access_token sem iat>`.

**Expected:** resposta `401`, e a requisição não alcança a lógica do endpoint (o corpo da resposta
não contém o conteúdo que `GET /api/protected` devolve em caso de sucesso).

### TC-3 (mandatory) — iat posterior ao instante atual rejeitado

1. Obtenha um `access_token` como em TC-1, mas com a claim `iat` definida para um instante no
   futuro em relação ao momento da chamada (além da tolerância de relógio de 60 segundos fixada em
   `research.md`).
2. Chame `GET /api/protected` com o cabeçalho `Authorization: Bearer <access_token com iat no
   futuro>`.

**Expected:** resposta `401`, e a requisição não alcança a lógica do endpoint (o corpo da resposta
não contém o conteúdo que `GET /api/protected` devolve em caso de sucesso).

### TC-4 (recommended) — iat dentro da tolerância de relógio ainda aceito

1. Obtenha um `access_token` como em TC-1, mas com a claim `iat` definida para um instante até 60
   segundos à frente do instante atual (dentro da tolerância de relógio registrada em
   `research.md`, a mesma já aplicada pelo `JwtTimestampValidator` padrão a `exp`/`nbf` no mesmo
   `jwtDecoder`).
2. Chame `GET /api/protected` com o cabeçalho `Authorization: Bearer <access_token>`.

**Expected:** resposta `200`.
