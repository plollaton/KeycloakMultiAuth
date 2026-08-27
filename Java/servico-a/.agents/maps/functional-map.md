---
name: functional-map
description: Mapa dos quatro domínios de negócio da POC de autenticação (gestão de chaves/JWKS, cliente OAuth2 com client_assertion, resource server e acesso cruzado a aplicação externa), com dependências, ordem sugerida e o efeito das decisões de documentação (OpenAPI/Swagger) e testes (sem testes automatizados) já incorporado a cada domínio.
metadata:
  author: clovis-cli
  responsibility: Mapa de identificação dos domínios de negócio (bounded contexts), seus limites, dependências e ordem de implementação sugerida. Índice de domínios para geração de skills e para o fluxo spec-driven; não detalha regras de negócio nem duplica as decisões transversais, que vivem no discovery-answers.md.
---

# Domínios identificados

## 1. Gestão de Chaves RSA e Publicação JWKS

- **Objetivo de negócio**: gerar e manter, na própria aplicação, o par de chaves RSA (2048 bits, RS256, `kid` único) usado para assinar o client_assertion, e publicar a chave pública correspondente via endpoint JWKS público, para que o Keycloak consiga validar essa assinatura (e, quando aplicável, access_tokens emitidos com essa chave). Suporta rotação de chaves refletida automaticamente no JWKS.
- **Evidência no material fornecido**: `descritivo.md` linhas 55-83 (Funcionalidades obrigatórias 1 e 2) e linhas 5-12 (cenário/rotação de chaves) — ver índice em [[business-input]].
- **Dependências**: nenhuma (domínio de fundação para os demais).
- **Regras inferidas**:
  - bean de chave RSA gerado na inicialização da aplicação, 2048 bits, algoritmo RS256, com `kid` único;
  - chave privada mantida em memória por padrão; carregamento opcional de um par de chaves de arquivo PKCS12 via `@ConfigurationProperties`;
  - endpoint `GET /oauth2/jwks` público (sem autenticação), retornando um `JWKSet` contendo apenas a chave pública, no formato `kty`/`kid`/`use`/`alg`/`n`/`e`.
- O endpoint `GET /oauth2/jwks` é documentado via OpenAPI/Swagger, junto dos demais endpoints REST do projeto.
- Este domínio não possui testes automatizados nesta POC.
- **Dependências externas relevantes**: Nimbus JOSE+JWT (geração e serialização do par RSA e do `JWKSet`); OpenAPI/Swagger (documentação do endpoint `/oauth2/jwks`).
- **Domain technical dependencies**:
  - Nimbus JOSE+JWT — biblioteca que constrói o `RSAKey`/`JWKSet` e serializa a chave pública no formato esperado pelo Keycloak; sem ela não há como montar o endpoint no contrato exigido.
  - Endpoint HTTP público, fora de qualquer regra de autenticação da `SecurityFilterChain` — sem essa liberação, o Keycloak não consegue obter a chave pública para validar o client_assertion, quebrando todo o fluxo RFC 7523.
  - OpenAPI/Swagger — documenta o contrato do endpoint `/oauth2/jwks`; sem ela o endpoint fica sem descrição formal para quem for integrá-lo.
- **Nível de confiança**: high (evidência direta e literal no descritivo.md, incluindo o formato exato de resposta esperado).

## 2. Cliente OAuth2 com Client Assertion JWT (RFC 7523)

- **Objetivo de negócio**: autenticar a aplicação como client OAuth2 no Keycloak via `client_credentials` com `client_assertion` (JWT assinado com a chave privada própria), obtendo um access_token para uso posterior.
- **Evidência no material fornecido**: `descritivo.md` linhas 14-21 e 28-29 (fluxo OAuth2) e linhas 85-93 (Funcionalidade obrigatória 3) — ver índice em [[business-input]].
- **Dependências**: depende de **Gestão de Chaves RSA e Publicação JWKS** (assina o client_assertion com a chave privada gerida por aquele domínio).
- **Regras inferidas**:
  - geração de um JWT client_assertion com as claims `iss`, `sub`, `aud`, `exp`, `iat`, `jti`, assinado em RS256;
  - chamada `POST /oauth2/token` do Keycloak com `grant_type=client_credentials`, `client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer` e `client_assertion=<JWT assinado>`;
  - o access_token obtido é usado para chamadas subsequentes a outras APIs; essa chamada a uma API específica (cenário `servico-a` → `api-b`) é implementada pelo domínio **Acesso Cruzado a Aplicação Externa**.
- Este domínio não expõe endpoint REST próprio, portanto não entra na documentação OpenAPI/Swagger do projeto; não possui testes automatizados nesta POC.
- **Dependências externas relevantes**: Keycloak (emite o access_token e valida o client_assertion contra o JWKS deste projeto); `spring-boot-starter-oauth2-client`; Nimbus JOSE+JWT.
- **Domain technical dependencies**:
  - Keycloak — expõe o endpoint `/oauth2/token` que recebe o client_assertion e emite o access_token; sem ele não há como testar ou completar o fluxo.
  - Gestão de Chaves RSA e Publicação JWKS — fornece a chave privada necessária para assinar o client_assertion; sem ela este domínio não tem o que assinar.
- **Nível de confiança**: high (fluxo descrito passo a passo no descritivo.md, incluindo claims e parâmetros exatos da requisição).

