---
name: discovery-answers
description: Memória durável da descoberta do App B (resource server que valida JWT do Keycloak offline via JWKS local); registra objetivo, escopo, restrições, decisões transversais e o log de gaps resolvidos (documentação via OpenAPI/springdoc e sem testes automatizados nesta POC).
metadata:
  author: clovis-cli
  responsibility: "Durable memory of the functional-discovery context and decisions: objective, scope, declared restrictions, validated cross-cutting decisions (with destination), documentation forms to maintain and the log of human decisions resolved in the gap loop. Source re-read by the following stages; its restrictions take precedence over later inferences."
---

# Memória de descoberta — App B (Resource Server)

## Objetivo

Documentar e dar manutenção ao **App B**, um resource server Spring Boot que recebe
requisições autenticadas por `Authorization: Bearer <JWT>`, sendo o token um access token
emitido pelo **Keycloak** (via `client_credentials` pela App A). O App B **valida o token
de forma offline** — verifica assinatura, `iss`, `aud` e validade temporal usando um JWKS
(chaves públicas do Keycloak) lido de um **arquivo local**, sem consultar o Keycloak a cada
requisição. O propósito da POC é **apresentar/demonstrar o fluxo de autenticação** ponta a
ponta.

## Escopo

- Tipo de escopo: **projeto inteiro** (`whole_project`) — apenas o módulo
  `app-b-resource-server`.
- A App A (`app-a-client`, projeto irmão) e o próprio Keycloak estão **fora do escopo** de
  manutenção; são citados apenas como integrações externas.
- O script `sync-jwks.sh` mora no diretório-pai (fora da árvore do App B), mas é parte
  essencial do fluxo operacional do domínio de JWKS e por isso é referenciado como
  dependência técnica externa.
- Modo de trabalho: **manutenção** (`maintenance`) — o código já existe e deve ser
  preservado; não é reescrita.

## Restrições declaradas pelo usuário

- **Apresentar autenticação**: a POC existe para demonstrar o fluxo de autenticação
  (validação offline do JWT). Essa é a finalidade central e orienta a priorização dos
  domínios: o coração do sistema é a autenticação, não uma regra de negócio própria.

## Restrições e decisões técnicas herdadas do código/contexto (a preservar)

Estas são decisões transversais já **validadas por evidência no código** ou na documentação
do projeto (README do diretório-pai). Não são gaps — devem ser preservadas na manutenção.

1. **Autenticação offline via JWKS de arquivo local.**
   Em vez de `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` (busca HTTP no Keycloak),
   o `JwtDecoder` é construído manualmente a partir de um JWKS lido de arquivo local
   (`LocalFileJWKSource`). Motivação declarada: o App B roda em segmento de rede sem saída
   direta para o Keycloak.
   **Destino:** regra central dos domínios `gestao-jwks-local` e `autenticacao-token-jwt`.

2. **API stateless, autenticada por Bearer token, com CSRF desabilitado de propósito.**
   `SessionCreationPolicy.STATELESS`; CSRF desabilitado explicitamente porque o Spring
   Security 7 passou a habilitá-lo por padrão também para APIs; `/actuator/health/**` é
   `permitAll`, todo o restante exige autenticação.
   **Destino:** convenção em `AGENTS.md`.

3. **Validação de claims: `iss`, `aud` e timestamps com clock skew configurável.**
   `JwtIssuerValidator` (iss = realm do Keycloak), `JwtClaimValidator` sobre `aud` (deve
   conter a audience esperada — exige Audience Mapper no Keycloak), `JwtTimestampValidator`
   com tolerância de relógio (`clock-skew-seconds`, padrão 60s).
   **Destino:** domínio `autenticacao-token-jwt`.

4. **Configuração externalizada por variáveis de ambiente.**
   `KEYCLOAK_ISSUER_URI`, `APP_B_EXPECTED_AUDIENCE`, `APP_B_JWKS_PATH`,
   `APP_B_CLOCK_SKEW_SECONDS`, mapeadas em `AppBSecurityProperties`
   (`@ConfigurationProperties(prefix = "app.security.jwt")`).
   **Destino:** convenção em `AGENTS.md`.

