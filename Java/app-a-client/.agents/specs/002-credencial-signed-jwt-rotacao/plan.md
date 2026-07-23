# Plano: Credencial do cliente por Signed JWT com rotação de chaves

Visão técnica (**como**) da spec transversal
[`spec.md`](./spec.md). Regras de negócio e critérios de aceite vivem na spec e na
documentação autoritativa dos domínios; aqui ficam as decisões técnicas.

## Domínios envolvidos

- **Autenticação Máquina-a-Máquina** (`autenticacao-maquina-a-maquina`) — troca do método de
  autenticação do cliente (client-secret → `private_key_jwt`), geração/rotação/persistência do par
  de chaves e publicação do JWKS.
- **Integração com a App B** (`integracao-app-b`) — novos endpoints REST de entrada ampliam a
  superfície exposta a documentar via OpenAPI; o consumo de `GET /api/protegido` permanece
  inalterado.

## Estado atual do código

- `config/OAuth2RestClientConfig.java` — declara `OAuth2AuthorizedClientService`
  (`InMemoryOAuth2AuthorizedClientService`), o `OAuth2AuthorizedClientManager`
  (`AuthorizedClientServiceOAuth2AuthorizedClientManager` com provider `clientCredentials()`) e o
  bean `appBRestClient` com o `OAuth2ClientHttpRequestInterceptor` fixando o registration
  `keycloak-client-credentials`.
- `client/AppBClient.java` e `web/DemoController.java` — consumo de `GET /api/protegido` e o
  gatilho `GET /demo/chamar-app-b`. **Não mudam** nesta unidade.
- `resources/application.yml` — registration `keycloak-client-credentials` com
  `client-secret: ${APP_A_CLIENT_SECRET:changeit}` e `authorization-grant-type: client_credentials`,
  provider `keycloak` com `issuer-uri`.
- `pom.xml` — `spring-boot-starter-web`, `-oauth2-client`, `-actuator`, `-test`. **Não há**
  `springdoc-openapi` declarado; **não há** componente de chaves nem endpoints de credencial.

O plano parte desse estado: estende o `OAuth2RestClientConfig`, adiciona um componente de gestão de
chaves e dois controllers, ajusta `application.yml`/`pom.xml`. O grant `client_credentials`, o
escopo `app-b.invoke`, a descoberta OIDC e o token em memória são preservados.

## Stack e estrutura

Java 21, Spring Boot 4.0.6, Maven, pacote base `com.dbfinanceira.appa` (convenções em `AGENTS.md`).
Novos elementos, seguindo a organização atual por pacote técnico:

- `com.dbfinanceira.appa.credential` (novo pacote) — gestão do par de chaves: geração, persistência
  em keystore, carregamento no boot, rotação, e exposição do JWK de assinatura corrente e do
  `JWKSet` público.
- `com.dbfinanceira.appa.config.OAuth2RestClientConfig` (alterado) — passa a configurar a
  autenticação `private_key_jwt` no cliente de token do fluxo `client_credentials`.
- `com.dbfinanceira.appa.web.JwksController` (novo) — `GET /.well-known/jwks.json`.
- `com.dbfinanceira.appa.web.CredencialController` (novo) — `POST /credencial/rotacionar-chave`.
- `resources/application.yml` (alterado) — remove `client-secret`, adiciona
  `client-authentication-method: private_key_jwt` e a configuração do keystore/chave.
- `pom.xml` (alterado) — adiciona `springdoc-openapi-starter-webmvc-ui`.

## Decisões técnicas

- **Autenticação `private_key_jwt` via Spring Security.** O provider `clientCredentials()` recebe um
  token response client baseado em `RestClient` (linha do Spring Security empacotada pelo Spring
  Boot 4.0.6) com um `NimbusJwtClientAuthenticationParametersConverter` que resolve, por
  registration, o JWK de assinatura corrente e monta a `client_assertion`. O registration passa a
  `client-authentication-method: private_key_jwt` e **deixa de ter** `client-secret`. Base firme:
  framework já em uso (`spring-boot-starter-oauth2-client`); Nimbus JOSE (`spring-security-oauth2-jose`)
  já vem transitivo. Detalhes e nome exato da classe do token client em [`research.md`](./research.md).
