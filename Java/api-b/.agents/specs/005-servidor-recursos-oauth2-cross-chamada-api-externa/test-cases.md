# Test cases: Endpoint Público de Acesso Cruzado a Aplicação Externa (GET /api/cross)

Esta POC não implementa testes automatizados (`discovery-answers.md`); este catálogo serve como
roteiro de validação manual desta unidade e como referência de cobertura na fase de implementação.

## Preconditions

- Aplicação em execução (`mvn spring-boot:run`), com `GET /api/cross` liberado como público na
  `SecurityFilterChain` (`SecurityConfig`).
- `app.cross.external-base-url` informado na linha de comando de inicialização
  (`--app.cross.external-base-url=...` ou variável de ambiente `APP_CROSS_EXTERNAL_BASE_URL`),
  apontando para uma aplicação externa real e acessível, que expõe `GET /api/protected` como
  resource server (mesma regra de validação da skill `servidor-recursos-oauth2`), exceto quando o
  caso indicar o contrário.
- Troca `client_credentials`/`client_assertion` junto ao Keycloak já configurada e funcional
  (domínio "Cliente OAuth2 com Client Assertion JWT", `OAuth2AuthorizedClientManager` existente),
  com `client_id`/`aud` reconhecidos pela aplicação externa configurada, exceto quando o caso
  indicar o contrário.
- Cliente HTTP capaz de enviar requisições sem o cabeçalho `Authorization` (ex.: `curl`).
- Para os casos de falha: uma forma de configurar a aplicação externa para rejeitar o
  `access_token` recebido (ex.: audiência não reconhecida), de torná-la temporariamente
  inacessível (endereço incorreto ou serviço parado), ou de tornar a troca com o Keycloak
  indisponível.

## História 1 — Acesso público ao endpoint de acesso cruzado

### TC-1 (mandatory) — GET /api/cross processado sem access_token do chamador

1. Sem enviar o cabeçalho `Authorization`, chame `GET /api/cross`.

**Expected:** a requisição é processada sem exigir autenticação do chamador (não retorna `401`
por ausência de credenciais).

## História 2 — Endereço configurável por variável de linha de comando

### TC-2 (mandatory) — Chamada de saída direcionada ao endereço configurado

Precondição adicional: aplicação externa observável, capaz de registrar as requisições que
recebe (ex.: log de acesso).

1. Configure `app.cross.external-base-url` apontando para a aplicação externa observável.
2. Suba a aplicação com essa configuração.
3. Chame `GET /api/cross`.
4. Verifique o registro de acesso da aplicação externa.

**Expected:** a aplicação externa recebe uma requisição `GET /api/protected` originada desta
aplicação, no endereço configurado.

### TC-3 (mandatory) — Aplicação não inicia sem o endereço configurado

1. Suba a aplicação sem informar `app.cross.external-base-url`, nem por argumento de linha de
   comando nem por variável de ambiente.

**Expected:** a inicialização falha — o contexto Spring não sobe, por não conseguir resolver o
placeholder da propriedade `app.cross.external-base-url`.

## História 3 — Resultado do acesso cruzado

### TC-4 (mandatory) — Sucesso do acesso cruzado

_Cobre também a História 1._

1. Com `app.cross.external-base-url` apontando para a aplicação externa configurada e alcançável,
   sem enviar o cabeçalho `Authorization`, chame `GET /api/cross`.

**Expected:** a chamada de saída a `GET /api/protected` da aplicação externa é aceita (o
`access_token` obtido é reconhecido) e `GET /api/cross` responde `200`.

### TC-5 (mandatory) — Falha por access_token rejeitado pela aplicação externa

1. Configure a aplicação externa para rejeitar o `access_token` emitido para esta aplicação (ex.:
   audiência não reconhecida).
2. Chame `GET /api/cross`.

**Expected:** resposta `502`; o corpo da resposta não contém o `access_token` nem material de
chave privada.

### TC-6 (mandatory) — Falha por aplicação externa inacessível

1. Configure `app.cross.external-base-url` apontando para um endereço inacessível (serviço parado
   ou endereço incorreto).
2. Chame `GET /api/cross`.

**Expected:** resposta `502`; o corpo da resposta não contém o `access_token` nem material de
chave privada.

### TC-7 (recommended) — Falha na obtenção do access_token

1. Torne a troca `client_credentials`/`client_assertion` junto ao Keycloak indisponível para esta
   aplicação (ex.: Keycloak inacessível, ou client desabilitado no realm).
2. Chame `GET /api/cross`.

**Expected:** resposta `502`; o corpo da resposta não contém o `access_token` nem material de
chave privada.

### TC-8 (recommended) — Contrato OpenAPI descreve GET /api/cross

1. Sem enviar nenhuma credencial, chame `GET /v3/api-docs`.
2. Localize, no contrato retornado, a operação correspondente a `GET /api/cross`.

**Expected:** resposta `200`; a operação aparece descrita no contrato.
