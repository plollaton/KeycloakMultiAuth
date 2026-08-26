# Research: Endpoint Público de Acesso Cruzado a Aplicação Externa (GET /api/cross)

## Obtenção do access_token para a chamada de saída

**Contexto.** `GET /api/cross` precisa apresentar um `access_token` válido na chamada de saída a
`GET /api/protected` da aplicação externa. A aplicação já mantém, no pacote `oauth2client`, um
`OAuth2AuthorizedClientManager` que executa a troca `client_credentials` com `client_assertion`
junto ao Keycloak, hoje acionado apenas pelo endpoint diagnóstico
`GET /diagnostics/oauth2-client-assertion`.

**Alternativas:**

- **Reaproveitar o `OAuth2AuthorizedClientManager` já existente** — mesma `client registration`
  (`keycloak`, `client-id: of-pagamentos`), sem nenhuma configuração nova no Keycloak nem no
  `application.yml`.
- **Nova `client registration` dedicada a esta chamada** — permitiria um `client_id`/`aud` próprios
  para o acesso cruzado, isolado do restante da aplicação, mas exige uma configuração adicional no
  Keycloak e no `application.yml` sem contrapartida de negócio conhecida hoje.

**Decisão:** reaproveitar o `OAuth2AuthorizedClientManager` já existente.

**Confirmação basis:** decisão humana, gap `g1` desta fase de plano.

**Consequências:** o `access_token` usado na chamada de saída carrega a mesma `aud` já configurada
para o client `keycloak` desta aplicação; se a aplicação externa exigir uma audiência diferente da
que o Keycloak emite para esse client, a chamada de saída falhará com `access_token` rejeitado —
comportamento coberto pelo critério de aceite de falha do `spec.md` (resposta `502`).

## Cliente HTTP para a chamada de saída

**Contexto.** É necessário um cliente HTTP para `GET {base-url}/api/protected` da aplicação
externa, incluindo o cabeçalho `Authorization: Bearer <access_token>`.

**Alternativas:**

- **`RestClient`** (Spring Framework) — já disponível via `spring-boot-starter-web`, já em uso
  internamente pelo Spring Security OAuth2 (`RestClientClientCredentialsTokenResponseClient`,
  consumido por `OAuth2ClientAssertionConfig`).
- **`WebClient`** (Spring WebFlux) — exigiria a dependência `spring-boot-starter-webflux`, ausente
  do `pom.xml` e sem uso em nenhum outro ponto da aplicação (stack síncrona, `spring-boot-starter-web`).
- **`RestTemplate`** — suportado, mas descontinuado em favor de `RestClient` nas versões atuais do
  Spring Framework usadas por este projeto.

**Decisão:** `RestClient`.

**Confirmação basis:** já em uso no código (via `RestClientClientCredentialsTokenResponseClient`);
nenhuma dependência nova é introduzida.

**Consequências:** nenhuma dependência Maven adicional; o mesmo cliente já usado internamente pelo
fluxo OAuth2 client passa a ser usado também para uma chamada de negócio explícita.

## Configuração do endereço da aplicação externa

**Contexto.** O endereço base da aplicação externa precisa ser informado por variável passada na
linha de comando de inicialização desta aplicação, não fixado em um `application.yml` por
ambiente (diferente do padrão já usado por `app.security.resource-server.expected-audience`, que
tem valor fixo por perfil).

**Alternativas:**

- **Propriedade Spring Boot sem valor padrão, resolvida via argumento de linha de comando ou
  variável de ambiente** (`app.cross.external-base-url`, binding relaxado padrão do Spring Boot:
  `--app.cross.external-base-url=...` ou `APP_CROSS_EXTERNAL_BASE_URL`) — sem código adicional,
  usa o mecanismo padrão do framework.
- **`@ConfigurationProperties` dedicada com leitura manual de variável de ambiente
  (`System.getenv`)** — reimplementaria, com código próprio, o que o binding relaxado do Spring
  Boot já resolve, sem ganho.

**Decisão:** propriedade `app.cross.external-base-url`, sem valor padrão em nenhum
`application.yml`, resolvida pela variável informada na linha de comando de inicialização.

**Confirmação basis:** comportamento padrão do Spring Boot (binding relaxado de propriedades via
argumento de linha de comando ou variável de ambiente), já a mesma técnica usada pelos
`@Value` existentes no projeto (`issuer-uri`, `expected-audience`), sem reserva.

**Consequências:** sem essa variável informada na inicialização, o contexto Spring falha ao subir
por placeholder não resolvido — a aplicação não inicia sem o endereço configurado, em vez de
aceitar requisições a `GET /api/cross` que falhariam apenas em tempo de execução.

## Tratamento de falha da chamada de saída

**Contexto.** A chamada de saída pode falhar tanto na obtenção do `access_token` (Keycloak
rejeita a troca) quanto na chamada à aplicação externa (token rejeitado, aplicação inacessível,
erro de rede). O `spec.md` exige que, em qualquer uma dessas falhas, a resposta reflita o erro sem
expor o `access_token` nem material de chave privada.

**Alternativas:**

- **`502 Bad Gateway` sem corpo adicional**, mesma convenção já usada por
  `OAuth2ClientAssertionDiagnosticsController` para falha da troca com o Keycloak.
- **Corpo de erro detalhado** (motivo da falha, status HTTP retornado pela aplicação externa) —
  arrisca vazar detalhes da resposta da aplicação externa ou do erro de token na resposta de
  `GET /api/cross`, sem exigência de negócio que peça esse detalhamento.

**Decisão:** `502 Bad Gateway` sem corpo adicional, para qualquer uma das falhas (obtenção do
token ou chamada à aplicação externa).

**Confirmação basis:** já em uso no código, no mesmo tratamento de falha de
`OAuth2ClientAssertionDiagnosticsController`.

**Consequências:** o chamador de `GET /api/cross` distingue sucesso (`200`) de falha (`502`), sem
detalhar a causa específica da falha na resposta.