- **Material de credencial: par RSA-2048, assinatura RS256, X.509 autoassinado em keystore PKCS12.**
  A aplicação gera o par, embrulha a chave pública num certificado autoassinado e persiste a entrada
  no keystore; o JWK público (com `kid`) é derivado dessa entrada. Base firme: padrão da JVM,
  algoritmo default do Keycloak para Signed JWT, sem nova dependência. Tradeoffs em `research.md`.
- **Persistência do keystore em arquivo/volume.** Decisão humana registrada (lacuna
  `persistencia-chave-privada`: persistir em keystore). O keystore é lido no boot; se ausente, um par
  inicial é gerado e persistido. O access token continua **apenas em memória** (preservado).
- **Rotação e janela de cache do JWKS.** O keystore mantém a chave **corrente** (usada para assinar)
  e a **anterior**; o `JWKSet` publica ambas as chaves públicas. A assinatura usa sempre a corrente.
  Assim, enquanto o Keycloak ainda tiver o JWKS anterior em cache, a validação continua possível até
  ele re-buscar o endereço. Estratégia detalhada em `research.md`.
- **Endpoint de rotação aberto (POC).** Decisão humana registrada (lacuna
  `protecao-endpoint-rotacao`: aberto, como o `/demo`). Sem `spring-security-web`/filtros de
  autorização nesta unidade.
- **Documentação OpenAPI via springdoc.** Adota-se `springdoc-openapi-starter-webmvc-ui` (decisão de
  descoberta `doc-forms` + skill técnica `documentacao-api-openapi`), anotando os novos endpoints e,
  ao menos com `@Operation`, o `GET /demo/chamar-app-b` existente. A versão deve casar com a linha do
  Spring Boot 4 pela matriz de compatibilidade do springdoc no momento da adoção (ver `research.md`).

## Modelo de dados

Não há banco de dados (preservado). As estruturas relevantes são pequenas e ficam aqui, sem
`data-model.md` dedicado:

- **Entrada de chave (keystore PKCS12).** Alias/`kid`, chave privada RSA, certificado X.509
  autoassinado (chave pública), papel (`corrente` | `anterior`). Persistida em arquivo; protegida por
  senha do keystore.
- **Documento JWKS (`GET /.well-known/jwks.json`).** `{ "keys": [ { "kty":"RSA", "use":"sig",
  "alg":"RS256", "kid":"...", "n":"...", "e":"..." }, ... ] }` — chave corrente e anterior; **sem**
  material privado.
- **Resposta da rotação (`POST /credencial/rotacionar-chave`).** `200` com `{ "kid": "...",
  "criadaEm": "<timestamp>" }` (identificador da nova chave corrente); a chave privada **nunca**
  aparece no corpo.

## Contratos externos

Descritos aqui; sem pasta `contracts/` (ver "Artefatos opcionais"), pois o contrato REST do projeto é
**gerado do código** por springdoc (a skill `documentacao-api-openapi` proíbe manter um
`.openapi.yaml` à mão em paralelo).

- **Entrada — `GET /.well-known/jwks.json`.** Sem parâmetros. `200` → documento JWKS (acima).
  Consumido pelo Keycloak ("Use JWKS URL") a cada renovação.
- **Entrada — `POST /credencial/rotacionar-chave`.** Sem corpo de requisição. `200` → `{ kid,
  criadaEm }`. Aberto (POC).
- **Saída — obtenção do token (Keycloak).** Fluxo `client_credentials` com `client-authentication-method:
  private_key_jwt`: a requisição ao endpoint de token (resolvido por descoberta) envia
  `client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer` e a
  `client_assertion` (JWT assinado RS256 com a chave corrente, `kid` no header), sem `client_secret`.
  Escopo `app-b.invoke` preservado.
- **Saída — App B (`GET /api/protegido`).** Inalterado; bearer anexado pelo interceptor.

## Interface

Não se aplica — mudança puramente de backend. Sem `ui/`. O Swagger UI (springdoc) é ferramenta de
documentação, não interface de negócio; avaliar restringir/desabilitar fora de desenvolvimento por
`springdoc.swagger-ui.enabled`, conforme a skill técnica.

## Estratégia de testes

