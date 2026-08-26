# Test cases: Servidor de Recursos OAuth2

Esta POC não implementa testes automatizados (`discovery-answers.md`); este catálogo serve como
roteiro de validação manual do domínio e como referência de cobertura na fase de implementação.

## Preconditions

- Aplicação em execução (`mvn spring-boot:run`), com `spring-boot-starter-oauth2-resource-server`
  e `spring-boot-starter-actuator` presentes no `pom.xml` e a `SecurityFilterChain`
  (`SecurityConfig`) configurada com `.oauth2ResourceServer(...)`.
- `spring.security.oauth2.resourceserver.jwt.issuer-uri` (`KEYCLOAK_ISSUER_URI`) apontando para um
  Keycloak/realm acessível pela aplicação, e `app.security.resource-server.expected-audience`
  (`RESOURCE_SERVER_EXPECTED_AUDIENCE`) configurado com um valor de `aud` alcançável por um token
  emitido por esse realm.
- Um client no Keycloak desse realm capaz de obter um `access_token` (ex.: via `client_credentials`)
  com a claim `aud` igual ao valor configurado em `RESOURCE_SERVER_EXPECTED_AUDIENCE`, exceto
  quando o caso indicar o contrário.
- Cliente HTTP capaz de enviar requisições com e sem cabeçalho `Authorization` (ex.: `curl`).
- Para os casos de falha de claim: um segundo Keycloak/realm (ou um client configurado para emitir
  para uma audiência diferente) capaz de gerar um `access_token` com `iss` ou `aud` diferentes dos
  valores configurados.
- Para o caso de token expirado: um `access_token` com vida curta configurada no Keycloak (ou
  aguardar a expiração natural de um token obtido).

## História 1 — Acesso protegido

### TC-1 (mandatory) — Acesso concedido com access_token válido

1. Obtenha um `access_token` válido do Keycloak configurado, com assinatura, `iss`, `exp` e `aud`
   correspondentes aos valores esperados pelo ambiente.
2. Chame `GET /api/protected` com o cabeçalho `Authorization: Bearer <access_token>`.

**Expected:** resposta `200`.

### TC-2 (mandatory) — Assinatura inválida rejeitada

1. Obtenha um `access_token` como em TC-1 e altere um caractere da sua assinatura (terceiro
   segmento do JWT), de forma que ela não corresponda mais ao JWKS do Keycloak configurado.
2. Chame `GET /api/protected` com o cabeçalho `Authorization: Bearer <access_token alterado>`.

**Expected:** resposta `401`.

### TC-3 (mandatory) — Token expirado rejeitado

1. Obtenha um `access_token` cuja claim `exp` já esteja expirada no momento da chamada.
2. Chame `GET /api/protected` com o cabeçalho `Authorization: Bearer <access_token expirado>`.

**Expected:** resposta `401`.

### TC-4 (mandatory) — Issuer diferente do configurado rejeitado

1. Obtenha um `access_token` emitido por um issuer diferente do configurado em
   `KEYCLOAK_ISSUER_URI` (ex.: outro realm do Keycloak).
2. Chame `GET /api/protected` com o cabeçalho `Authorization: Bearer <access_token>`.

**Expected:** resposta `401`.

### TC-5 (mandatory) — Audience diferente da configurada rejeitada

1. Obtenha um `access_token` válido do Keycloak configurado, cuja claim `aud` não contenha o valor
   configurado em `RESOURCE_SERVER_EXPECTED_AUDIENCE`.
2. Chame `GET /api/protected` com o cabeçalho `Authorization: Bearer <access_token>`.

**Expected:** resposta `401`.

### TC-6 (mandatory) — Ausência de access_token rejeitada

1. Chame `GET /api/protected` sem o cabeçalho `Authorization`.

**Expected:** resposta `401`.

## História 2 — Acesso público

### TC-7 (mandatory) — GET /api/public sem access_token

1. Chame `GET /api/public` sem o cabeçalho `Authorization`.

**Expected:** resposta `200`.

### TC-8 (mandatory) — GET /actuator/health sem access_token

1. Chame `GET /actuator/health` sem o cabeçalho `Authorization`.

**Expected:** resposta `200`.

### TC-9 (recommended) — GET /oauth2/jwks permanece público

1. Chame `GET /oauth2/jwks` sem o cabeçalho `Authorization`.

**Expected:** resposta `200`, confirmando que a liberação pública já existente para este caminho
(domínio "Gestão de Chaves RSA e Publicação JWKS") continua em vigor após a configuração deste
domínio na mesma `SecurityFilterChain`.

## História 3 — Rejeição sem impacto na lógica de negócio

### TC-10 (mandatory) — Nenhum efeito observável da lógica do endpoint nas rejeições

_Cobre também a História 1._

1. Repita cada uma das chamadas de TC-2 a TC-6 (assinatura inválida, `exp` expirado, `iss`
   incorreto, `aud` incorreto e ausência de token).
2. Para cada chamada, inspecione o corpo da resposta.

**Expected:** em nenhuma das cinco chamadas o corpo da resposta contém o conteúdo que `GET
/api/protected` devolve em caso de sucesso (TC-1); nenhuma delas produz efeito observável além do
status `401`.

### TC-11 (mandatory) — Contrato OpenAPI descreve os endpoints de exemplo

1. Sem enviar nenhuma credencial, chame `GET /v3/api-docs`.
2. Localize, no contrato retornado, as operações correspondentes a `GET /api/protected` e
   `GET /api/public`.

**Expected:** resposta `200`; as duas operações aparecem descritas no contrato.
