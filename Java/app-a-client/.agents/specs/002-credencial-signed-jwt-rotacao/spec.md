# Spec: Credencial do cliente por Signed JWT com rotação de chaves

## Visão geral

Trocar a autenticação do cliente OAuth2 da App A de **client-secret** para **Signed JWT
(`private_key_jwt`)**, preservando o grant `client_credentials`: a aplicação passa a gerar e
rotacionar um par de chaves (persistido para sobreviver a reinícios), assinar a asserção do
cliente com a chave privada, publicar as chaves públicas em um endereço JWKS que o Keycloak busca
a cada renovação, e expor um endpoint POST para gerar/rotacionar a chave.

## Domínios envolvidos

Mudança transversal, autônoma (não é filha de um único domínio):

- **Autenticação Máquina-a-Máquina** (`autenticacao-maquina-a-maquina`) —
  [`.agents/skills/autenticacao-maquina-a-maquina/SKILL.md`](../../skills/autenticacao-maquina-a-maquina/SKILL.md).
  Impacto: substitui o método de autenticação do cliente (client-secret → `private_key_jwt`),
  preservando o grant `client_credentials` e o escopo `app-b.invoke`; passa a gerar, rotacionar e
  persistir o par de chaves e a publicar as chaves públicas (JWKS).
- **Integração com a App B** (`integracao-app-b`) —
  [`.agents/skills/integracao-app-b/SKILL.md`](../../skills/integracao-app-b/SKILL.md).
  Impacto: novos endpoints REST de entrada (o POST de rotação e o endereço JWKS) ampliam a
  superfície REST exposta pela aplicação, que deve ser documentada via OpenAPI (regra 8 do
  domínio). O consumo de `GET /api/protegido` da App B permanece inalterado (continua recebendo o
  bearer token pelo interceptor).

## Escopo

**In:**

- Autenticação do cliente no Keycloak por Signed JWT (asserção assinada com a chave privada),
  em substituição ao segredo compartilhado, mantendo o fluxo `client_credentials`.
- Geração inicial e renovação (rotação) do par de chaves da aplicação por um endpoint POST
  dedicado, acionado manualmente.
- Publicação das chaves públicas da aplicação em um endereço (JWKS) que o Keycloak busca a cada
  renovação.
- Persistência do par de chaves para sobreviver a reinícios da aplicação.

**Out:**

- Validação da assinatura do access token (via JWKS do Keycloak) — responsabilidade da App B
  (resource server).
- Cofre de segredos ou gestão externa de credenciais — o segredo compartilhado é removido; esta
  unidade não introduz cofre.
- Proteção/autorização do endpoint de rotação — permanece aberto (coerente com a POC), como o
  `GET /demo/chamar-app-b` atual.
- Persistência de outros estados — o access token continua mantido apenas em memória.
- Rotação automática/agendada de chaves — a rotação é sempre acionada manualmente pelo endpoint
  POST.

## Fronteira de domínio

**Esta spec implementa (transversal aos dois domínios envolvidos):**

- Registration OAuth2 `keycloak-client-credentials` reconfigurado para
  `client-authentication-method: private_key_jwt`, com remoção do `client-secret` e da variável
  `APP_A_CLIENT_SECRET`.
- Componente de geração e armazenamento do par de chaves (keystore/arquivo persistido).
- Endpoint `POST /credencial/rotacionar-chave` — gera/rotaciona o par de chaves.
- Endpoint `GET /.well-known/jwks.json` — publica a(s) chave(s) pública(s) corrente(s) para
  descoberta pelo Keycloak.
- Assinatura da `client_assertion` (`private_key_jwt`) na obtenção do token junto ao endpoint de
  token do Keycloak.
- Configuração externalizada dos novos parâmetros (localização e segredo do keystore, caminho do
  JWKS, algoritmo/tipo de chave), mantendo o padrão de variáveis de ambiente com defaults de
  desenvolvimento.
- Atualização do contrato OpenAPI para incluir os novos endpoints expostos.

**Pertence a outros domínios/sistemas (dependência transversal, não vira tarefa aqui):**

- Validação da assinatura do JWT via JWKS do Keycloak → App B (resource server), domínio
  `integracao-app-b`.
