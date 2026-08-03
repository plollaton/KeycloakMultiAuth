---
name: functional-map
description: Mapa dos bounded contexts do App B — gestão do JWKS local, autenticação/validação offline do JWT e recurso protegido demonstrativo (documentado via OpenAPI/springdoc) — com dependências, dependências técnicas e ordem sugerida; sem gaps abertos.
metadata:
  author: clovis-cli
  responsibility: "Map of identification of the business domains (bounded contexts), their boundaries, dependencies and suggested implementation order. Index of domains for skill generation and the spec-driven flow; it does not detail business rules nor duplicate the cross-cutting decisions, which live in the discovery-answers.md."
---

# Mapa funcional — App B (Resource Server)

O App B é um resource server cuja razão de existir é **autenticar requisições validando um
JWT do Keycloak de forma offline**. Como se trata de uma POC de autenticação, os bounded
contexts giram em torno do próprio mecanismo de segurança (material de chaves, validação do
token e o recurso protegido que serve de prova). Não há domínio de negócio financeiro
próprio implementado — o `/api/protegido` é um recurso demonstrativo.

---

## Domínio: Gestão do JWKS local

- **Slug:** `gestao-jwks-local`
- **Objetivo de negócio:** manter disponível localmente o conjunto de chaves públicas (JWKS)
  do Keycloak usado para validar assinaturas de token, recarregando-o automaticamente quando
  o arquivo muda e expondo há quanto tempo foi carregado, para permitir monitoração de
  "staleness" (chave desatualizada).
- **Evidência no código:**
  - `src/main/java/com/dbfinanceira/appb/security/LocalFileJWKSource.java` — leitura do
    arquivo, cache volátil, hot-reload por `mtime` com `ReentrantLock` (double-check),
    fail-fast na subida, `getLastLoadedAt()`.
  - `src/main/resources/application.yml` — propriedade `app.security.jwt.jwks-path`
    (`APP_B_JWKS_PATH`, padrão `/etc/app-b/jwks/jwks.json`).
- **Evidência no material fornecido:** `README.md` do diretório-pai, seção "Sincronizando o
  JWKS local"; script `sync-jwks.sh` (diretório-pai) que busca
  `GET {issuer}/protocol/openid-connect/certs` e grava o arquivo atomicamente.
- **Dependências de outros domínios:** nenhuma (é o domínio-base do qual a autenticação
  depende).
- **Regras inferidas (nível de identificação):**
  - O JWKS vem de **arquivo local**, nunca de chamada HTTP ao Keycloak em tempo de
    requisição.
  - Recarga automática quando o `last modified time` do arquivo muda (não requer reiniciar
    a aplicação após rotação de chave).
  - **Fail-fast** na subida se o arquivo não existir ou for inválido; em falha de checagem
    posterior, mantém a última versão válida em memória.
  - `getLastLoadedAt()` existe para virar um health indicator de "JWKS velho" — indicator
    **ainda não implementado** (melhoria conhecida; ver `discovery-answers.md`).
- **Domain technical dependencies:**
  - **`sync-jwks.sh` (processo externo de sincronização)** — popula/atualiza o arquivo JWKS
    lido por este domínio. Se parar e o Keycloak rotacionar a chave de assinatura, tokens
    novos (com `kid` desconhecido) passam a ser rejeitados mesmo sendo válidos.
  - **Keycloak (endpoint `/protocol/openid-connect/certs`)** — fonte original das chaves
    públicas; consumido pelo `sync-jwks.sh`, não pelo App B diretamente.
  - **Nimbus JOSE (`com.nimbusds.jose.jwk.JWKSet`)** — faz o parse/carga do arquivo JWKS;
    sem ela não há como interpretar o material de chaves.
  - **Sistema de arquivos / volume montado em `APP_B_JWKS_PATH`** — a pasta específica
    precisa existir e ser legível; é o contrato de integração com o processo de sync.
- **Nível de confiança:** `high` para o comportamento implementado; `medium` para tratá-lo
  como bounded context próprio em vez de subdomínio de suporte de `autenticacao-token-jwt`
  (a fronteira é justificável pelo ciclo de vida operacional distinto — sync externo,
  hot-reload, staleness).

---

## Domínio: Autenticação e validação offline do token JWT

- **Slug:** `autenticacao-token-jwt`
- **Objetivo de negócio:** garantir que toda requisição a rotas protegidas apresente um
  access token (JWT) Bearer legítimo emitido pelo Keycloak, validando **offline** sua
  assinatura, emissor, audience e validade temporal antes de permitir o acesso.
- **Evidência no código:**
  - `src/main/java/com/dbfinanceira/appb/config/JwtDecoderConfig.java` — monta o
    `NimbusJwtDecoder` sobre o JWKS local e encadeia `JwtTimestampValidator`,
    `JwtIssuerValidator` e `JwtClaimValidator` (aud) via `DelegatingOAuth2TokenValidator`.
  - `src/main/java/com/dbfinanceira/appb/config/SecurityConfig.java` — filter chain
    stateless, CSRF desabilitado, `oauth2ResourceServer().jwt(...)`, `/actuator/health/**`
    liberado e `anyRequest().authenticated()`.
  - `src/main/java/com/dbfinanceira/appb/config/AppBSecurityProperties.java` e
    `application.yml` — `issuer`, `audience`, `clock-skew-seconds`.
