# Test cases: Cliente OAuth2 com Client Assertion JWT

Esta POC não implementa testes automatizados (`discovery-answers.md`); este catálogo serve como
roteiro de validação manual do domínio e como referência de cobertura na fase de implementação.

## Preconditions

- Aplicação em execução (`mvn spring-boot:run`), com o par de chaves RSA do domínio "Gestão de
  Chaves RSA e Publicação JWKS" carregado (`api-a-private.pem`/`api-a-cert.pem` em
  `src/main/resources`) e `GET /oauth2/jwks` acessível.
- Um Keycloak acessível pela aplicação, com um client configurado para autenticação via JWT
  assinado (`private_key_jwt`), apontando para `GET /oauth2/jwks` desta aplicação como fonte de
  validação, e com as propriedades `spring.security.oauth2.client.registration.keycloak`
  (`client-id`) e `spring.security.oauth2.client.provider.keycloak` (`token-uri`/`issuer-uri`)
  apontando para esse client, exceto quando o caso indicar o contrário.
- Cliente HTTP capaz de fazer requisições sem enviar nenhuma credencial (ex.: `curl`).
- Para os casos que inspecionam o `client_assertion` enviado ao Keycloak: um proxy HTTP de
  inspeção (ex.: mitmproxy) posicionado entre a aplicação e o Keycloak, ou acesso aos logs de
  requisição do Keycloak, capaz de capturar o corpo do `POST /oauth2/token` recebido.
- Para o caso de falha controlada: uma configuração alternativa do client no Keycloak (ou o mesmo
  client temporariamente reconfigurado) sem a fonte de validação apontando para
  `GET /oauth2/jwks` desta aplicação, de forma que a assinatura do `client_assertion` seja
  rejeitada.

## História 4 — Endpoint de diagnóstico (aciona as Histórias 1, 2 e 3)

### TC-1 (mandatory) — Troca bem-sucedida confirmada pelo endpoint de diagnóstico

1. Com o client desta aplicação configurado corretamente no Keycloak, e sem enviar nenhuma
   credencial, chame `GET /diagnostics/oauth2-client-assertion`.

**Expected:** resposta `200` com corpo `{"status": "ok"}`; o corpo da resposta não contém o
`access_token` nem qualquer material da chave privada.

### TC-2 (mandatory) — Falha na troca refletida pelo endpoint de diagnóstico

1. Reconfigure o client desta aplicação no Keycloak para uma fonte de validação diferente de
   `GET /oauth2/jwks` desta aplicação, de forma que a assinatura do `client_assertion` seja
   rejeitada.
2. Sem enviar nenhuma credencial, chame `GET /diagnostics/oauth2-client-assertion`.

**Expected:** resposta `502`, sem o corpo `{"status": "ok"}` usado em TC-1 e sem expor detalhes da
chave privada nem o corpo de erro do Keycloak.

### TC-3 (recommended) — Claims do client_assertion enviado ao Keycloak

_Cobre também a História 1._

1. Com o proxy de inspeção capturando o tráfego entre a aplicação e o Keycloak (ou com acesso aos
   logs de requisição do Keycloak), chame `GET /diagnostics/oauth2-client-assertion`.
2. Localize o parâmetro `client_assertion` no corpo do `POST /oauth2/token` capturado e decodifique
   o JWT (cabeçalho e claims).

**Expected:** o JWT está assinado em RS256; o cabeçalho traz o `kid` do par de chaves ativo
(o mesmo publicado em `GET /oauth2/jwks` desta aplicação); as claims `iss` e `sub` contêm o mesmo
valor — o `client_id` configurado desta aplicação —; `aud` contém o identificador do endpoint de
token do Keycloak configurado; `iat` e `exp` estão presentes com `exp` posterior a `iat`; e `jti`
está presente.

### TC-4 (recommended) — jti único entre chamadas distintas

_Cobre também a História 1._

1. Repita a captura de TC-3 duas vezes, chamando `GET /diagnostics/oauth2-client-assertion` em
   cada uma.
2. Compare o `jti` dos dois `client_assertion` capturados.

**Expected:** os dois valores de `jti` são diferentes.

### TC-5 (mandatory) — Parâmetros da troca enviados ao Keycloak

_Cobre também a História 2._

1. Com o proxy de inspeção capturando o tráfego (ou com acesso aos logs do Keycloak), chame
   `GET /diagnostics/oauth2-client-assertion`.
2. Inspecione o corpo do `POST /oauth2/token` capturado e a resposta que o Keycloak devolveu para
   essa requisição.

**Expected:** o corpo capturado contém `grant_type=client_credentials`,
`client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer` e
`client_assertion` com o JWT assinado; a resposta do Keycloak para essa requisição contém um
`access_token`.

### TC-6 (recommended) — Disponibilidade do access_token em chamadas sucessivas

_Cobre também a História 3._

1. Com o proxy de inspeção capturando o tráfego entre a aplicação e o Keycloak (ou com acesso aos
   logs do Keycloak), chame `GET /diagnostics/oauth2-client-assertion`.
2. Imediatamente em seguida, antes da expiração do `access_token` obtido no passo 1, chame
   `GET /diagnostics/oauth2-client-assertion` novamente.
3. Conte quantas requisições `POST /oauth2/token` o Keycloak recebeu ao longo dos passos 1 e 2.

**Expected:** ambas as chamadas ao endpoint de diagnóstico respondem `200` com
`{"status": "ok"}`; apenas uma requisição `POST /oauth2/token` é observada no Keycloak, já que a
segunda chamada reaproveita o `access_token` ainda válido mantido pelo
`OAuth2AuthorizedClientManager`.

### TC-7 (mandatory) — Contrato OpenAPI descreve o endpoint de diagnóstico

1. Sem enviar nenhuma credencial, chame `GET /v3/api-docs`.
2. Localize, no contrato retornado, a operação correspondente a
   `GET /diagnostics/oauth2-client-assertion`.

**Expected:** resposta `200`; a operação aparece descrita no contrato.
