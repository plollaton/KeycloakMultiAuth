# Spec: Endpoint Público de Acesso Cruzado a Aplicação Externa (GET /api/cross)

## Overview

Novo endpoint público `GET /api/cross` nesta aplicação, que aciona uma chamada de saída ao endpoint `GET /api/protected` de uma aplicação externa, cujo endereço base é configurável por variável passada na linha de comando de inicialização desta aplicação.

## Domain

- Slug: `acesso-cruzado-api-externa`
- Skill: [`.agents/skills/acesso-cruzado-api-externa/SKILL.md`](../../skills/acesso-cruzado-api-externa/SKILL.md)

## Estado atual do código

A `SecurityFilterChain` (`SecurityConfig`, `src/main/java/com/aplicacaosegura/web/SecurityConfig.java`) libera hoje como públicos `GET /oauth2/jwks`, `GET /diagnostics/oauth2-client-assertion`, `GET /api/public`, `GET /actuator/health` e os caminhos do Swagger/OpenAPI, exigindo `access_token` válido para qualquer outro caminho (`anyRequest().authenticated()`). O pacote `resourceserver` (`ResourceServerExampleController`) expõe apenas `GET /api/protected` (protegido) e `GET /api/public` (público); não existe `GET /api/cross`.

O pacote `oauth2client` já configura um `OAuth2AuthorizedClientManager` (`OAuth2ClientAssertionConfig`) que executa a troca `client_credentials` com `client_assertion` junto ao Keycloak e obtém um `access_token`. O único consumidor atual desse manager é `OAuth2ClientAssertionDiagnosticsController` (`GET /diagnostics/oauth2-client-assertion`), um endpoint exclusivamente diagnóstico que confirma a troca (200) ou a falha (502), sem nunca usar o `access_token` para chamar outra aplicação nem devolvê-lo no corpo da resposta. Nenhum componente desta aplicação realiza hoje uma chamada de saída a uma aplicação externa, e não existe nenhuma propriedade de configuração para o endereço de uma aplicação externa.

## Scope

**In:**

- Novo endpoint `GET /api/cross`, público (sem exigência de `access_token` do chamador).
- Chamada de saída, disparada pelo processamento de `GET /api/cross`, ao endpoint `GET /api/protected` de uma aplicação externa.
- Endereço base da aplicação externa configurável por variável passada na linha de comando de inicialização desta aplicação.
- Inclusão de um `access_token` válido na chamada de saída.

**Out:**

- Mecanismo de obtenção do `access_token` incluído na chamada de saída — decisão a ser informada pelo requerente em rodada futura, fora do escopo desta spec.
- Qualquer alteração na validação de `access_tokens` recebidos pelos endpoints já existentes desta aplicação (`GET /api/protected`, `GET /api/public`) — domínio "Servidor de Recursos OAuth2" já implementado (spec `003-servidor-recursos-oauth2`).
- Qualquer alteração no fluxo de obtenção de `access_token` junto ao Keycloak via `client_assertion` — domínio "Cliente OAuth2 com Client Assertion JWT" (spec `002-cliente-oauth2-client-assertion`).
- Qualquer lógica de negócio interna à aplicação externa chamada por `GET /api/cross` — fora do controle desta aplicação.

## Domain boundary

**This spec implements:**

- Endpoint `GET /api/cross`, na mesma família de endpoints de exemplo já mantida por este domínio (`GET /api/protected`, `GET /api/public`), liberado como público na `SecurityFilterChain` (`SecurityConfig`).
- Chamada HTTP de saída desta aplicação para `GET {endereço-base-configurável}/api/protected` da aplicação externa, disparada pelo processamento de `GET /api/cross`.
- Propriedade de configuração do endereço base da aplicação externa, informada por variável na linha de comando de inicialização desta aplicação.

**Belongs to other domains (cross-domain, does not become a task here):**

- Obtenção do `access_token` incluído na chamada de saída (montagem e assinatura do `client_assertion`, troca `client_credentials` junto ao Keycloak) → domínio "Cliente OAuth2 com Client Assertion JWT"; já implementado via `OAuth2ClientAssertionConfig`/`OAuth2AuthorizedClientManager`, apenas consumido por esta spec.
- Validação do `access_token` apresentado na chamada de saída → responsabilidade da aplicação externa chamada, fora do controle deste repositório.
- Validação de `access_tokens` recebidos pelos endpoints já existentes desta aplicação (`GET /api/protected`) → já coberta pela spec `003-servidor-recursos-oauth2`, não alterada por esta unidade.

## User stories

