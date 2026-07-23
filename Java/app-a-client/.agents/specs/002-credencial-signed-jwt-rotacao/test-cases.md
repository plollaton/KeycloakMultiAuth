# Casos de teste: Credencial do cliente por Signed JWT com rotação de chaves

Catálogo derivado estritamente de [`spec.md`](./spec.md) e [`plan.md`](./plan.md), agrupado por
história de usuário. Serve como runbook de validação manual ponta a ponta no checkpoint de
implementação (o projeto é POC, sem testes automatizados — decisão `testing-strategy`).

**Domínios envolvidos:** Autenticação Máquina-a-Máquina (`autenticacao-maquina-a-maquina`) — método
de autenticação por Signed JWT, geração/rotação/persistência do par de chaves e publicação do JWKS;
Integração com a App B (`integracao-app-b`) — novos endpoints de entrada e o fluxo ponta a ponta
`GET /demo/chamar-app-b` (inalterado no consumo).

## Precondições

- **App A** no ar (`mvn spring-boot:run`, porta `8081`), com as variáveis do keystore configuradas
  (caminho e senha) conforme `plan.md`.
- **App B** no ar (porta `8082`) validando o JWT via JWKS do Keycloak, com `APP_B_BASE_URL`
  apontando para ela.
- **Keycloak** no ar, com o cliente `APP_A_CLIENT_ID` no realm de `KEYCLOAK_ISSUER_URI` configurado
  para **autenticação Signed JWT** com **"Use JWKS URL"** apontando para o endereço JWKS da App A
  (`GET /.well-known/jwks.json`), e o client scope `app-b.invoke` associado. O Keycloak precisa
  alcançar o endereço JWKS da App A pela rede.
- Um cliente HTTP para acionar os endpoints de entrada da App A.
- Salvo indicação em contrário, considere que a App A já possui um par de chaves corrente
  gerado/persistido (execute o TC-4 uma vez, se necessário, para criar o par inicial).

---

## História 1 — Autenticação por Signed JWT

### TC-1 (obrigatório) — Fluxo ponta a ponta autenticando por Signed JWT

1. Com a App A, a App B e o Keycloak no ar conforme as precondições, chame
   `GET /demo/chamar-app-b` na App A.

**Expected:** `200` com o corpo repassado verbatim pela App B. O sucesso comprova que a App A obteve
o access token junto ao Keycloak autenticando o cliente por `private_key_jwt` (asserção assinada com
a chave privada corrente, sem `client_secret`) e chamou `GET /api/protegido` da App B autorizada.

### TC-2 (obrigatório) — Ausência de segredo compartilhado na configuração

1. Inspecione a configuração do registration `keycloak-client-credentials` (em
   `src/main/resources/application.yml`).

**Expected:** não há `client-secret` nem a variável `APP_A_CLIENT_SECRET`; o registration declara
`authorization-grant-type: client_credentials` e `client-authentication-method: private_key_jwt`,
conforme `plan.md`.

### TC-3 (recomendado) — Grant e escopo preservados

1. Após executar o TC-1, consulte o evento de login/emissão de token do cliente no console de
   administração do Keycloak (ou o token capturado no fluxo).

**Expected:** a emissão foi pelo grant `client_credentials` e o token carrega o escopo
`app-b.invoke` — ambos preservados em relação ao comportamento anterior.

---

## História 2 — Geração e rotação via POST

### TC-4 (obrigatório) — Geração inicial do par de chaves sem par prévio

1. Partindo de uma App A sem keystore/par de chaves existente, chame
   `POST /credencial/rotacionar-chave` (sem corpo de requisição).

**Expected:** `200` com `{ "kid": "...", "criadaEm": "<timestamp>" }`; o corpo **não** contém
qualquer material privado. Como efeito colateral, o par é gerado e persistido no keystore, e a chave
passa a ser exposta em `GET /.well-known/jwks.json` (verificável no TC-8).

### TC-5 (obrigatório) — Rotação gera nova chave corrente com `kid` distinto

1. Com um par corrente existente de identificador `K1` (obtido em `GET /.well-known/jwks.json`),
   chame `POST /credencial/rotacionar-chave`.

**Expected:** `200` com `{ "kid": "K2", "criadaEm": "<timestamp>" }`, onde `K2` ≠ `K1`; `K2` passa a
ser a chave corrente de assinatura.

### TC-6 (obrigatório) — Endpoint de rotação aberto (POC)

1. Chame `POST /credencial/rotacionar-chave` **sem** qualquer cabeçalho de autenticação/autorização.

**Expected:** a chamada é aceita (`200`), não retornando `401`/`403` — endpoint aberto, coerente com
a POC e com o `GET /demo/chamar-app-b`.

---

## História 3 — Publicação do JWKS

### TC-7 (obrigatório) — JWKS publica a chave pública corrente sem material privado

1. Com um par corrente de identificador `K`, chame `GET /.well-known/jwks.json`.

**Expected:** `200` com um documento JWKS (`{ "keys": [ ... ] }`) contendo uma entrada com
`kty=RSA`, `use=sig`, `alg=RS256` e `kid=K`; **nenhum** campo de material privado (ex.: `d`) está
presente.

### TC-8 (obrigatório) — Sobreposição corrente + anterior após rotação

1. Observe o `kid` corrente `K1` em `GET /.well-known/jwks.json`.
2. Chame `POST /credencial/rotacionar-chave` (nova chave `K2`).
3. Chame novamente `GET /.well-known/jwks.json`.

**Expected:** o JWKS contém tanto `K2` (corrente) quanto `K1` (anterior); a assinatura passa a usar
`K2`, mantendo `K1` publicada para tolerar o cache do JWKS no Keycloak.

### TC-9 (recomendado) — Tolerância à janela de cache do Keycloak após rotação

1. Com o fluxo do TC-1 funcionando, chame `POST /credencial/rotacionar-chave` para rotacionar a
   chave.
2. Chame `GET /demo/chamar-app-b`.

**Expected:** `200` com o corpo da App B — a chamada continua autorizada, seja porque o Keycloak
re-buscou o JWKS e validou pela chave corrente, seja porque ainda validou pela chave anterior em
cache (ambas publicadas).

---

## História 4 — Persistência do par de chaves

### TC-10 (obrigatório) — Par sobrevive ao reinício (kid estável)

1. Observe o `kid` corrente `K` em `GET /.well-known/jwks.json`.
2. Reinicie a App A.
3. Chame novamente `GET /.well-known/jwks.json`.

**Expected:** o `kid` corrente continua sendo `K` (par recarregado do keystore persistido); não há
geração de um novo par no boot.

### TC-11 (obrigatório) — Token reobtido após reinício usando a chave persistida

1. Após o reinício do TC-10, chame `GET /demo/chamar-app-b`.

**Expected:** `200` com o corpo da App B — um novo access token é obtido no primeiro uso (o token não
é persistido, segue apenas em memória), assinado com a chave persistida `K`, e a App B autoriza a
chamada.

---

## Cobertura complementar (contrato exposto)

### TC-12 (recomendado) — Contrato OpenAPI inclui os novos endpoints

1. Com a App A no ar, acesse o contrato gerado em `/v3/api-docs` (ou o Swagger UI em
   `/swagger-ui.html`).

**Expected:** o contrato lista `POST /credencial/rotacionar-chave` e `GET /.well-known/jwks.json`,
além do `GET /demo/chamar-app-b` existente, conforme a estratégia de documentação do `plan.md`.
