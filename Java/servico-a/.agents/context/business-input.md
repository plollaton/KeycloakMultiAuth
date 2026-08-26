---
name: business-input
description: Registro do material de negócio bruto fornecido pelo usuário (arquivo descritivo.md) e índice de proveniência/reacesso das regras de negócio da POC de autenticação.
metadata:
  author: clovis-cli
  responsibility: Registro fiel do material de negócio fornecido pelo usuário e um índice de proveniência e reacesso desse material, organizado por fonte. O Estágio 2 reabre as fontes originais a partir daqui; ele não classifica domínios, limites, dependências ou nível de confiança.
---

# Material de negócio fornecido

## Entrada do usuário no campo de regras de negócio

O usuário informou, no campo de regras de negócio da sessão, o caminho de arquivo local:

```
./descritivo.md
```

Nenhum outro texto livre, URL ou espaço de MCP foi fornecido nesta rodada.

## Índice de fontes consultadas

### Fonte: `descritivo.md` (arquivo local, raiz do projeto `aplicacao-segura`)

Caminho de reacesso: `D:\Estudos\KeycloakMultiAuth\Java\aplicacao-segura\descritivo.md` (arquivo texto Markdown, leitura direta).

| Tema | Localização no arquivo | Conteúdo (resumo, não é cópia literal) |
|---|---|---|
| Objetivo geral da aplicação | linha 1 | Aplicação Spring Boot 4.x/Java 21 que atua como OAuth2 Client (com client_assertion JWT) e OAuth2 Resource Server, expondo endpoint JWKS próprio. |
| Cenário / arquitetura multi-aplicação | linhas 5-12 | Múltiplas aplicações (servico-a, api-b etc.) trocando tokens via Keycloak; cada uma gera seu par de chaves RSA e expõe seu próprio JWKS; rotação de chaves deve refletir automaticamente no JWKS. |
| Fluxo OAuth2 client_credentials com client_assertion (RFC 7523) | linhas 14-33 | Passo a passo: (1) app gera client_assertion assinado com a private key e chama `POST /oauth2/token` do Keycloak; (2) Keycloak consulta o JWKS da app para validar o client_assertion e emite o access_token; (3) app usa o access_token para chamar outra aplicação (api-b); (4) api-b, como resource server, valida o access_token contra o JWKS do Keycloak. |
| Versões / stack alvo | linhas 35-51 | Spring Boot 4.1.x, Spring Framework 7.x, Spring Security 7.x, Java 21+, Jakarta EE 11, `spring-boot-starter-oauth2-client`, `spring-boot-starter-oauth2-resource-server`, Nimbus JOSE+JWT (via starters), Lombok (opcional). |
| Funcionalidade obrigatória 1 — Geração e gestão de chaves RSA | linhas 55-62 | Bean `RSAKey` gerado na inicialização (2048 bits, `kid` único, algoritmo RS256); private key em memória por padrão ou em arquivo PKCS12 se configurado; public key exposta via JWKS; suporte opcional a carregar o par de chaves de PKCS12 via `@ConfigurationProperties`. |
| Funcionalidade obrigatória 2 — Endpoint JWKS | linhas 64-83 | `GET /oauth2/jwks` público (sem autenticação), retornando um `JWKSet` somente com a chave pública; formato de resposta detalhado (`kty`, `kid`, `use`, `alg`, `n`, `e`). |
| Funcionalidade obrigatória 3 — OAuth2 Client (client_assertion) | linhas 85-93 | Configurar OAuth2 Client para RFC 7523; gerar client_assertion com claims `iss`, `sub`, `aud`, `exp`, `iat`, `jti`, assinado em RS256; obter access_token do Keycloak; usar o access_token para chamar outras APIs. |
| Funcionalidade obrigatória 4 — OAuth2 Resource Server | linhas 95-104 | `SecurityFilterChain` validando JWTs em endpoints protegidos, usando o JWKS do Keycloak; validação das claims `iss`, `exp`, `aud`; endpoints de exemplo: `GET /api/protected` (autenticado), `GET /api/public`, `GET /actuator/health` e `GET /oauth2/jwks` (todos públicos). |

## Fontes citadas mas não fornecidas como material de leitura

Nenhuma. O único material de negócio indicado pelo usuário foi o arquivo `descritivo.md`, que foi lido integralmente.
