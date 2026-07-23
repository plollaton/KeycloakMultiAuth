# Research: Correção da falha de inicialização do cliente HTTP da App B

## Origem da dependência ausente (`RestClient.Builder`) e abordagem de correção

**Contexto.** Na inicialização, o bean `appBRestClient` (`com.dbfinanceira.appa.config.OAuth2RestClientConfig`)
declara o parâmetro `RestClient.Builder`, que o container Spring não encontra, abortando a subida com
`required a bean of type 'org.springframework.web.client.RestClient$Builder' that could not be found`.
O projeto usa Spring Boot `4.0.6` com `spring-boot-starter-web`. O `RestClient.Builder` é, em condições
normais, fornecido por autoconfiguração do Spring Boot; no arranjo atual esse bean não está disponível
para injeção — seja por uma condição/ordenação de autoconfiguração não satisfeita neste contexto, seja
por reorganização dos módulos de autoconfiguração na linha 4.x. O diagnóstico exato da autoconfiguração
é secundário para esta correção: a abordagem escolhida não depende desse bean.

**Alternativas:**

- **A — Construir via fábrica estática `RestClient.builder()` no próprio bean.** Remove o parâmetro
  injetado e cria o builder localmente. Não depende de autoconfiguração, não adiciona dependência e
  altera apenas o bean que falha. Contrapartida: não herda automaticamente eventuais customizers
  globais do builder autoconfigurado — irrelevante nesta POC, que faz uma chamada simples e repassa
  texto opaco.
- **B — Declarar explicitamente um bean `RestClient.Builder`** no contexto. Efeito equivalente ao A,
  porém adiciona um bean extra sem ganho para o caso.
- **C — Diagnosticar e restaurar a autoconfiguração do `RestClient.Builder`** (ajustar
  dependências/condições para que o bean autoconfigurado passe a existir). Maior superfície de mudança
  e incerteza; extrapola o escopo de uma correção mínima de POC.

**Decisão:** Alternativa **A**.

**Fundamentação (procedência).** Decisão técnica inteiramente dentro do stack já em uso
(`spring-web`/`RestClient`, presente via `spring-boot-starter-web` — evidência em `AGENTS.md` e
`pom.xml`); não introduz biblioteca, framework ou dependência nova, portanto não constitui uma decisão
tecnológica em aberto que exija escalonamento. A base concreta é o próprio erro observado: o bean
autoconfigurado está ausente, logo não se deve depender dele; a fábrica estática é a via robusta e de
menor mudança. Alinha-se às constraints do projeto (preservar a autenticação `client_credentials`;
tratar como POC; alterar o mínimo).

**Consequências.** O contexto passa a inicializar normalmente; o `appBRestClient` mantém `baseUrl`
(`app.app-b.base-url`), o `OAuth2ClientHttpRequestInterceptor` e o registration fixo
`keycloak-client-credentials`. Nenhum outro bean é afetado, e os beans do domínio de Autenticação
permanecem intactos. Se no futuro houver necessidade de herdar customizers globais de `RestClient`,
reavaliar a Alternativa C.