5. **Convenções de segurança para instituição financeira (checklist do README).**
   Nunca logar o JWT bruto (apenas metadados não sensíveis como `sub`, `azp`, `jti`,
   timestamps); TLS obrigatório em todos os saltos; expiração curta do access token;
   `client_secret` em cofre de segredos. Parte desse checklist é operacional/lado-Keycloak
   (fora do App B), mas a regra "não logar token bruto" aplica-se diretamente ao App B.
   **Destino:** convenção em `AGENTS.md`.

6. **Observabilidade mínima via Actuator.**
   Apenas o endpoint `health` está exposto (`management.endpoints.web.exposure.include:
   health`); log do Spring Security em `INFO`. `LocalFileJWKSource.getLastLoadedAt()` foi
   **intencionalmente exposto** para virar um health indicator de "JWKS desatualizado",
   mas esse indicator **ainda não foi implementado/conectado** ao Actuator (melhoria
   recomendada no README).
   **Destino:** regra inferida do domínio `gestao-jwks-local`; a implementação do health
   indicator é uma melhoria conhecida (ver gap de observabilidade, se elevado no futuro).

7. **Sem testes automatizados nesta POC (decidido no loop de gaps).**
   As dependências `spring-boot-starter-test` e `spring-security-test` permanecem no
   `pom.xml`, mas **não** se prioriza escrever testes automatizados nesta POC. Não gera
   `technical-skill` (é regra curta e estável).
   **Destino:** convenção em `AGENTS.md`.

## Stack técnica (evidência no código)

- Java 21, Spring Boot **4.0.6** (Spring Framework 7 / Spring Security 7), Maven.
- `spring-boot-starter-web` (Tomcat, padrão — Undertow removido no Boot 4),
  `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`
  (traz Nimbus JOSE `com.nimbusds` transitivamente), `spring-boot-starter-actuator`.
- Dependências de teste presentes (`spring-boot-starter-test`, `spring-security-test`),
  **porém sem nenhum teste escrito** (`src/test` inexistente) — ver gap de testes.

## Formas de documentação a manter

- **Documentação existente:** `README.md` no diretório-pai descreve o fluxo completo,
  configuração do Keycloak, variáveis de ambiente e checklist de segurança.
- **Documentação de API — Swagger/OpenAPI (decidido).** O App B passa a manter documentação
  de API via **Swagger/OpenAPI** (ex.: `springdoc-openapi`). Hoje não há nenhuma dependência
  springdoc no `pom.xml` — deve ser adicionada — e os endpoints `/api/protegido` e
  `/actuator/health` passam a ser descritos por essa documentação.
  **Destino:** `technical-skill` `documentacao-api-openapi` (how-to reutilizável de como
  manter/gerar a doc OpenAPI). Afeta o domínio `recurso-protegido`.
- **Postman / ADRs:** não adotados.

## Framework spec-driven concorrente

Investigado: **nenhum** framework concorrente (Spec Kit, OpenSpec ou similar) foi
encontrado. Existem apenas `.clovis/` (estado do próprio fluxo Clovis) e os artefatos
`.agents/` gerados por este fluxo. Sem conflito de fonte da verdade.

## Log de decisões humanas resolvidas no loop de gaps

- **`doc-forms`** (transversal) — _Gap:_ nenhuma forma de documentação de API adotada no App
  B (sem springdoc/OpenAPI, sem collection Postman). _Decisão humana:_ **manter Swagger/
  OpenAPI** (ex.: `springdoc-openapi`). _Efeito:_ vira o `technical-skill`
  `documentacao-api-openapi`; adicionar dependência springdoc; documentar `/api/protegido` e
  `/actuator/health`. Reflete no domínio `recurso-protegido`.
- **`testing-strategy`** (transversal) — _Gap:_ dependências de teste presentes no `pom.xml`
  mas nenhum teste escrito. _Decisão humana:_ **não priorizar testes automatizados nesta
  POC**. _Efeito:_ regra curta e estável → convenção em `AGENTS.md`; não vira technical-skill.

## Gaps abertos (aguardando validação humana)

_(Nenhum gap aberto — todos os gaps desta descoberta foram resolvidos no loop acima.)_

> As restrições e decisões registradas aqui têm precedência sobre inferências posteriores
> das próximas etapas. Domínios, fronteiras e dependências vivem no `functional-map.md`.