1. Como chamador desta aplicação, quero acessar `GET /api/cross` sem apresentar credenciais, para acionar o acesso cruzado à aplicação externa sem precisar de um `access_token` próprio.
2. Como operador desta aplicação, quero configurar o endereço da aplicação externa por variável de linha de comando na inicialização, para apontar o acesso cruzado para o ambiente correto sem alterar código.
3. Como chamador desta aplicação, quero que `GET /api/cross` acesse `GET /api/protected` da aplicação externa configurada e me informe o resultado, para confirmar que o acesso cruzado entre as duas aplicações funciona.

## Acceptance criteria

**História 1 — Acesso público ao endpoint de acesso cruzado:**

- Dado nenhum `access_token` apresentado pelo chamador, quando ele executa `GET /api/cross`, então a aplicação processa a requisição sem exigir autenticação do chamador.

**História 2 — Endereço configurável:**

- Dado o endereço base da aplicação externa configurado pela variável de linha de comando de inicialização desta aplicação, quando `GET /api/cross` é processado, então a chamada de saída é feita para `GET {endereço configurado}/api/protected`.

**História 3 — Resultado do acesso cruzado:**

- Dado que a chamada de saída a `GET /api/protected` da aplicação externa retorna sucesso com o `access_token` aceito, quando o chamador executa `GET /api/cross`, então a aplicação responde `200`, confirmando que o acesso cruzado foi bem-sucedido.
- Dado que a chamada de saída a `GET /api/protected` da aplicação externa falha (token rejeitado, aplicação externa inacessível, ou qualquer erro na chamada), quando o chamador executa `GET /api/cross`, então a aplicação responde com um status de erro que reflita a falha da chamada de saída, sem expor o `access_token` nem material de chave privada na resposta.

## Current → new behavior

- Atualmente a `SecurityFilterChain` libera como públicos `GET /oauth2/jwks`, `GET /diagnostics/oauth2-client-assertion`, `GET /api/public`, `GET /actuator/health` e os caminhos do Swagger/OpenAPI, exigindo autenticação para qualquer outro caminho; `GET /api/cross` passa a integrar a lista de caminhos públicos, sem alterar a classificação dos caminhos já liberados ou já protegidos.
- Atualmente nenhum componente desta aplicação usa o `access_token` obtido junto ao Keycloak para chamar outra aplicação — o único consumidor do `OAuth2AuthorizedClientManager` é o endpoint diagnóstico `GET /diagnostics/oauth2-client-assertion`, que confirma a troca mas não chama nenhuma outra aplicação; `GET /api/cross` passa a ser o primeiro uso de negócio desse token para uma chamada de saída real.
- Atualmente não existe nenhuma propriedade de configuração para o endereço de uma aplicação externa; esta spec introduz essa configuração, informada por variável de linha de comando na inicialização.

## Cross-domain dependencies

- **"Cliente OAuth2 com Client Assertion JWT"** — fornece o `access_token` incluído na chamada de saída de `GET /api/cross`, através do `OAuth2AuthorizedClientManager` já configurado; esta spec consome essa capacidade sem alterá-la.
- **Aplicação externa** (sistema chamado por `GET /api/cross`) — expõe `GET /api/protected` e valida o `access_token` apresentado contra o JWKS do Keycloak, fora do controle deste repositório.
- **Keycloak** (sistema externo) — emissor do `access_token` consumido na chamada de saída, papel já coberto pelo domínio "Cliente OAuth2 com Client Assertion JWT".

## Risks and observations

- O mecanismo de obtenção do `access_token` usado na chamada de saída de `GET /api/cross` fica em aberto nesta spec, por indicação explícita do requerente ("depois eu informe como deverá ser feita a geração do token"); essa decisão é necessária antes da fase de plano (`plan.md`) desta unidade.
- O comportamento de `GET /api/cross` quando o endereço da aplicação externa não é informado na inicialização não é definido pelo pedido nem pelas fontes consultadas; fica como ponto em aberto para a fase de plano.
- O contrato de resposta de `GET /api/cross` (corpo específico de sucesso ou erro) recebe o mesmo tratamento já dado a `GET /api/protected` e `GET /api/public` pelo domínio "Servidor de Recursos OAuth2": o material de negócio disponível não fixa um contrato de corpo de resposta, apenas o resultado observável (sucesso ou falha do acesso cruzado).
- A convenção já usada em `GET /diagnostics/oauth2-client-assertion` — responder com um status de erro quando a troca com o Keycloak falha, sem nunca devolver o `access_token` ou material de chave privada no corpo da resposta — é uma referência direta de precedente nesta mesma aplicação para o tratamento de falha da chamada de saída de `GET /api/cross`.
- Esta POC não implementa testes automatizados; a verificação deste fluxo depende de uma aplicação externa real, acessível no endereço configurado, expondo `GET /api/protected`.
