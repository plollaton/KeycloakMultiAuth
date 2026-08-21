---
name: discovery-answers
description: Memória do contexto de discovery — prova de conceito de autenticação Keycloak em Spring Boot/Java 21, escopo de um único domínio, com o modelo criptográfico (descriptografia JWE com chave privada), sem documentação adicional e sem testes automatizados já decididos.
metadata:
  author: clovis-cli
  responsibility: "Durable memory of the functional-discovery context and decisions: objective, scope, declared restrictions, validated cross-cutting decisions (with destination), documentation forms to maintain and the log of human decisions resolved in the gap loop. Source re-read by the following stages; its restrictions take precedence over later inferences."
---

# Contexto e decisões da discovery

## Objetivo

Construir uma aplicação **Spring Boot / Java 21** que **valide as requisições** destinadas à
"aplicação de moto offline". As requisições chegam com um header `Bearer` emitido por um servidor
**Keycloak** associado a um **certificado**; a aplicação mantém a **chave privada** para
descriptografar o token e validar permissões. O objetivo do projeto, declarado pelo usuário, é
**provar a ideia de autenticação** — não entregar funcionalidade de negócio.

## Escopo

- Projeto inteiro (greenfield). O diretório de execução está vazio (apenas `.clovis/` e `.git/`);
  não há scaffold, código nem framework spec-driven concorrente.
- **Um único domínio** (decisão explícita do usuário), exercitado por **uma requisição no estilo
  "hello world"** — recurso mínimo protegido para demonstrar o fluxo de autenticação/autorização.

## Restrições declaradas pelo usuário

- Stack fixa: **Spring Boot**, **Java 21**.
- Emissor de identidade: **Keycloak** (token `Bearer` por requisição).
- A aplicação guarda a **chave privada** localmente e a usa para descriptografia do token e
  validação de permissões.
- Granularidade fixa em **um domínio**; superfície mínima ("hello world").
- Nenhuma outra restrição adicional foi declarada.

## Decisões transversais consolidadas

- **Modelo criptográfico da validação do token:** o Keycloak cifra o token (JWE) e a aplicação o
  **descriptografa com a chave privada local**; após decifrar, valida as permissões do portador.
  Destino: regra central do domínio `validacao-autenticacao-permissoes` (reflete na skill do
  domínio na Stage 2).
- **Documentação de API:** nenhuma documentação adicional (sem Swagger/OpenAPI, Postman ou ADRs)
  nesta prova de conceito. Destino: convenção em `AGENTS.md` na Stage 2; não gera `technical-skill`.
- **Testes automatizados:** nenhum teste automatizado nesta prova de conceito. Destino: convenção em
  `AGENTS.md` na Stage 2; não gera `technical-skill`.
- **Persistência / cache / mensageria:** fora de escopo nesta prova de conceito. A regra declarada
  ("hello world", validar a ideia de autenticação, um domínio) não envolve entidades de negócio nem
  estado persistente. Destino: convenção em `AGENTS.md` na Stage 2.
- **Observabilidade:** não é foco da prova de conceito; basta o logging padrão do Spring Boot.
  Destino: convenção em `AGENTS.md` na Stage 2.

## Formas de documentação a manter

Nenhuma documentação além das skills de negócio. A prova de conceito não mantém Swagger/OpenAPI,
coleção Postman nem ADRs.

## Log de decisões humanas (loop de gaps)

- **`auth-crypto-model`** — Dúvida: o token deveria ser descriptografado com a chave privada da
  aplicação (JWE) ou ter apenas a assinatura verificada com a chave pública do Keycloak (JWS/JWKS)?
  **Decisão:** descriptografia do token (JWE) — o Keycloak cifra o token e a aplicação o decifra
  com a chave privada local.
- **`api-documentation`** — Dúvida: qual forma de documentação de API manter (Swagger/OpenAPI,
  Postman, ADRs ou nenhuma)? **Decisão:** não manter documentação adicional nesta prova de conceito.
- **`testing-strategy`** — Dúvida: qual estratégia de testes automatizados adotar (nenhuma,
  unitários, ou unitários + integração)? **Decisão:** sem testes automatizados nesta prova de
  conceito.
