---
name: functional-map
description: Mapa dos dois domínios de negócio da POC de autenticação (gestão de chaves/JWKS e resource server), com dependências, ordem sugerida e o efeito das decisões de documentação (OpenAPI/Swagger) e testes (sem testes automatizados) já incorporado a cada domínio.
metadata:
  author: clovis-cli
  responsibility: Mapa de identificação dos domínios de negócio (bounded contexts), seus limites, dependências e ordem de implementação sugerida. Índice de domínios para geração de skills e para o fluxo spec-driven; não detalha regras de negócio nem duplica as decisões transversais, que vivem no discovery-answers.md.
---

# Domínios identificados

## 1. Gestão de Chaves RSA e Publicação JWKS

- **Objetivo de negócio**: gerar e manter, na própria aplicação, o par de chaves RSA (2048 bits, RS256, `kid` único), e publicar a chave pública correspondente via endpoint JWKS público (`GET /oauth2/jwks`), para que consumidores externos possam validar assinaturas produzidas pela aplicação. Suporta rotação de chaves refletida automaticamente no JWKS.
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
  - Endpoint HTTP público, fora de qualquer regra de autenticação da `SecurityFilterChain` — sem essa liberação, a chave pública não fica acessível a quem precisar validar assinaturas produzidas pela aplicação.
  - OpenAPI/Swagger — documenta o contrato do endpoint `/oauth2/jwks`; sem ela o endpoint fica sem descrição formal para quem for integrá-lo.
- **Nível de confiança**: high (evidência direta e literal no descritivo.md, incluindo o formato exato de resposta esperado).

## 2. Servidor de Recursos OAuth2 (Validação de Access Token)

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
