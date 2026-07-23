# Tasks: Credencial do cliente por Signed JWT com rotação de chaves

Checklist derivado de [`spec.md`](./spec.md) e [`plan.md`](./plan.md). Mudança transversal aos
domínios **Autenticação Máquina-a-Máquina** (`autenticacao-maquina-a-maquina`) e **Integração com a
App B** (`integracao-app-b`).

> **Sem testes automatizados.** Decisão validada (`testing-strategy`, projeto POC): as tarefas não
> incluem passos de teste automatizado. A validação é manual, no checkpoint de implementação,
> conforme [`test-cases.md`](./test-cases.md) — fora desta checklist. As referências `TC-*` abaixo
> apenas indicam a cobertura esperada de cada tarefa.

---

- [ ] **T1. Atualizar a documentação autoritativa afetada (drift deliberado)**

  Primeira tarefa: a implementação lê a doc durante a execução, então a doc corrigida precede as
  tarefas que a consomem. Origem: drift deliberado registrado em `plan.md` ("Impacto na documentação
  autoritativa"), decorrente das decisões humanas desta unidade.

  - Alvo: `.agents/skills/autenticacao-maquina-a-maquina/SKILL.md`.
    - Seções: "Regras de negócio" (regras 1, 2 e 8), "Fluxo e ciclo de vida do token" (obtenção do
      token), "Entidades e contratos", "Variáveis de ambiente do domínio", "Débito técnico conhecido".
    - Delta: a identidade do cliente deixa de ser "identificador + segredo" e passa a "identificador
      + par de chaves" com autenticação `private_key_jwt` (asserção assinada); a obtenção do token
      envia `client_assertion` no lugar do `client-secret`; acrescentar os endpoints de credencial
      expostos (`GET /.well-known/jwks.json`, `POST /credencial/rotacionar-chave`); remover
      `APP_A_CLIENT_SECRET` e acrescentar as variáveis do keystore/chave; ressalvar que o **token**
      segue apenas em memória, mas o **par de chaves** passa a ser persistido; encerrar o débito do
      segredo em configuração e registrar os novos (endpoint de rotação aberto, material privado
      persistido a proteger).
    - Origem: História 1 (autenticação por Signed JWT; remoção do `client-secret`) e História 4
      (persistência do par de chaves).
  - Alvo: `.agents/skills/autenticacao-maquina-a-maquina/references/technical-dependencies.md`.
    - Seção: itens de pré-condições técnicas.
    - Delta: incluir a geração/persistência do par de chaves e a publicação do JWKS como
      pré-condições; a "Configuração do cliente no realm do Keycloak" passa a exigir autenticação
      Signed JWT com "Use JWKS URL" apontando para o endereço da App A, em vez do segredo.
    - Origem: História 1 e História 3 (Keycloak busca as chaves públicas no endereço JWKS).
  - Alvo: `.agents/skills/documentacao-api-openapi/SKILL.md`.
    - Seções: "Como aplicar" e "Ferramentas e artefatos envolvidos" (inventário de caminhos a
      documentar).
    - Delta: acrescentar `GET /.well-known/jwks.json` e `POST /credencial/rotacionar-chave` aos
      caminhos expostos a documentar, além do `GET /demo/chamar-app-b` já citado.
    - Origem: História 2 e História 3 (novos endpoints expostos).
  - Observação: o domínio `integracao-app-b` **não** tem tarefa de doc — seu comportamento
    documentado (consumo de `GET /api/protegido` com bearer anexado) não muda; os novos endpoints
    expostos são cobertos pela doc do domínio de autenticação e pela skill técnica de OpenAPI.

- [ ] **T2. Geração e persistência do par de chaves e publicação do JWKS**

  - Pré-requisito (infra desta unidade): adicionar ao `pom.xml` o `springdoc-openapi-starter-webmvc-ui`
    na versão compatível com a linha do Spring Boot 4.0.6 (confirmar na matriz de compatibilidade do
    springdoc no momento da adoção) — é o primeiro endpoint novo a ser documentado via OpenAPI.
  - Componente de gestão de chaves (novo pacote `com.dbfinanceira.appa.credential`) que, no boot,
    carrega o par de chaves do keystore PKCS12 persistido ou, na ausência, gera o par inicial
    (RSA-2048/RS256, certificado X.509 autoassinado) e o persiste; expõe a chave pública corrente
    como JWK (`kid`) e o `JWKSet` público.
  - Configuração externalizada do keystore (caminho e senha) no `application.yml`, no padrão de
    variáveis de ambiente com defaults de desenvolvimento.
  - Endpoint `GET /.well-known/jwks.json` publicando o `JWKSet` com a(s) chave(s) pública(s)
    corrente(s), sem qualquer material privado; anotado para o contrato OpenAPI (incluir também ao
    menos `@Operation` no `GET /demo/chamar-app-b` existente).
  - Cobre TC-7 (JWKS sem material privado) e TC-10 (`kid` estável após reinício).

- [ ] **T3. Autenticação do cliente no Keycloak por Signed JWT (`private_key_jwt`)**

  - Precondição externa (fora do repositório): o cliente no realm do Keycloak deve estar configurado
    para autenticação Signed JWT com "Use JWKS URL" apontando para o endereço JWKS da App A e com o
    client scope `app-b.invoke`.
  - Ajustar o `OAuth2RestClientConfig` para o fluxo `client_credentials` autenticar por
    `private_key_jwt`, montando a `client_assertion` assinada com a chave privada corrente fornecida
    pelo componente de chaves (T2), preservando o grant e o escopo `app-b.invoke`.
  - No `application.yml`, definir `client-authentication-method: private_key_jwt` no registration
    `keycloak-client-credentials` e **remover** o `client-secret`/`APP_A_CLIENT_SECRET`.
  - Preservar inalterados o `AppBClient`, o `DemoController` e a anexação do bearer nas chamadas à
    App B.
  - Cobre TC-1 (fluxo ponta a ponta por Signed JWT), TC-2 (ausência de segredo), TC-3 (grant/escopo
    preservados) e TC-11 (token reobtido em memória após reinício com a chave persistida).

- [ ] **T4. Rotação da chave via endpoint POST com sobreposição corrente + anterior**

  - Estender o componente de chaves (T2) com a operação de rotação: gera um novo par, promove-o a
    corrente, mantém a chave anterior e passa a publicar ambas as chaves públicas no `JWKSet`
    (assinando sempre com a corrente), para tolerar o cache do JWKS no Keycloak.
  - Endpoint `POST /credencial/rotacionar-chave`, sem corpo de requisição, retornando `200` com o
    identificador da nova chave corrente (`kid`) e o `criadaEm`; sem expor material privado; **aberto**
    (sem autenticação, coerente com a POC), anotado para o contrato OpenAPI.
  - Cobre TC-4 (geração inicial via POST), TC-5 (novo `kid` na rotação), TC-6 (endpoint aberto), TC-8
    (sobreposição corrente + anterior no JWKS) e TC-9 (tolerância à janela de cache do Keycloak).