- Configuração do cliente no realm do Keycloak (habilitar autenticação Signed JWT e "Use JWKS URL"
  apontando para o endereço JWKS da App A) → pré-condição no Keycloak, fora do repositório.
- Anexação do bearer token nas chamadas à App B → domínio `integracao-app-b` (inalterado).

## Histórias de usuário

1. Como operador da aplicação, quero que a App A se autentique no Keycloak por Signed JWT
   (asserção assinada com chave privada) em vez de um segredo compartilhado, para eliminar o
   segredo em configuração e reduzir o risco de exposição da credencial.
2. Como operador, quero acionar a geração inicial e a renovação do par de chaves por um endpoint
   POST, para provisionar e rotacionar a credencial sem reimplantar a aplicação.
3. Como Keycloak (sistema consumidor), quero buscar as chaves públicas correntes da App A em um
   endereço estável a cada renovação, para validar a asserção assinada sem upload manual de
   certificado.
4. Como operador, quero que o par de chaves sobreviva a reinícios da aplicação, para não invalidar
   a autenticação enquanto o Keycloak ainda tem o JWKS anterior em cache.

## Critérios de aceite

**História 1 — Autenticação por Signed JWT:**

- Dado que a App A precisa de um access token, quando ela o solicita ao Keycloak, então autentica
  o cliente via `private_key_jwt` (envia uma `client_assertion` JWT assinada com a chave privada
  corrente), sem enviar `client_secret`.
- Dado o fluxo de obtenção do token, quando o token é solicitado, então o grant permanece
  `client_credentials` e o escopo permanece `app-b.invoke` (comportamento preservado).
- Dado que o `client-secret`/`APP_A_CLIENT_SECRET` foi removido, quando a aplicação inicia, então
  não há mais configuração de segredo compartilhado para o registration
  `keycloak-client-credentials`.
- Dado um token obtido por Signed JWT, quando a App A chama `GET /api/protegido` da App B (por
  exemplo via `GET /demo/chamar-app-b`), então a chamada continua autorizada como antes (fluxo de
  integração inalterado).

**História 2 — Geração e rotação via POST:**

- Dado que a aplicação ainda não possui par de chaves, quando `POST /credencial/rotacionar-chave`
  é chamado, então um novo par de chaves é gerado e persistido, e a resposta retorna `200` com o
  identificador da chave gerada (`kid`); a chave privada nunca aparece no corpo da resposta.
- Dado que já existe um par de chaves, quando `POST /credencial/rotacionar-chave` é chamado
  novamente, então um novo par é gerado e passa a ser a chave corrente de assinatura, e o `kid`
  retornado difere do anterior.
- Dado o caráter de POC, quando `POST /credencial/rotacionar-chave` é chamado sem qualquer
  autenticação, então a chamada é aceita (endpoint aberto, como `GET /demo/chamar-app-b`).

**História 3 — Publicação do JWKS:**

- Dado que existe um par de chaves corrente, quando o Keycloak (ou qualquer cliente) faz
  `GET /.well-known/jwks.json`, então a resposta é `200` com um documento JWKS contendo a(s)
  chave(s) pública(s) corrente(s), cada uma com seu `kid`, e sem qualquer material privado.
- Dado que uma rotação acabou de ocorrer, quando o endereço JWKS é buscado, então ele reflete a
  chave pública corrente e pode manter temporariamente a chave pública anterior para tolerar o
  cache do Keycloak (estratégia de sobreposição detalhada no `plan.md`/`research.md`).

**História 4 — Persistência do par de chaves:**

- Dado um par de chaves gerado e persistido, quando a aplicação é reiniciada, então ela recarrega
  o mesmo par do armazenamento persistente e continua assinando com a mesma chave corrente (o
  `kid` publicado no JWKS permanece o mesmo após o reinício).
- Dado que o access token continua mantido apenas em memória, quando a aplicação reinicia, então
  um novo token é obtido no primeiro uso (comportamento preservado), assinado com a chave
  persistida.

## Comportamento atual → novo