Sem testes automatizados — decisão validada (`testing-strategy`, projeto POC); esta unidade **não**
introduz runner nem libs de teste (`spring-boot-starter-test` permanece sem suíte). Verificação
manual, por tarefa:

- Subir a aplicação (`mvn spring-boot:run`, porta 8081) e conferir `GET /.well-known/jwks.json`
  (chave corrente presente, sem material privado) e o Swagger UI/`/v3/api-docs` com os novos
  endpoints.
- `POST /credencial/rotacionar-chave` gera nova chave corrente (novo `kid` no JWKS; `kid` anterior
  ainda presente).
- Reiniciar a aplicação e confirmar que o `kid` corrente do JWKS **não** muda (chave recarregada do
  keystore).
- Fluxo ponta a ponta com o Keycloak configurado para Signed JWT ("Use JWKS URL"): `GET
  /demo/chamar-app-b` retorna a resposta da App B, comprovando a autenticação por asserção assinada.

## Artefatos opcionais

- `data-model.md` — **dispensado.** As estruturas (entrada de keystore, JWKS, resposta da rotação)
  são pequenas e cabem na seção "Modelo de dados" acima.
- `research.md` — **gerado.** Há decisões técnicas não triviais com tradeoffs e proveniência
  (mecanismo `private_key_jwt`, tipo/algoritmo de chave, formato do keystore, rotação × cache do
  JWKS, versão do springdoc).
- `contracts/` — **dispensado.** O contrato REST é gerado do código por springdoc; manter um
  `.openapi.yaml` à mão contraria a skill `documentacao-api-openapi`. Os contratos ficam descritos na
  seção "Contratos externos".
- `ui/` — **dispensado.** Não há interface de usuário.

## Impacto na documentação autoritativa

Esta unidade altera comportamento **de propósito**, com decisões registradas (descrição da unidade +
respostas às lacunas `substituicao-client-secret`, `persistencia-chave-privada`,
`protecao-endpoint-rotacao`). Drift deliberado a virar tarefa de atualização de **doc** na fase
`tasks` (a doc **não** é editada agora):

- **`.agents/skills/autenticacao-maquina-a-maquina/SKILL.md`** — atualizar: a identidade de cliente
  deixa de ser "identificador + segredo" e passa a "identificador + par de chaves" com autenticação
  `private_key_jwt` (regras 1, 2, 8); o passo de obtenção do token passa a enviar `client_assertion`
  assinada em vez de `client-secret` (Fluxo e ciclo de vida, Entidades e contratos); acrescentar os
  endpoints expostos de credencial (`GET /.well-known/jwks.json`, `POST /credencial/rotacionar-chave`);
  nas Variáveis de ambiente, remover `APP_A_CLIENT_SECRET` e acrescentar as do keystore/chave;
  ressalvar a persistência (regra 7): o **token** segue só em memória, mas o **par de chaves** passa a
  ser persistido; revisar o "Débito técnico conhecido" (o débito do segredo em configuração é
  **encerrado**; registram-se os novos: endpoint de rotação aberto e material privado persistido a
  proteger).
- **`.agents/skills/autenticacao-maquina-a-maquina/references/technical-dependencies.md`** —
  atualizar: incluir a geração/persistência do par de chaves e a publicação do JWKS; a "Configuração
  do cliente no realm do Keycloak" passa a exigir autenticação Signed JWT com "Use JWKS URL"
  apontando para o endereço da App A, em vez do segredo.
- **`.agents/skills/documentacao-api-openapi/SKILL.md`** — atualizar o inventário de caminhos
  expostos: além de `GET /demo/chamar-app-b`, passam a existir `GET /.well-known/jwks.json` e
  `POST /credencial/rotacionar-chave` a documentar.

**Observação de escopo:** as convenções globais de `AGENTS.md` (gestão do client-secret; "não
introduza persistência") e a memória em `.agents/context/discovery-answers.md` também ficam
superadas pelas decisões humanas desta rodada, mas `AGENTS.md`, `.agents/context/*` e `.agents/maps/*`
estão **fora do escopo de edição** deste fluxo; nenhuma tarefa de edição desses arquivos é emitida
aqui. As tarefas de doc miram exclusivamente a documentação autoritativa de domínio (as skills acima).
