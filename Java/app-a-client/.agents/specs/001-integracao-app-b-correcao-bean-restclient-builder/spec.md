# Spec: Correção da falha de inicialização do cliente HTTP da App B

## Visão geral

Restaurar a inicialização da aplicação e o fluxo de demonstração de chamada à App B,
hoje impedidos porque o cliente HTTP de saída (`appBRestClient`) não consegue ser
criado por faltar a dependência `RestClient.Builder` no contexto Spring.

## Domínio

- Slug: `integracao-app-b`
- Skill: [`.agents/skills/integracao-app-b/SKILL.md`](../../skills/integracao-app-b/SKILL.md)

> **Reescopo.** Esta correção foi originalmente aberta sob o domínio Autenticação
> Máquina-a-Máquina e, por decisão humana (lacuna `dominio-dono-appbrestclient`), foi
> reaberta sob **Integração com a App B** — dono do bean `appBRestClient`, conforme o
> `functional-map.md` e a fronteira declarada na skill de Autenticação (que exclui
> explicitamente anexar o token a uma chamada a jusante e consumir o serviço).

## Escopo

**In:**

- Subida da aplicação (inicialização do contexto Spring) sem falha de criação de bean.
- Acionamento do fluxo de demonstração de chamada à App B (`GET /demo/chamar-app-b`):
  consumir `GET /api/protegido` com o bearer token anexado automaticamente e repassar a
  resposta verbatim.

**Out:**

- Qualquer alteração no fluxo de autenticação `client_credentials` e na obtenção/renovação
  do token (domínio de Autenticação; preservado sem mudanças).
- Introdução de tratamento de erro especializado na integração (comportamento atual
  mantido — regra 7 da skill).
- Alteração do contrato ou do caminho consumido na App B (`/api/protegido`) e do endpoint
  exposto (`/demo/chamar-app-b`).
- Mudança do alvo/parametrização (`APP_B_BASE_URL`) ou do registration
  (`keycloak-client-credentials`).

## Fronteira de domínio

**Este spec implementa:**

- Correção da fiação (wiring) do bean `appBRestClient` em `OAuth2RestClientConfig`, de
  modo que o cliente HTTP de saída da App B seja construído com sucesso na inicialização
  (resolvendo a dependência `RestClient.Builder` hoje ausente no contexto), preservando o
  `baseUrl` (`app.app-b.base-url`), o `OAuth2ClientHttpRequestInterceptor` e o resolvedor
  fixo do registration `keycloak-client-credentials`.

**Pertence a outros domínios (dependência cross-domain, não vira tarefa aqui):**

- Beans de obtenção/renovação do token (`authorizedClientService`, `authorizedClientManager`,
  provider `clientCredentials()`) → skill `autenticacao-maquina-a-maquina`.
- Validação da assinatura do JWT via JWKS → App B (resource server).

## Histórias de usuário

1. Como operador da aplicação, quero que a App A inicialize sem erro de contexto Spring,
   para que o serviço fique disponível na porta `8081`.
2. Como desenvolvedor, quero acionar `GET /demo/chamar-app-b` e receber a resposta da App B,
   para validar o fluxo ponta a ponta (App A obtém o token e chama a App B com ele).

## Critérios de aceite

**História 1 — Inicialização:**

- Dado o projeto com configuração válida, quando a aplicação é iniciada
  (`mvn spring-boot:run`), então o contexto Spring carrega sem o erro
  `Parameter 0 of method appBRestClient ... required a bean of type
  'org.springframework.web.client.RestClient$Builder'` e a aplicação passa a escutar na
  porta `8081`.
- Dado o contexto iniciado, quando os beans são criados, então `appBRestClient` é
  instanciado como um `RestClient` com `baseUrl` igual a `app.app-b.base-url` e com o
  `OAuth2ClientHttpRequestInterceptor` anexado.

**História 2 — Chamada à App B:**

- Dada a aplicação no ar e a App B disponível no endereço de `APP_B_BASE_URL`, quando se
  faz `GET /demo/chamar-app-b`, então a aplicação emite `GET {APP_B_BASE_URL}/api/protegido`
  com o cabeçalho `Authorization: Bearer <jwt>` anexado automaticamente e devolve, como
  texto, o corpo respondido pela App B (verbatim).
- Dada a credencial resolvida, quando a chamada de saída é montada, então o registration
  usado é sempre `keycloak-client-credentials` (sem seleção dinâmica por requisição).
- Dada uma falha da App B (401/403/5xx/indisponibilidade), quando ela ocorre, então a
  falha propaga como falha da requisição de demonstração, sem tradução para erro de negócio
  próprio (comportamento atual preservado).

## Comportamento esperado (correção de bug)

- **Comportamento de referência (skill `integracao-app-b`).** O cliente HTTP da App B é a
  configuração lógica do cliente de saída — o endereço-base da App B mais o interceptor que
  anexa a credencial de máquina resolvida pelo registration `keycloak-client-credentials`.
  Esse cliente deve existir no contexto para que `AppBClient` e `DemoController` consumam a
  App B; a aplicação deve iniciar normalmente e o endpoint de demonstração deve acionar o
  fluxo ponta a ponta.
- **Desvio observado.** Na inicialização, o bean `appBRestClient`
  (`com.dbfinanceira.appa.config.OAuth2RestClientConfig`) declara como parâmetro um
  `RestClient.Builder`, que não está presente no contexto Spring; o container não encontra o
  bean e aborta a subida com `APPLICATION FAILED TO START` /
  `required a bean of type 'org.springframework.web.client.RestClient$Builder' that could
  not be found`. Com isso, a aplicação sequer sobe — embora a causa esteja restrita à
  construção do cliente de saída da App B, e não à lógica de obtenção do token.

## Dependências cross-domain

- **`autenticacao-maquina-a-maquina`** — fornece o `OAuth2AuthorizedClientManager` (token
  `client_credentials`) consumido pelo `OAuth2ClientHttpRequestInterceptor` que o
  `appBRestClient` anexa; sua lógica não é alterada por esta correção.
- **App B** — destino da chamada (`GET /api/protegido`) e responsável pela validação do JWT
  via JWKS; contrato consumido de forma opaca.

## Riscos e observações

- **Causa-raiz técnica em aberto para o plano.** A investigação de por que o
  `RestClient.Builder` não está disponível no contexto (autoconfiguração do Spring Boot
  `4.0.6`, ordem/condições de autoconfiguração, ou necessidade de obter o builder por outra
  via) e a escolha da abordagem de correção pertencem ao `plan.md`/`research.md`, não a este
  spec. Os critérios de aceite fixam o resultado (a aplicação sobe e o cliente é construído
  preservando `baseUrl` + interceptor + registration), não o meio.
- **Preservar a autenticação atual.** Constraint global do projeto: o fluxo
  `client_credentials` e a obtenção/renovação do token não devem ser alterados por esta
  correção.
- **Sem testes automatizados (POC).** A validação é manual (subir a aplicação e acionar
  `GET /demo/chamar-app-b`), conforme decisão registrada em `discovery-answers.md`.
- **OpenAPI.** O endpoint exposto não muda; não há alteração de contrato a refletir na
  documentação OpenAPI nesta correção.
