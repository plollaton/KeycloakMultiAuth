---
name: business-input
description: Registro fiel do material de negócio fornecido pelo usuário (tipo de sistema e regra de negócio em texto livre) e índice de proveniência/reacesso das informações, organizado por fonte.
metadata:
  author: clovis-cli
  responsibility: Faithful record of the business material provided by the user and an index of provenance and re-access of that material, organized by source. Stage 2 reopens the original sources from here; it does not classify domains, boundaries, dependencies or confidence level.
---

# Material de negócio fornecido

Este projeto é greenfield. Todo o material de negócio veio como **texto livre digitado pelo
usuário** no questionário da sessão. Nenhum caminho de arquivo, URL ou espaço de MCP foi citado,
portanto não há fontes externas a reabrir — o reacesso se dá relendo os próprios campos do
questionário registrados no estado da CLI.

## Fonte 1 — Tipo de sistema (texto do usuário)

**Proveniência (ONDE):** campo `discoveryAnswers.systemType` do questionário da sessão, persistido em
`.clovis/cli-state.json` (raiz do projeto).

**Reacesso (COMO):** abrir `.clovis/cli-state.json` e ler a chave `discoveryAnswers.systemType`.
Sem credencial ou método não óbvio envolvido.

**Conteúdo, conforme fornecido (transcrição literal):**

> gostaria de uma aplicação em SpringBoot, java 21, que valide as requisições realizadas para a
> aplicação de moto offline, as requisições serão enviadas com um header Bearer gerado por um
> servidor Keycloak com um certificado. A aplicação permanecerá com a chave privada para a
> descriptografia e validaçã de permissões.

**Informações de negócio extraídas desta fonte:**
- Plataforma-alvo: aplicação Spring Boot em Java 21.
- Papel da aplicação: validar as requisições recebidas destinadas à "aplicação de moto offline".
- Transporte de credencial: header `Bearer` em cada requisição.
- Emissor do token: servidor **Keycloak**, associado a um **certificado**.
- Material criptográfico local: a aplicação guarda a **chave privada**, usada para
  **descriptografia** do token e **validação de permissões**.

## Fonte 2 — Regra de negócio / escopo (texto do usuário)

**Proveniência (ONDE):** campo `discoveryAnswers.businessInput` do questionário da sessão, persistido
em `.clovis/cli-state.json` (raiz do projeto).

**Reacesso (COMO):** abrir `.clovis/cli-state.json` e ler a chave `discoveryAnswers.businessInput`.
Sem credencial ou método não óbvio envolvido.

**Conteúdo, conforme fornecido (transcrição literal):**

> apenas um dominio, com uma requisição no estilo hello world, o que me importa é validar a ideia
> da autenticação

**Informações de negócio extraídas desta fonte:**
- Granularidade: **apenas um domínio** (decisão explícita do usuário).
- Superfície funcional: **uma única requisição no estilo "hello world"** — um recurso mínimo
  protegido, usado só para exercitar o fluxo.
- Objetivo declarado: **validar a ideia da autenticação** (prova de conceito de autenticação/
  autorização), e não entregar funcionalidade de negócio.

## Índice de proveniência (resumo)

| Informação | Fonte | Localização de reacesso |
|---|---|---|
| Stack Spring Boot / Java 21 | Fonte 1 | `.clovis/cli-state.json` → `discoveryAnswers.systemType` |
| Validação de requisições da app de moto offline | Fonte 1 | idem |
| Token Bearer emitido pelo Keycloak com certificado | Fonte 1 | idem |
| Chave privada local para descriptografia e validação de permissões | Fonte 1 | idem |
| Escopo de um único domínio | Fonte 2 | `.clovis/cli-state.json` → `discoveryAnswers.businessInput` |
| Requisição "hello world" como recurso protegido | Fonte 2 | idem |
| Objetivo de provar a ideia de autenticação | Fonte 2 | idem |