| Aspecto | Atual | Novo |
| --- | --- | --- |
| Autenticação do cliente | `client-secret` compartilhado (`APP_A_CLIENT_SECRET`, default `changeit`) enviado ao endpoint de token | `private_key_jwt` — asserção assinada com a chave privada, sem segredo |
| Material de credencial | Segredo em configuração externalizada (débito técnico conhecido) | Par de chaves gerado pela aplicação; chave privada persistida em keystore/arquivo; chave pública publicada via JWKS |
| Descoberta pelo Keycloak | Keycloak valida o segredo compartilhado | Keycloak busca as chaves públicas no endereço JWKS da App A a cada renovação ("Use JWKS URL") |
| Superfície REST de entrada | Apenas `GET /demo/chamar-app-b` | Acrescenta `POST /credencial/rotacionar-chave` e `GET /.well-known/jwks.json` |
| Persistência | Nada persistido (token em memória) | Par de chaves persistido; token continua em memória |

**Permanece igual:** grant `client_credentials`; escopo `app-b.invoke`; descoberta OIDC via
`issuer-uri` (`/.well-known/openid-configuration`); access token mantido em memória e renovado sob
demanda; consumo de `GET /api/protegido` da App B com bearer anexado pelo interceptor; endpoint
`GET /demo/chamar-app-b`; ausência de proteção nos endpoints de entrada (o POST de rotação também
é aberto); ausência de testes automatizados (POC).

## Dependências entre domínios

- **App B** (`integracao-app-b` / resource server) — valida a assinatura do access token via JWKS
  do Keycloak; inalterada por esta unidade e não vira tarefa aqui.
- **Keycloak** — deve ter o cliente configurado para autenticação Signed JWT com "Use JWKS URL"
  apontando para o endereço JWKS da App A; pré-condição no realm, fora do repositório (ver
  `references/technical-dependencies.md` do domínio de autenticação).
- **Skill técnica `documentacao-api-openapi`** — o contrato OpenAPI deve ser atualizado para
  incluir os novos endpoints expostos (padrão de documentação validado na descoberta).

## Riscos e observações

- **Drift deliberado ante a documentação autoritativa (método de autenticação).** Esta unidade
  contraria as regras 2 e 8 e o "Débito técnico conhecido" da skill
  `autenticacao-maquina-a-maquina` (identidade de cliente = identificador + segredo; segredo em
  configuração) e a decisão `auth-adjustments` do `discovery-answers.md` (manter o client-secret
  por ora). Por decisão humana registrada nesta rodada (resposta à lacuna
  `substituicao-client-secret`: substituir integralmente), a mudança é deliberada. O impacto na
  documentação autoritativa será registrado no `plan.md` ("Impacto na documentação autoritativa")
  e a atualização da skill vira tarefa na fase de `tasks`; a doc **não** é editada nesta fase.
- **Drift deliberado ante a decisão "sem persistência".** Passa a haver persistência do par de
  chaves (keystore/arquivo), o que diverge da convenção "sem persistência; não introduzir
  persistência sem decisão humana". Decisão humana registrada (lacuna `persistencia-chave-privada`:
  persistir em keystore). Também registrado como drift no `plan.md`.
- **Janela de inconsistência na rotação.** Ao rotacionar, o Keycloak pode ainda ter o JWKS
  anterior em cache; recomenda-se manter temporariamente a chave pública anterior no JWKS e/ou
  coordenar o momento da troca da chave de assinatura. A estratégia exata é decisão do
  `plan.md`/`research.md`.
- **Endpoint de rotação aberto (POC).** Qualquer um pode acionar a rotação; há risco de negação de
  serviço de autenticação (forçar rotações e provocar janelas de falha). Aceito por decisão humana
  (lacuna `protecao-endpoint-rotacao`: aberto, coerente com a POC); reavaliar proteção fora da POC.
- **Proteção do material privado.** A chave privada persistida (keystore) deve ser protegida por
  permissões/segredo do keystore; o material real nunca deve ser versionado, seguindo o mesmo
  princípio dos segredos, e os defaults servem apenas a desenvolvimento.
- **Decisões técnicas em aberto para o `plan.md`/`research.md`:** algoritmo e tipo de chave
  (RSA/EC), tamanho, formato do keystore e do documento JWKS, e a forma de assinar a
  `client_assertion` com o suporte do Spring Security OAuth2 Client.
