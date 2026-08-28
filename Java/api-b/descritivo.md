Crie uma aplicação Spring Boot 4.x com Java 21 que atue como **OAuth2 Client (com JWT client_assertion) E OAuth2 Resource Server**, expondo um **endpoint JWKS** com sua chave pública RSA para permitir que o Keycloak valide tokens e suporte rotação de chaves.

## Contexto e Arquitetura

### Cenário
- Múltiplas aplicações (servico-a, api-b, etc.) se comunicam via Keycloak
- Cada aplicação gera seu próprio par de chaves RSA
- Cada aplicação expõe seu JWKS público (`/oauth2/jwks`)
- Keycloak consulta os JWKS das aplicações para validar:
  - **client_assertion** (JWT assinado pela private key da aplicação client)
  - **access_tokens** (quando a aplicação é resource server)
- **Rotação de chaves**: ao trocar o par de chaves, o JWKS é atualizado automaticamente

### Fluxo OAuth2 (client_credentials com JWT client_assertion - RFC 7523)

1. **Aplicação (como Client)**:
   - Gera JWT client_assertion assinado com sua **private key**
   - Chama `POST /oauth2/token` do Keycloak com:
     - `grant_type=client_credentials`
     - `client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer`
     - `client_assertion=<JWT assinado>`

2. **Keycloak**:
   - Consulta o JWKS da aplicação (`https://servico-a/oauth2/jwks`)
   - Valida a assinatura do client_assertion com a **public key**
   - Emite **access_token** (assinado pelo Keycloak)

3. **Aplicação (como Client)**:
   - Usa o access_token para chamar outra aplicação (api-b)

4. **api-b (como Resource Server)**:
   - Valida o access_token consultando o JWKS do **Keycloak**
   - Processa a requisição

## Versões

- **Spring Boot**: 4.1.x (ou versão mais recente estável)
- **Spring Framework**: 7.x
- **Spring Security**: 7.x
- **Java**: 21 ou superior
- **Jakarta EE**: 11

## Stack Tecnológico

- Spring Boot 4.1.x
- Java 21
- Spring Security 7.x
- spring-boot-starter-oauth2-client
- spring-boot-starter-oauth2-resource-server
- Nimbus JOSE+JWT (já incluído nos starters)
- Lombok (opcional)

## Funcionalidades Obrigatórias

### 1. Geração e Gestão de Chaves RSA

- Criar um bean `RSAKey` que gera um par de chaves RSA 2048 bits ao iniciar
- A chave deve ter um `keyID` (kid) único
- Algoritmo: RS256
- A **private key** fica armazenada na memória da aplicação (ou em arquivo PKCS12 se configurado)
- A **public key** é exposta via JWKS
- (Opcional) Suporte a carregar par de chaves de arquivo PKCS12 via `@ConfigurationProperties`

### 2. Endpoint JWKS

- Expor endpoint GET `/oauth2/jwks` que retorna um `JWKSet` contendo apenas a **public key**
- O endpoint deve ser **público** (sem autenticação)
- O Keycloak consultará este endpoint para obter a chave pública
- Formato de resposta:
```json
{
  "keys": [
    {
      "kty": "RSA",
      "kid": "jwt-key-id",
      "use": "sig",
      "alg": "RS256",
      "n": "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbf...",
      "e": "AQAB"
    }
  ]
}
```

### 3. OAuth2 Client (JWT client_assertion)

- Configurar OAuth2 Client para usar **JWT client_assertion** (RFC 7523)
- Ao chamar `POST /oauth2/token` do Keycloak:
  - Gerar JWT client_assertion assinado com a **private key**
  - Incluir claims: `iss`, `sub`, `aud`, `exp`, `iat`, `jti`
  - Assinar com algoritmo RS256
- Obter **access_token** do Keycloak
- Usar o access_token para chamar outras APIs

### 4. OAuth2 Resource Server (Validação de JWT)

- Configurar `SecurityFilterChain` para validar JWTs em endpoints protegidos
- Usar o JWKS do **Keycloak** para validar access_tokens recebidos
- Validar claims: `iss`, `exp`, `aud`
- Endpoints de exemplo:
  - `GET /api/protected` → requer token válido
  - `GET /api/public` → sem autenticação
  - `GET /actuator/health` → sem autenticação
  - `GET /oauth2/jwks` → sem autenticação (público)


