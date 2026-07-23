# AGENTS.md — app-a-client

Fonte única de verdade para agentes que atuam neste repositório. Leia este arquivo antes de
qualquer tarefa. Convenções globais e estáveis vivem aqui; detalhes executáveis (how-tos) e o
conhecimento de domínio vivem sob demanda em `.agents/` (ver seção final).

## Visão geral e domínio

`app-a-client` é o **backend da aplicação cliente (App A)** de uma POC de integração autenticada
com Keycloak. A aplicação se autentica no Keycloak pelo fluxo OAuth2 `client_credentials`
(máquina-a-máquina, sem usuário/sessão), obtém um access token (JWT) e o utiliza para chamar uma
**aplicação servidora (App B)**, que valida a assinatura do token via JWKS.

O trabalho neste projeto é de **manutenção** sobre o código existente, com escopo no projeto
inteiro. Há dois contextos de negócio (detalhados em `.agents/maps/functional-map.md`):

1. **Autenticação Máquina-a-Máquina** — obter e renovar a credencial de máquina (`client_credentials`)
   junto ao Keycloak. Domínio fundacional, sem dependências.
2. **Integração com a App B** — consumir o endpoint protegido da App B anexando automaticamente o
   bearer token, e expor um endpoint de demonstração do fluxo ponta a ponta. Depende do domínio 1.

## Stack principal

- **Java 21**, **Spring Boot 4.0.6** (parent `spring-boot-starter-parent`).
- Build via **Maven** (`pom.xml`; não há Maven Wrapper `mvnw` no repositório).
- Starters em uso:
  - `spring-boot-starter-web` — API REST (endpoint de demonstração) e `RestClient` HTTP.
  - `spring-boot-starter-oauth2-client` — cliente OAuth2 `client_credentials`.
  - `spring-boot-starter-actuator` — observabilidade.
  - `spring-boot-starter-test` — presente no `pom.xml`, porém sem suíte de testes (ver constraints).

## Estrutura padrão do repositório

```
app-a-client/
├── pom.xml                       # build Maven, dependências, Java 21
├── src/main/java/com/dbfinanceira/appa/
│   ├── AppAClientApplication.java   # bootstrap Spring Boot
│   ├── client/AppBClient.java       # RestClient que chama GET /api/protegido da App B
│   ├── config/OAuth2RestClientConfig.java  # beans OAuth2 client_credentials + RestClient
│   └── web/DemoController.java      # GET /demo/chamar-app-b (gatilho manual do fluxo)
├── src/main/resources/application.yml   # configuração e variáveis de ambiente com defaults
└── .agents/                      # base de conhecimento para agentes (ver seção final)
```

## Convenções de arquitetura

- **Fluxo `client_credentials` puro (M2M):** usa `AuthorizedClientServiceOAuth2AuthorizedClientManager`
  (não o gerenciador baseado em `HttpServletRequest`, próprio de login OAuth2 de usuário). O
  `OAuth2ClientHttpRequestInterceptor` obtém/renova o token e anexa `Authorization: Bearer <jwt>`
  automaticamente a cada chamada à App B.
- **Descoberta OpenID Connect automática:** `token-uri` e `jwks-uri` são resolvidos a partir do
  `issuer-uri` (`/.well-known/openid-configuration`); não os configure manualmente.
- **Persistência:** não há banco de dados. O access token é mantido **em memória** via
  `InMemoryOAuth2AuthorizedClientService`. Adequado a uma POC; não introduza persistência sem
  validação humana.
- **Configuração externalizada:** endpoints e credenciais são parametrizados por variáveis de
  ambiente com defaults no `application.yml`. Mantenha novas configurações nesse mesmo padrão.
- **Observabilidade:** Spring Boot Actuator habilitado; logging de `org.springframework.security`
  em nível `INFO`.

## Constraints globais

- **Preservar a autenticação atual.** O fluxo OAuth2 `client_credentials` contra o Keycloak é a
  base e deve ser mantido. O único ajuste confirmado até aqui é a evolução futura da gestão do
  segredo (abaixo); qualquer outro ajuste exige validação humana.
- **Gestão do `client-secret`:** por ora o segredo é mantido no `application.yml` (via variável de
  ambiente com default). Decisão validada; evoluir depois para uma solução mais segura (ex.: cofre
  de segredos). Não versione segredos reais.
- **Sem testes automatizados.** O projeto é tratado como POC; não há suíte a manter. A dependência
  `spring-boot-starter-test` permanece no `pom.xml`, mas não escreva/exija testes salvo decisão
  humana em contrário.
- **Contrato REST documentado via OpenAPI** (`springdoc-openapi`), versionado junto ao código.
- **`package.json` na raiz é ruído** (dependências `latest`/`nvm` sem relação com a aplicação Java);
  não faz parte da stack e não deve ser tratado como tal.

## Comandos essenciais

Execute a partir da raiz do projeto (não há `mvnw`; use um Maven instalado localmente):

- **Build:** `mvn clean package`
- **Executar a aplicação:** `mvn spring-boot:run` (sobe na porta `8081`; App B esperada em `8082`)
- **Testes:** `mvn test` — não há testes no projeto (POC); o comando existe mas nada é exercido.
- **Lint/format:** nenhuma ferramenta de lint/formatação configurada no repositório.

## Variáveis de ambiente

As variáveis de ambiente e seus defaults estão declarados em
[`src/main/resources/application.yml`](./src/main/resources/application.yml) — consulte esse arquivo
como fonte das variáveis necessárias (`APP_A_CLIENT_ID`, `APP_A_CLIENT_SECRET`,
`KEYCLOAK_ISSUER_URI`, `APP_B_BASE_URL`). Os defaults servem apenas a desenvolvimento; sobrescreva-os
por ambiente e nunca comprometa segredos reais.

## Estrutura de conhecimento em `.agents/`

O diretório `.agents/` concentra a base de conhecimento consultável pelos agentes:

- `.agents/maps/` — mapas do sistema (ex.: `functional-map.md`, o mapa dos domínios de negócio,
  suas fronteiras e dependências). Comece por aqui para situar-se no domínio.
- `.agents/skills/` — skills carregadas **sob demanda** pelo agente, guiadas pelo campo
  `description` do frontmatter de cada uma. A pasta reúne tanto skills de **negócio (domínio)**
  quanto skills **técnicas (transversais)**. Não há índice manual: cada skill se declara por si.
- `.agents/context/` — memória durável da descoberta (ex.: `discovery-answers.md`), cujas
  restrições têm precedência sobre inferências posteriores.

### Fonte de verdade do domínio

As **skills locais em `.agents/skills/`** são a fonte autoritativa da documentação de domínio.
Consulte-as e leia-as **antes de implementar qualquer regra de negócio**, e mantenha-as
atualizadas quando o comportamento do domínio mudar.
