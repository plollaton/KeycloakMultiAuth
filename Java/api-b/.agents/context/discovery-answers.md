---
name: discovery-answers
description: Objetivo, escopo, restrições declaradas e decisões transversais da descoberta funcional da POC de autenticação Spring Boot (OAuth2 Client + Resource Server + JWKS); documentação via OpenAPI/Swagger e sem testes automatizados nesta POC.
metadata:
  author: clovis-cli
  responsibility: Memória durável do contexto e das decisões da descoberta funcional: objetivo, escopo, restrições declaradas, decisões transversais validadas (com destino), formas de documentação a manter e o log de decisões humanas resolvidas no loop de gaps. Fonte relida pelos estágios seguintes antes de montar prompts ou gerar artefatos; suas restrições têm precedência sobre inferências posteriores do agente.
---

# Objetivo e escopo

- **Tipo de sistema**: POC de backend Java para validar autenticação.
- **Tipo de projeto**: greenfield (diretório do projeto sem código-fonte prévio; apenas `descritivo.md` e configuração do `.clovis`/`.claude`).
- **Objetivo**: construir uma aplicação Spring Boot que atua simultaneamente como OAuth2 Client (autenticando-se no Keycloak via `client_assertion` JWT, RFC 7523) e como OAuth2 Resource Server (validando access_tokens emitidos pelo Keycloak), expondo um endpoint JWKS próprio com sua chave pública RSA para que o Keycloak valide o `client_assertion` e suporte rotação de chaves.
- **Escopo**: projeto inteiro (`aplicacao-segura`).

# Restrições declaradas

- Nenhuma restrição adicional foi declarada pelo usuário além do que já está fixado no `descritivo.md`.
- **Stack alvo fixada pelo descritivo.md** (linhas 35-51, ver [[business-input]]): Spring Boot 4.1.x, Spring Framework 7.x, Spring Security 7.x, Java 21 ou superior, Jakarta EE 11, `spring-boot-starter-oauth2-client`, `spring-boot-starter-oauth2-resource-server`, Nimbus JOSE+JWT (via starters), Lombok (opcional). Trata-se de uma decisão transversal já validada pelo próprio material de negócio — destino: convenção fixa em `AGENTS.md` no Estágio 2.

# Formas de documentação a manter

Os endpoints REST do projeto (`/oauth2/jwks`, `/api/public`, `/api/protected`) são documentados via OpenAPI/Swagger, conforme decisão humana registrada no gap `formas-documentacao`.

# Estratégia de testes

Esta POC não implementa testes automatizados (unitários, integração ou e2e), conforme decisão humana registrada no gap `estrategia-testes`.

# Decisões transversais validadas

| Decisão | Origem | Destino |
|---|---|---|
| Stack alvo (Spring Boot 4.1.x / Spring Security 7.x / Java 21+ / Jakarta EE 11 / Nimbus JOSE+JWT) | `descritivo.md` linhas 35-51 | Convenção em `AGENTS.md` |
| Chave RSA em memória por padrão, com carregamento opcional de PKCS12 via `@ConfigurationProperties` | `descritivo.md` linhas 60-62 | Regra do domínio "Gestão de Chaves RSA e Publicação JWKS" no `functional-map.md` |
| Documentação dos endpoints via OpenAPI/Swagger | Resposta humana ao gap `formas-documentacao` | Technical skill "Documentação de API com OpenAPI/Swagger" |
| Sem testes automatizados nesta POC | Resposta humana ao gap `estrategia-testes` | Convenção em `AGENTS.md` |

# Framework spec-driven concorrente

Nenhum framework spec-driven concorrente (ex.: Spec Kit, OpenSpec) foi encontrado no diretório do projeto `aplicacao-segura` nesta rodada — o diretório contém apenas `.clovis/`, `.claude/` e `descritivo.md`.

# Log de decisões humanas resolvidas no loop de gaps

- **`formas-documentacao`**: pergunta — que forma de documentação adicional manter no projeto, já que o descritivo.md não definia nenhuma. Decisão — documentar os endpoints via OpenAPI/Swagger.
- **`estrategia-testes`**: pergunta — qual estratégia de testes adotar, já que o descritivo.md não definia nenhuma. Decisão — não implementar testes automatizados nesta POC.
