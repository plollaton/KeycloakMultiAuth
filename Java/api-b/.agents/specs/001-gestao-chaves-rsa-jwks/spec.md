# Spec: Gestão de Chaves RSA e Publicação JWKS

## Overview

Manutenção do par de chaves RSA próprio da aplicação, fixo e carregado dos arquivos `api-a-private.pem`/`api-a-cert.pem`, e publicação pública da chave pública ativa em formato JWK Set, para que o Keycloak valide assinaturas produzidas com a chave privada da aplicação, com rotação de chaves refletida automaticamente quando esses arquivos são substituídos.

## Domain

- Slug: `gestao-chaves-rsa-jwks`
- Skill: [`.agents/skills/gestao-chaves-rsa-jwks/SKILL.md`](../../skills/gestao-chaves-rsa-jwks/SKILL.md)

## Scope

**In:**

- Carregamento, na inicialização da aplicação, do par de chaves RSA fixo mantido nos arquivos `api-a-private.pem` (chave privada) e `api-a-cert.pem` (certificado com a chave pública correspondente) em `src/main/resources`, com identificador (`kid`) único derivado do número de série do certificado.
- Publicação pública (sem autenticação) da chave pública ativa em formato JWK Set.
- Reflexo automático de rotação de chaves na publicação, sem exigir sincronização manual adicional.
- Documentação do endpoint de publicação via OpenAPI/Swagger.

**Out:**

- Validação de `access_tokens` e proteção dos endpoints `GET /api/protected`, `GET /api/public` e `GET /actuator/health` — domínio "Servidor de Recursos OAuth2 (Validação de Access Token)".
- Chamadas desta aplicação a outras APIs do cenário (ex.: api-b) — fora do escopo das funcionalidades obrigatórias do material de negócio.
- Persistência, histórico ou publicação de chaves de rotações anteriores — o JWKS desta POC expõe apenas a chave pública ativa.

## Domain boundary

**This spec implements:**

- Componente que mantém o par de chaves RSA ativo da aplicação (2048 bits, RS256), carregado na inicialização do par fixo de arquivos `api-a-private.pem` e `api-a-cert.pem` em `src/main/resources`, com `kid` derivado do número de série do certificado.
- Endpoint `GET /oauth2/jwks`, público, retornando um `JWKSet` com a chave pública ativa nos campos `kty`/`kid`/`use`/`alg`/`n`/`e`.
- Anotações OpenAPI/Swagger do endpoint `GET /oauth2/jwks`, conforme a skill técnica `documentacao-api-openapi`.
- Scaffold Maven do projeto (`pom.xml`, layout `src/main/java`/`src/main/resources`, classe de aplicação Spring Boot) e a dependência `springdoc-openapi-starter-webmvc-ui` — infraestrutura transversal que este domínio cria por ser o primeiro a precisar dela, já que o repositório ainda não contém nenhum scaffold de build nem código-fonte.
- `SecurityFilterChain` mínima liberando de autenticação `GET /oauth2/jwks`, `GET /v3/api-docs/**`, `GET /swagger-ui/**` e `GET /swagger-ui.html` — infraestrutura transversal de segurança que este domínio cria por ser o primeiro a precisar dela; a regra de negócio que exige `GET /oauth2/jwks` público é deste domínio (regra 6 da skill), mas a definição de quais demais endpoints exigem ou não autenticação pertence ao domínio "Servidor de Recursos OAuth2".

**Belongs to other domains (cross-domain, does not become a task here):**

- Validação de `access_tokens` contra o JWKS do Keycloak e as regras de proteção de `GET /api/protected`, `GET /api/public` e `GET /actuator/health` na `SecurityFilterChain` → domínio "Servidor de Recursos OAuth2 (Validação de Access Token)".

## User stories