- **Dependências de outros domínios:** `gestao-jwks-local` (fonte das chaves públicas usadas
  para verificar a assinatura).
- **Regras inferidas (nível de identificação):**
  - Valida `iss` = realm do Keycloak configurado.
  - Valida que `aud` **contém** a audience esperada (`app-b`) — depende de Audience Mapper
    no Keycloak; sem ele o token só traz `aud: account` e toda chamada é rejeitada.
  - Valida `exp`/`nbf`/`iat` com tolerância de relógio configurável (padrão 60s).
  - API **stateless**, autenticada por Bearer token, **sem** sessão; CSRF desabilitado de
    propósito (ver `discovery-answers.md`).
  - Definir o bean `JwtDecoder` desativa a auto-config do resource server — a propriedade
    `jwk-set-uri` **não** deve ser usada.
- **Domain technical dependencies:**
  - **Keycloak** — emissor dos tokens; determina os valores esperados de `iss` e `aud`.
    Sem alinhamento com o realm/Audience Mapper, nenhuma requisição autentica.
  - **Spring Security OAuth2 Resource Server** — provê o pipeline de validação de JWT e o
    `oauth2ResourceServer`.
  - **Nimbus JOSE (`NimbusJwtDecoder`, `JWKSource`)** — executa a verificação
    criptográfica da assinatura.
  - **`gestao-jwks-local` (`LocalFileJWKSource`)** — sem a fonte de chaves local, o decoder
    não consegue verificar assinaturas.
  - **`AppBSecurityProperties` / variáveis de ambiente** — `iss`, `aud`, clock skew e caminho
    do JWKS; erro de configuração aqui bloqueia toda autenticação.
- **Nível de confiança:** `high`.

---

## Domínio: Recurso protegido (API demonstrativa)

- **Slug:** `recurso-protegido`
- **Objetivo de negócio:** expor o recurso de negócio protegido acessível somente com token
  válido e, como prova da autenticação offline, devolver os metadados não sensíveis do token
  validado (subject, clientId, issuer, audience, scope, expiração).
- **Evidência no código:**
  - `src/main/java/com/dbfinanceira/appb/web/SecureController.java` — endpoint
    `GET /api/protegido` que lê o `JwtAuthenticationToken` e retorna claims selecionados.
- **Dependências de outros domínios:** `autenticacao-token-jwt` (só é alcançável depois que
  a requisição foi autenticada com sucesso).
- **Regras inferidas (nível de identificação):**
  - `GET /api/protegido` exige autenticação (cai em `anyRequest().authenticated()`).
  - Retorna `mensagem`, `subject`, `clientId` (`azp`), `issuer`, `audience`, `scope`,
    `expiresAt` — todos metadados não sensíveis do token (coerente com a regra de nunca
    logar/expor o JWT bruto).
  - Em uma POC, é um endpoint demonstrativo; em um sistema real representaria o(s)
    verdadeiro(s) recurso(s) de negócio protegido(s).
  - **Documentação de API via Swagger/OpenAPI (decidido):** os endpoints deste domínio
    (`/api/protegido` e o `/actuator/health`) devem ser descritos por documentação
    OpenAPI/Swagger. Ver o `technical-skill` `documentacao-api-openapi`.
- **Domain technical dependencies:**
  - **`autenticacao-token-jwt`** — o controller só recebe um `JwtAuthenticationToken` porque
    o filter chain já validou o token; sem o domínio de autenticação, o endpoint fica
    inacessível/aberto.
  - **Spring Web (`spring-boot-starter-web`, Tomcat)** — expõe o endpoint REST.
  - **`springdoc-openapi` (a adicionar)** — dependência ainda ausente no `pom.xml`; será
    incluída para gerar/servir a documentação OpenAPI/Swagger deste endpoint. Ver
    `discovery-answers.md` e o technical-skill `documentacao-api-openapi`.
- **Nível de confiança:** `high`.

---

## Dependências entre domínios (resumo)

```
gestao-jwks-local  ──▶  autenticacao-token-jwt  ──▶  recurso-protegido
   (fornece chaves)        (valida o token)            (consome o acesso)
```

## Ordem sugerida (manutenção/entendimento)

1. `gestao-jwks-local` (ordem 0) — base: material de chaves e sua atualização.
2. `autenticacao-token-jwt` (ordem 1) — depende do JWKS para verificar assinatura.
3. `recurso-protegido` (ordem 2) — depende da autenticação bem-sucedida.

## Gaps abertos

_(Nenhum gap aberto — documentação de API resolvida como Swagger/OpenAPI e testes
automatizados despriorizados nesta POC; ambas registradas em `discovery-answers.md`.)_

_Não há decisão pendente de fusão/divisão de domínios além da confiança `medium` já
registrada para `gestao-jwks-local` (mantido como domínio próprio nesta rodada)._
