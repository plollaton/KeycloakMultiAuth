---
name: functional-map
description: Mapa funcional com um único domínio — Validação de Autenticação e Permissões de tokens Bearer JWE do Keycloak, descriptografados com a chave privada local, exercitado por um endpoint hello world. Sem gaps abertos.
metadata:
  author: clovis-cli
  responsibility: "Map of identification of the business domains (bounded contexts), their boundaries, dependencies and suggested implementation order. Index of domains for skill generation and the spec-driven flow; it does not detail business rules nor duplicate the cross-cutting decisions, which live in the discovery-answers.md."
---

# Mapa Funcional

Projeto greenfield, prova de conceito com **um único domínio** (decisão explícita do usuário).
O diretório de execução está vazio (sem código), portanto toda evidência vem do material de negócio
fornecido pelo usuário.

## Domínio: Validação de Autenticação e Permissões

**Objetivo de negócio:** garantir que cada requisição destinada à "aplicação de moto offline" traga
um token `Bearer` válido emitido pelo Keycloak e que o portador tenha as permissões necessárias. A
aplicação mantém a chave privada e a usa para processar o token (descriptografia/validação) antes de
autorizar o acesso. Um endpoint no estilo **"hello world"** funciona como o recurso protegido que
exercita e demonstra esse fluxo — ele não é um domínio próprio, e sim a superfície mínima usada para
provar a ideia de autenticação.

**Evidência no material fornecido:**
- Papel de validar as requisições da app de moto offline, com token `Bearer` do Keycloak e chave
  privada local para descriptografia e validação de permissões — ver `business-input.md`, Fonte 1.
- Escopo de um único domínio e requisição "hello world" com o objetivo de validar a autenticação —
  ver `business-input.md`, Fonte 2.

**Dependências entre domínios:** nenhuma (domínio único).

**Regras inferidas (apenas o necessário para identificar o domínio):**
- Toda requisição ao recurso protegido exige um token `Bearer`; ausência ou invalidez ⇒ acesso
  negado.
- O token é **cifrado (JWE)** pelo Keycloak; a aplicação o **descriptografa com a chave privada
  local** antes de qualquer autorização. Falha na descriptografia ⇒ acesso negado.
- Após decifrar o token, a autorização considera as **permissões** do portador (papéis/escopos) — o
  mapeamento específico será detalhado na skill do domínio na Stage 2.

**Dependências técnicas do domínio:**
- **Keycloak** — emissor dos tokens `Bearer` cifrados (JWE). Sem ele não há credencial a validar; é
  a origem da identidade e das permissões consumidas pela aplicação.
- **Chave privada + certificado** — material criptográfico que a aplicação guarda para
  **descriptografar** o token JWE e liberar o acesso. Sem ele, nenhuma requisição pode ser validada.
  A forma de guardar/carregar essa chave (keystore, arquivo, variável de ambiente) será definida na
  Stage 2.
- **Spring Security (resource server)** — camada que intercepta as requisições, extrai o header
  `Bearer` e aplica a descriptografia/autorização em cada endpoint; sem ela o endpoint "hello world"
  ficaria desprotegido.

**Dependências externas relevantes (por nome):** `Keycloak`, `Spring Boot`, `Spring Security`.

**Nível de confiança:** `high` — a existência e a fronteira do domínio têm evidência direta no
material do usuário, e o modelo criptográfico (descriptografia JWE com chave privada) foi confirmado
por decisão humana (ver `discovery-answers.md`).