1. Como Keycloak (consumidor externo), quero obter a chave pública ativa da aplicação em formato JWK Set via um endpoint HTTP sem autenticação, para validar assinaturas produzidas pela aplicação.
2. Como Keycloak (consumidor externo), quero que a publicação da chave pública reflita automaticamente o par de chaves atualmente mantido pela aplicação, para continuar validando assinaturas corretamente após uma rotação de chaves, sem depender de um passo manual de republicação.
3. Como responsável pela operação da aplicação, quero que o par de chaves ativo seja sempre o par fixo mantido nos arquivos da aplicação, para ter uma chave determinística e estável entre reinicializações.
4. Como integrador que consome o contrato desta aplicação, quero consultar a documentação OpenAPI/Swagger do endpoint `GET /oauth2/jwks`, para conhecer o formato de resposta sem precisar ler o código-fonte.

## Acceptance criteria

**Story 1 — Publicação pública da chave pública:**

- Dado que a aplicação está no ar, quando um cliente sem credenciais faz `GET /oauth2/jwks`, então a resposta é `200` com corpo JSON no formato `{"keys": [...]}`.
- Dado o par de chaves ativo, quando `GET /oauth2/jwks` é consultado, então o único item de `keys` traz `kty: "RSA"`, `use: "sig"`, `alg: "RS256"`, `kid` igual ao identificador do par ativo, e `n`/`e` correspondentes ao módulo e expoente público da chave RSA ativa, codificados em Base64URL.
- Dado o contrato do endpoint, quando `GET /oauth2/jwks` é consultado, então a resposta nunca inclui a chave privada nem qualquer atributo que a exponha.
- Dado o par de chaves ativo, quando `GET /oauth2/jwks` é consultado, então o array `keys` contém exatamente um item — nunca chaves de outras aplicações do cenário nem chaves de rotações anteriores.

**Story 2 — Rotação de chaves refletida automaticamente:**

- Dado que os arquivos `api-a-private.pem` e `api-a-cert.pem` são substituídos por um novo par de chaves válido, quando a aplicação é reiniciada, então `GET /oauth2/jwks` passa a refletir o novo `kid` e o novo módulo/expoente, sem exigir nenhuma ação adicional de republicação.

**Story 3 — Carregamento do par de chaves fixo:**

- Dado que os arquivos `api-a-private.pem` e `api-a-cert.pem` estão presentes em `src/main/resources`, quando a aplicação inicializa, então o par de chaves desses arquivos é carregado (2048 bits, RS256) e usado como par ativo.
- Dado o par de chaves carregado, quando a aplicação é reiniciada sem que os arquivos sejam alterados, então `GET /oauth2/jwks` retorna o mesmo `kid` e o mesmo módulo/expoente da consulta anterior.

**Story 4 — Documentação OpenAPI/Swagger:**

- Dado o endpoint `GET /oauth2/jwks` implementado, quando o contrato OpenAPI é consultado em `GET /v3/api-docs`, então a operação do endpoint aparece descrita, incluindo o schema de resposta com os campos `kty`, `kid`, `use`, `alg`, `n` e `e`.
- Dado o Swagger UI publicado, quando um cliente sem credenciais acessa `GET /swagger-ui.html`, então a página carrega sem exigir autenticação.

## Cross-domain dependencies

- **"Servidor de Recursos OAuth2 (Validação de Access Token)"** — estende a `SecurityFilterChain` criada por este domínio, adicionando suas próprias regras de proteção (`GET /api/protected`) e liberação (`GET /api/public`, `GET /actuator/health`), mantendo `GET /oauth2/jwks` liberado.
- **Keycloak** (sistema externo) — consome `GET /oauth2/jwks` para validar assinaturas produzidas com a chave privada da aplicação e, conforme o cenário, de `access_tokens` emitidos com essa chave.

## Risks and observations

- A estratégia de geração do identificador (`kid`) não é fixada pelo material de negócio — fica a cargo da implementação em `plan.md`, respeitando apenas a unicidade do `kid`.
- Esta POC não implementa testes automatizados (unitários, integração ou e2e), conforme decisão registrada em `discovery-answers.md`.
- O repositório está em estado greenfield para este domínio: nenhum scaffold Maven, código-fonte ou configuração de segurança existe ainda. Este spec cobre, além das regras de negócio do domínio, a infraestrutura transversal mínima (scaffold do projeto e `SecurityFilterChain` inicial) necessária para viabilizá-lo, já que é o primeiro domínio a ser implementado.
