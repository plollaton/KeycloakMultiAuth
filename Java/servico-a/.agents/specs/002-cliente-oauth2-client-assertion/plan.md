# Plan: Cliente OAuth2 com Client Assertion JWT

## Stack and structure

Aplicação Spring Boot 4.1.x / Java 21 / Jakarta EE 11, conforme `AGENTS.md`. O scaffold Maven e o
layout `src/main/java/com/aplicacaosegura/...` já existem, criados pelo domínio "Gestão de Chaves
RSA e Publicação JWKS". Este domínio segue a convenção de pacote por domínio de negócio já
registrada no `research.md` daquele domínio, recebendo o pacote próprio
`com.aplicacaosegura.oauth2client` (configuração do client OAuth2 e do resolver de chave usada
para assinar o `client_assertion`).

Nova dependência Maven: `spring-boot-starter-oauth2-client`, que o `research.md` do domínio 1
já registrava como explicitamente pendente para quando o domínio que a configura fosse
implementado — este é esse domínio. A dependência direta `com.nimbusds:nimbus-jose-jwt`, trazida
pelo domínio 1, é mantida como está: `RsaKeyPairConfig` já a referencia diretamente, e o starter
adicionado agora não substitui esse uso. Detalhes em
[`research.md`](./research.md#dependencia-direta-nimbus-jose-jwt-apos-o-starter-oauth2-client).

## Technical decisions

- **Autenticação do client via `private_key_jwt`**: usa-se o suporte nativo do
  `spring-boot-starter-oauth2-client` para `ClientAuthenticationMethod.PRIVATE_KEY_JWT`
  (RFC 7523), combinando um `ClientRegistration` com esse método de autenticação a um
  `NimbusJwtClientAuthenticationParametersConverter` registrado no token response client do grant
  `client_credentials`. Esse converter monta e assina o `client_assertion` (claims `iss`, `sub`,
  `aud`, `exp`, `iat`, `jti`) a partir do `ClientRegistration`, sem código manual de montagem de
  JWT nesta unidade. Alternativas e confirmação em
  [`research.md`](./research.md#mecanismo-de-autenticacao-do-client-junto-ao-keycloak).
- **Origem da chave de assinatura**: o resolver de JWK do converter (`Function<ClientRegistration,
  JWK>`) retorna diretamente o bean `RSAKey activeRsaKey()` já publicado por `RsaKeyPairConfig`
  (domínio "Gestão de Chaves RSA e Publicação JWKS") — nenhuma leitura adicional de
  `api-a-private.pem`/`api-a-cert.pem` é feita por este domínio. Detalhes em
  [`research.md`](./research.md#origem-da-chave-usada-para-assinar-o-client_assertion).
- **Registro do client (`ClientRegistrationRepository`)**: configurado via propriedades em
  `application.yml`, sob um id de registration (`keycloak`) com `authorization-grant-type:
  client_credentials` e `client-authentication-method: private_key_jwt`. `client-id` e o
  `token-uri` (ou `issuer-uri`) do provider ficam como propriedades a preencher por ambiente —
  a skill de domínio já registra que esses valores concretos são específicos da implantação e não
  são fixados pelo material de negócio; este plano não os fixa, apenas define onde vivem
  (`spring.security.oauth2.client.registration.keycloak` e
  `spring.security.oauth2.client.provider.keycloak`).
- **Disponibilização do `access_token` obtido**: um bean `OAuth2AuthorizedClientManager`
  (`AuthorizedClientServiceOAuth2AuthorizedClientManager` com um
  `ClientCredentialsOAuth2AuthorizedClientProvider` que usa o token response client configurado
  acima) é exposto para que qualquer componente futuro desta aplicação obtenha um `access_token`
  válido pelo id da registration, sem reimplementar a troca nem o cache. A implementação da
  chamada que consome esse `access_token` (ex.: api-b) permanece fora do escopo desta unidade,
  conforme o `spec.md`. Alternativas em
  [`research.md`](./research.md#disponibilizacao-do-access_token-para-uso-futuro).
- **Endpoint de diagnóstico**: um `@RestController` no pacote `com.aplicacaosegura.oauth2client`
  expõe `GET /diagnostics/oauth2-client-assertion`, que solicita um `access_token` ao
  `OAuth2AuthorizedClientManager` pela registration `keycloak` e responde `200` com um corpo
  `{"status": "ok"}` quando a troca é bem-sucedida, ou `502` quando o Keycloak rejeita a troca —
  nunca inclui o `access_token` no corpo. É o único ponto de entrada deste domínio, existindo
  exclusivamente para permitir a validação manual do fluxo, já que a chamada consumidora do
  `access_token` está fora do escopo desta unidade. A `SecurityFilterChain` criada pelo domínio
  "Gestão de Chaves RSA e Publicação JWKS" passa a liberar também esse caminho com `permitAll()`,
  para que o endpoint funcione antes da configuração do domínio "Servidor de Recursos OAuth2". O
  controller recebe `@Tag`, `@Operation` e `@ApiResponse` do springdoc-openapi, seguindo o padrão
  da skill técnica `documentacao-api-openapi`.

## Data model

Não se aplica: nenhuma entidade é persistida por este domínio. O `access_token` obtido é mantido
em memória pelo `OAuth2AuthorizedClientService` padrão do Spring Security (mesmo padrão de
não-persistência já adotado pelo par de chaves RSA do domínio 1). `data-model.md` dispensado.

## External contracts

O contrato de `POST /oauth2/token` é do Keycloak, não desta aplicação — não há contrato para
documentar ou versionar neste repositório sobre ele. O contrato do endpoint próprio
`GET /diagnostics/oauth2-client-assertion` já está totalmente especificado nos critérios de
aceite do `spec.md` (`200` com `{"status": "ok"}`, `502` na falha, sem o `access_token` no corpo).
Como no endpoint `GET /oauth2/jwks` do domínio 1, a especificação OpenAPI é gerada em tempo de
execução pelo springdoc-openapi a partir das anotações do controller — pasta `contracts/`
dispensada pelo mesmo motivo já registrado no plano daquele domínio: duplicar o mesmo contrato em
um arquivo mantido à mão não reduziria ambiguidade adicional para as tasks.

## Interface

Não se aplica — domínio puramente backend, sem interface própria. `ui/` dispensada.

## Testing strategy

Esta POC não implementa testes automatizados (unitários, integração ou e2e), conforme decisão
registrada em `.agents/context/discovery-answers.md`. Nenhuma infraestrutura de teste é introduzida
por esta unidade; a verificação do fluxo depende de um Keycloak real configurado com o client desta
aplicação em autenticação via JWT assinado, conforme já registrado nos riscos do `spec.md`.

## Impact on the authoritative documentation

A skill `cliente-oauth2-client-assertion` já descreve corretamente o comportamento de negócio que
este plano implementa (montagem do `client_assertion` com as claims `iss`/`sub`/`aud`/`exp`/`iat`/
`jti`, assinatura em RS256 com a chave do domínio 1, troca via `POST /oauth2/token` com os
parâmetros da regra 3). Ela também afirma que o domínio "não expõe nenhum endpoint REST próprio" e
que, por isso, "não integra a documentação OpenAPI/Swagger do projeto" — afirmação que este plano
diverge deliberadamente ao introduzir `GET /diagnostics/oauth2-client-assertion`, decisão humana
registrada em resposta ao gap `mecanismo-disparo-validacao-manual-client-assertion` (necessária
porque o domínio não tinha, até então, nenhum ponto de entrada observável para validação manual).
Isso gera uma task na fase `tasks` para atualizar a skill, registrando a existência desse endpoint
diagnóstico — deixando claro que ele não é uma funcionalidade de negócio — e sua integração à
documentação OpenAPI/Swagger do projeto.
