# Plan: Endpoint Público de Acesso Cruzado a Aplicação Externa (GET /api/cross)

## Stack and structure

Aplicação Spring Boot 4.1.x / Java 21 / Jakarta EE 11, conforme `AGENTS.md`. O novo endpoint vive
no pacote `com.aplicacaosegura.resourceserver`, junto de `ResourceServerExampleController`
(`GET /api/protected`, `GET /api/public`), mesma família de endpoints de exemplo já mantida pelo
domínio "Servidor de Recursos OAuth2". A chamada de saída consome, por injeção, o
`OAuth2AuthorizedClientManager` já configurado em
`com.aplicacaosegura.oauth2client.OAuth2ClientAssertionConfig` (domínio "Cliente OAuth2 com Client
Assertion JWT") — o mesmo bean já usado por `OAuth2ClientAssertionDiagnosticsController` — sem
alterar aquele pacote.

Nenhuma dependência Maven nova: a chamada HTTP de saída usa `RestClient` do Spring Framework,
já disponível via `spring-boot-starter-web` e já em uso internamente pelo Spring Security OAuth2
(`RestClientClientCredentialsTokenResponseClient`, consumido por
`OAuth2ClientAssertionConfig`).

## Technical decisions

- **Obtenção do `access_token`**: reaproveita o `OAuth2AuthorizedClientManager` já existente,
  acionado com o mesmo `REGISTRATION_ID = "keycloak"` já usado por
  `OAuth2ClientAssertionDiagnosticsController`, mas com um `principal name` próprio (`"cross"`),
  para não misturar a entrada em cache do `OAuth2AuthorizedClientService` com a do fluxo
  diagnóstico. Decisão humana registrada no gap `g1` desta fase de plano. Confirmação e
  alternativa descartada em
  [`research.md`](./research.md#obtenção-do-access_token-para-a-chamada-de-saída).
- **Cliente HTTP da chamada de saída**: `RestClient` configurado com a base URL lida de
  `app.cross.external-base-url`, chamando `GET {base-url}/api/protected` com o cabeçalho
  `Authorization: Bearer <access_token>`. Confirmação em
  [`research.md`](./research.md#cliente-http-para-a-chamada-de-saída).
- **Endereço da aplicação externa**: propriedade `app.cross.external-base-url`, sem valor padrão
  em nenhum dos `application.yml` (`resources-dev`, `resources-docker`) — só é resolvida pela
  variável informada na linha de comando de inicialização (argumento
  `--app.cross.external-base-url=...` ou variável de ambiente `APP_CROSS_EXTERNAL_BASE_URL`,
  ambos automaticamente reconhecidos pelo binding relaxado padrão do Spring Boot). Sem essa
  variável informada na inicialização, o contexto falha ao subir (placeholder não resolvido) —
  resolve o ponto em aberto do `spec.md` sobre o comportamento sem endereço configurado.
  Confirmação em
  [`research.md`](./research.md#configuração-do-endereço-da-aplicação-externa).
- **Classificação na `SecurityFilterChain`**: `SecurityConfig` (pacote `web`, já existente) ganha
  `GET /api/cross` na lista de caminhos com `permitAll()`, ao lado de `GET /api/public` e
  `GET /actuator/health` — decisão humana registrada no gap `g1` da fase de spec (endpoint
  público, sem exigência de `access_token` do chamador).
- **Novo controller**: `CrossAccessController`, no pacote `resourceserver`, expõe
  `GET /api/cross`. Recebe `OAuth2AuthorizedClientManager` e o `RestClient` configurado por
  injeção de construtor, seguindo o mesmo estilo de `OAuth2ClientAssertionDiagnosticsController`.
- **Resposta e tratamento de falha**: mesma convenção já usada por
  `OAuth2ClientAssertionDiagnosticsController` — `200` com um novo DTO mínimo
  (`CrossAccessResponse`, análogo a `OAuth2ClientAssertionDiagnosticsResponse`) quando a chamada
  de saída retorna sucesso; `502 Bad Gateway` sem corpo adicional quando a obtenção do
  `access_token` falha (`OAuth2AuthorizationException`) ou quando a chamada a `GET /api/protected`
  da aplicação externa falha (status não 2xx ou erro de rede via `RestClientException`). Em nenhum
  caso o `access_token` ou material de chave privada é incluído na resposta. Confirmação em
  [`research.md`](./research.md#tratamento-de-falha-da-chamada-de-saída).

## Data model

Não se aplica: este endpoint não persiste nenhuma entidade. `data-model.md` dispensado.

## External contracts

O contrato de `GET /api/cross` está totalmente especificado nos critérios de aceite do `spec.md`
(`200` em caso de sucesso da chamada de saída, `502` em caso de falha, sem contrato de corpo além
disso). Como nos demais domínios do projeto, a especificação OpenAPI é gerada em tempo de execução
pelo springdoc-openapi a partir das anotações do novo controller — pasta `contracts/` dispensada
pelo mesmo motivo já registrado nos planos anteriores: duplicar o contrato em um arquivo mantido à
mão não reduz ambiguidade adicional para as tasks.

## Interface

Não se aplica — endpoint puramente backend, sem interface própria. `ui/` dispensada.

## Testing strategy

Esta POC não implementa testes automatizados (unitários, integração ou e2e), conforme decisão
registrada em `.agents/context/discovery-answers.md`. Nenhuma infraestrutura de teste é introduzida
por esta unidade; a verificação do fluxo depende de uma aplicação externa real, acessível no
endereço configurado em `app.cross.external-base-url`, expondo `GET /api/protected`, e de um
Keycloak real emitindo o `access_token` consumido pelo `OAuth2AuthorizedClientManager` já
existente.

## Impact on the authoritative documentation

Drift deliberado na skill `servidor-recursos-oauth2`: a tabela de classificação de endpoints em
"Entidades e dados" e as regras de negócio 1 a 5 não incluem `GET /api/cross`. A decisão de que
esse endpoint é público está registrada no gap `g1` da fase de spec desta unidade; uma task na fase
`tasks` atualiza a skill (tabela de classificação e regra correspondente) para incluir
`GET /api/cross` como público, ao lado dos demais endpoints já documentados.

Nenhum impacto na skill `cliente-oauth2-client-assertion`: seu comportamento obrigatório documentado
termina na obtenção do `access_token` (regra 6 e "Restrições e validações" daquela skill); esta
unidade não implementa nem altera nenhum comportamento daquele domínio, apenas consome o
`OAuth2AuthorizedClientManager` que ele já expõe.
