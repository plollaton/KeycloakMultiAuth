# Research: Gestão de Chaves RSA e Publicação JWKS

## Escopo de dependências Maven do scaffold

**Contexto.** Este domínio cria o scaffold Maven do projeto (nenhum `pom.xml` existe ainda). O
`AGENTS.md` fixa a stack completa do projeto inteiro — incluindo `spring-boot-starter-oauth2-client`
e `spring-boot-starter-oauth2-resource-server` —, mas esses dois starters só são configurados
(client registration, issuer/jwk-set-uri) pelo domínio "Servidor de Recursos OAuth2", que ainda não existe.

**Alternativas:**

- **Trazer a stack completa agora** — declara todos os starters fixados em `AGENTS.md` de uma vez
  no `pom.xml` criado por esta unidade, mesmo os que só serão configurados por domínios futuros.
- **Trazer apenas o que este domínio compila e configura** — `spring-boot-starter-web`,
  `spring-boot-starter-security`, `springdoc-openapi-starter-webmvc-ui` e uma dependência direta
  em `com.nimbusds:nimbus-jose-jwt` (já que os starters OAuth2, que trariam o Nimbus
  transitivamente, ainda não entram); os dois starters OAuth2 ficam pendentes, para os domínios
  que os configuram.

**Decisão:** trazer apenas o que este domínio compila e configura; os starters
`spring-boot-starter-oauth2-client` e `spring-boot-starter-oauth2-resource-server` ficam
explicitamente pendentes.

**Confirmação:** a lista de bibliotecas em si (Spring Boot 4.1.x, Nimbus JOSE+JWT,
`spring-boot-starter-oauth2-client`, `spring-boot-starter-oauth2-resource-server`) está fixada sem
ressalva em `AGENTS.md`; a decisão registrada aqui é apenas de sequenciamento (quando cada peça
entra no `pom.xml`), atribuição que o `plan-authoring` reserva a este arquivo.

**Consequências:** o `pom.xml` desta unidade não inclui starters que nenhuma classe desta unidade
usa; o domínio "Servidor de Recursos OAuth2" adiciona
`spring-boot-starter-oauth2-resource-server` em seu próprio plano,
quando precisar configurá-lo — nesse momento a dependência direta em
`nimbus-jose-jwt` pode ser reavaliada (o starter passa a trazer o Nimbus transitivamente).

## Estrutura de pacotes

**Contexto.** `AGENTS.md` declara que o layout de pacotes "será definida quando o scaffold Maven
for criado" — ou seja, esta unidade, sendo quem cria o scaffold, é quem define o layout inicial.
Nenhum padrão arquitetural (hexagonal, camadas, etc.) é mencionado em nenhum artefato de negócio.

**Alternativas:**

- **Pacote por camada técnica** (`controller`, `service`, `config`) — comum em POCs pequenas, mas
  espalha as poucas classes de cada domínio entre pacotes que crescem de forma desbalanceada à
  medida que os domínios 2 e 3 forem adicionados.
- **Pacote por domínio de negócio** (`jwks`, `resourceserver`, mais um pacote
  `web` para a classe de aplicação e configuração REST/OpenAPI compartilhada) — espelha os
  domínios já identificados em `functional-map.md`, mantendo cada domínio autocontido.

**Decisão:** pacote por domínio de negócio, sem camada arquitetural adicional.

**Confirmação:** decisão de estrutura, não de tecnologia/biblioteca — cai fora da exigência de
base firme do `plan-authoring`, mas é registrada aqui pela relevância para as tasks futuras
(inclusive do domínio "Servidor de Recursos OAuth2", que deve seguir o mesmo padrão).

**Consequências:** o domínio "Servidor de Recursos OAuth2" recebe o pacote
`com.aplicacaosegura.resourceserver` quando for implementado, mantendo a mesma convenção.

## Postura padrão da SecurityFilterChain

**Contexto.** Este domínio precisa liberar `GET /oauth2/jwks` (regra 6 da skill) e os caminhos do
Swagger/OpenAPI de autenticação. A regra de quais outros caminhos exigem ou não autenticação
pertence ao domínio "Servidor de Recursos OAuth2", que ainda não existe — mas a
`SecurityFilterChain` precisa de alguma regra para `anyRequest()` para compilar e funcionar.

**Alternativas:**

- **`anyRequest().permitAll()`** — libera tudo que não está explicitamente listado; simples, mas
  deixa a aplicação totalmente aberta até o domínio "Servidor de Recursos OAuth2" ser
  implementado, inclusive para endpoints que vierem a existir antes disso.
- **`anyRequest().authenticated()`**, sem `AuthenticationProvider`/`JwtDecoder` configurado nesta
  unidade — nega por padrão qualquer caminho não listado (a ausência de um provedor de
  autenticação faz a requisição falhar em vez de autenticar), postura restritiva por padrão.

**Decisão:** `anyRequest().authenticated()`.

**Confirmação:** não é escolha de biblioteca/framework — é o valor do parâmetro de uma regra já
usando a API padrão do Spring Security (`HttpSecurity.authorizeHttpRequests`) citada nominalmente
em `functional-map.md`. Padrão "negar por padrão, liberar só o explicitamente público" não
contradiz nenhuma regra registrada nas fontes de negócio.

**Consequências:** o domínio "Servidor de Recursos OAuth2" substitui esta regra genérica por suas
próprias regras (`GET /api/protected` autenticado, `GET /api/public` e `GET /actuator/health`
públicos) e pela configuração do `JwtDecoder` contra o JWKS do Keycloak; até lá, qualquer endpoint
além dos listados nesta unidade responde com falha de autenticação em vez de ficar acessível.

## Geração do `kid`

**Contexto.** A skill de domínio deixa a estratégia de geração do `kid` a cargo da implementação,
exigindo apenas unicidade.

**Decisão:** `UUID.randomUUID().toString()` no momento da construção do bean `RSAKey`.

**Confirmação:** uso direto de API do JDK, sem dependência nova nem padrão debatível; unicidade
prática suficiente para uma POC com um único par de chaves ativo por vez.

**Consequências:** nenhuma — o `kid` muda a cada reinicialização com geração em memória, o que já
é o comportamento de rotação esperado pela regra 7 da skill.

## Lombok

**Contexto.** `AGENTS.md` lista Lombok como dependência opcional da stack.

**Decisão:** não adotado nesta unidade. As classes deste domínio (configuração do bean `RSAKey`,
controller do JWKS e a classe de propriedades do carregamento PKCS12) são poucas e cabem em
`record`/classes simples do Java 21, sem necessidade de reduzir boilerplate de getters/setters.

**Confirmação:** a própria stack marca Lombok como opcional, sem exigir uma direção; a omissão não
contradiz nenhuma fonte.

**Consequências:** se um domínio futuro (2 ou 3) precisar de classes com mais boilerplate
(DTOs com múltiplos campos mutáveis, por exemplo), a adoção de Lombok pode ser reaberta no plano
daquele domínio sem impacto neste.