## 3. Servidor de Recursos OAuth2 (Validação de Access Token)

- **Objetivo de negócio**: proteger endpoints da aplicação validando os access_tokens emitidos pelo Keycloak, liberando sem autenticação um conjunto específico de endpoints (incluindo o próprio JWKS deste projeto).
- **Evidência no material fornecido**: `descritivo.md` linhas 31-33 (papel de resource server no fluxo) e linhas 95-104 (Funcionalidade obrigatória 4) — ver índice em [[business-input]].
- **Dependências**: depende de **Gestão de Chaves RSA e Publicação JWKS** (a mesma `SecurityFilterChain` deste domínio precisa manter `/oauth2/jwks` liberado como público, junto de `/api/public` e `/actuator/health`).
- **Regras inferidas**:
  - `SecurityFilterChain` valida o JWT em endpoints protegidos usando o JWKS do **Keycloak** (não o JWKS próprio da aplicação);
  - validação das claims `iss`, `exp` e `aud` do access_token recebido;
  - `GET /api/protected` exige token válido; `GET /api/public`, `GET /actuator/health` e `GET /oauth2/jwks` são públicos.
- Os endpoints `GET /api/public` e `GET /api/protected` são documentados via OpenAPI/Swagger; `/actuator/health` segue a documentação padrão do Spring Boot Actuator.
- Este domínio não possui testes automatizados nesta POC.
- **Dependências externas relevantes**: Keycloak (publica o JWKS usado para validar os access_tokens recebidos); `spring-boot-starter-oauth2-resource-server`; Spring Boot Actuator (endpoint `/actuator/health`); OpenAPI/Swagger (documentação de `/api/public` e `/api/protected`).
- **Domain technical dependencies**:
  - Keycloak — publica o JWKS usado para validar assinatura e claims dos access_tokens recebidos; sem ele não há chave pública para validar nada.
  - Spring Boot Actuator — expõe `/actuator/health` como endpoint público de exemplo; sem ele esse endpoint de exemplo não existe.
  - Gestão de Chaves RSA e Publicação JWKS — define que `/oauth2/jwks` deve permanecer público dentro da mesma cadeia de segurança deste domínio.
  - OpenAPI/Swagger — documenta o contrato de `/api/public` e `/api/protected`; sem ela esses endpoints ficam sem descrição formal para quem for integrá-los.
- **Nível de confiança**: high (endpoints, papéis de validação e claims descritos explicitamente no descritivo.md).

## 4. Acesso Cruzado a Aplicação Externa

- **Objetivo de negócio**: demonstrar o cenário de acesso cruzado entre aplicações do material de negócio — usando o access_token obtido junto ao Keycloak pelo domínio Cliente OAuth2 com Client Assertion JWT, a própria aplicação chama `GET /api/protected` de uma aplicação externa (cenário `servico-a` → `api-b`), apresentando esse token como cliente daquela API.
- **Evidência no material fornecido**: `descritivo.md` linhas 5-12 e 14-33 (cenário `servico-a` → `api-b`) — ver índice em [[business-input]].
- **Dependências**: depende de **Cliente OAuth2 com Client Assertion JWT** (fornece o access_token via `OAuth2AuthorizedClientManager`) e de **Servidor de Recursos OAuth2** (mantém `GET /api/cross` público na `SecurityFilterChain` que possui).
- **Regras inferidas**:
  - `GET /api/cross` é público: processado sem exigir autenticação do chamador;
  - o endpoint obtém um access_token junto ao Keycloak via `OAuth2AuthorizedClientManager` e chama `GET /api/protected` da aplicação externa configurada na propriedade `app.cross.external-base-url`;
  - chamada aceita: responde `200` com `CrossAccessResponse` (`status: "ok"`); chamada ou obtenção de token falhas: responde `502`, sem expor access_token nem material de chave privada.
- O endpoint `GET /api/cross` é documentado via OpenAPI/Swagger, junto dos demais endpoints REST do projeto.
- Este domínio não possui testes automatizados nesta POC.
- **Dependências externas relevantes**: aplicação externa (cenário `servico-a` → `api-b`, expõe `GET /api/protected` e valida o access_token apresentado, fora do controle deste repositório); `spring-boot-starter-web` (`RestClient`); OpenAPI/Swagger (documentação de `GET /api/cross`).
- **Domain technical dependencies**:
  - Cliente OAuth2 com Client Assertion JWT — fornece o `OAuth2AuthorizedClientManager` já configurado, usado para obter o access_token apresentado na chamada de saída; sem ele este domínio não tem como se autenticar perante a aplicação externa.
  - Servidor de Recursos OAuth2 — mantém `GET /api/cross` público na `SecurityFilterChain` que possui; sem essa liberação o endpoint ficaria inacessível sem um access_token do próprio chamador.
  - `spring-boot-starter-web` (`RestClient`) — fornece o cliente HTTP usado na chamada de saída; sem ele não há como montar essa chamada na convenção de stack fixada para o projeto.
  - OpenAPI/Swagger — documenta o contrato de `GET /api/cross`; sem ela o endpoint fica sem descrição formal para quem for integrá-lo.
- **Nível de confiança**: high (endpoint, propriedade de configuração e comportamento de sucesso/falha descritos explicitamente no código e na spec fundadora deste domínio).
