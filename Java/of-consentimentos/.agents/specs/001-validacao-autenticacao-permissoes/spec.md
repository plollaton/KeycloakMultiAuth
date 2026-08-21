# Spec: Validação de Autenticação e Permissões

## Overview

Proteção do endpoint "hello world" da aplicação de moto offline, exigindo um token `Bearer`
emitido pelo Keycloak, descriptografado com a chave privada local (JWE) e portador de ao menos
uma permissão, antes de liberar o acesso.

## Domain

- Slug: `validacao-autenticacao-permissoes`
- Skill: [`.agents/skills/validacao-autenticacao-permissoes/SKILL.md`](../../skills/validacao-autenticacao-permissoes/SKILL.md)

## Scope

**In:**

- Proteção do endpoint "hello world" exigindo um token `Bearer` em toda requisição.
- Descriptografia do token JWE com a chave privada local da aplicação, como condição de
  autenticidade do token perante a aplicação.
- Checagem estrutural de validade do token decifrado (por exemplo, expiração).
- Checagem das permissões do portador (papéis de realm, papéis de client/resource ou escopos)
  no token decifrado, liberando o acesso quando ao menos uma estiver presente.
- Sinalização diferenciada de falha de autenticação (401) e falha de autorização (403).

**Out:**

- Qualquer funcionalidade de negócio própria do endpoint protegido além de demonstrar o fluxo de
  autenticação/autorização.
- Persistência de estado sobre o portador, o token ou a decisão de acesso.
- Documentação adicional de API (Swagger/OpenAPI, coleção Postman, ADRs).
- Testes automatizados.
- Controle de acesso granular por permissão específica — qualquer permissão presente no token
  decifrado já libera o acesso.
- Configuração do lado do Keycloak (registro do certificado desta aplicação, emissão de tokens
  cifrados, atribuição de papéis/escopos ao portador) — precondição externa consumida por este
  domínio, não implementada por esta unidade.

## Domain boundary

**This spec implements:**

- Endpoint protegido "hello world" — único recurso de negócio deste domínio.
- Camada de resource server (Spring Security) que intercepta a requisição e extrai o header
  `Authorization: Bearer <token>`, negando o acesso quando ausente.
- Descriptografia do token JWE com a chave privada local da aplicação.
- Checagem estrutural de validade do token decifrado (por exemplo, expiração).
- Extração e checagem das permissões do portador (papéis de realm, papéis de client/resource ou
  escopos) presentes no token decifrado.
- Sinalização diferenciada de falha de autenticação (401) e falha de autorização (403) ao
  chamador.

**Belongs to other domains (cross-domain, does not become a task here):**

- Não aplicável — projeto de domínio único, sem outros domínios internos ao repositório (ver
  `functional-map.md`). As únicas dependências externas ao que esta unidade implementa estão
  listadas em "Cross-domain dependencies", abaixo.

## User stories

1. Como mantenedor da aplicação de moto offline, quero que toda requisição ao recurso protegido
   exija um token `Bearer`, para impedir acesso não autenticado.
2. Como mantenedor da aplicação, quero que o token `Bearer` emitido pelo Keycloak seja
   descriptografado com a chave privada local antes de qualquer decisão de acesso, para validar
   a autenticidade do portador conforme o modelo criptográfico adotado (JWE).
3. Como mantenedor da aplicação, quero que o acesso seja negado com sinal de falha de
   autenticação quando o token estiver ausente, corrompido, incompatível com a chave privada ou
   estruturalmente inválido (por exemplo, expirado), para impedir o uso de credenciais inválidas.
4. Como mantenedor da aplicação, quero que, após decifrar o token, a aplicação verifique se o
   portador possui ao menos uma permissão (papel ou escopo), para provar que a checagem de
   autorização ocorre no fluxo.
5. Como mantenedor da aplicação, quero que o acesso seja negado com sinal de falha de
   autorização quando o token decifrado não carregar nenhuma permissão, para distinguir esse
   caso de uma falha de autenticação.

## Acceptance criteria

**Story 1 — Header obrigatório:**

- Given uma requisição ao endpoint protegido sem o header `Authorization`, when a requisição é
  enviada, then a aplicação responde `401` (falha de autenticação).

**Story 2 e 3 — Descriptografia e validade do token:**

- Given uma requisição com o header `Authorization: Bearer <token>`, when o token não pode ser
  decifrado com a chave privada local (chave incompatível, token corrompido ou malformado), then
  a aplicação responde `401` (falha de autenticação).
- Given uma requisição com o header `Authorization: Bearer <token>`, when o token é decifrado com
  sucesso mas falha em uma checagem estrutural própria de JWT (por exemplo, está expirado), then
  a aplicação responde `401` (falha de autenticação).
- Given uma requisição com o header `Authorization: Bearer <token>`, when o token é decifrado com
  sucesso e passa nas checagens estruturais, then a aplicação prossegue para a checagem de
  permissões descrita na Story 4.

**Story 4 e 5 — Checagem de permissões:**

- Given um token decifrado com sucesso que carregue ao menos uma permissão (papel de realm,
  papel de client/resource ou escopo), when a requisição é processada, then o acesso é liberado e
  o endpoint "hello world" responde normalmente.
- Given um token decifrado com sucesso que não carregue nenhuma permissão, when a requisição é
  processada, then a aplicação responde `403` (falha de autorização).

## Cross-domain dependencies

- **Keycloak** — emite o token `Bearer` cifrado (JWE) usando o certificado desta aplicação; é a
  origem das permissões (papéis/escopos) avaliadas após a descriptografia. Depende de
  configuração externa ao repositório: o client desta aplicação no Keycloak configurado para
  cifrar o token com o certificado registrado, e ao menos um papel/escopo atribuído ao portador —
  sem essas configurações, nenhuma requisição pode ser autenticada ou autorizada por esta
  unidade, independentemente da implementação local.

## Risks and observations

- A forma de guardar/carregar a chave privada local (keystore, arquivo, variável de ambiente)
  ainda não foi definida nas fontes investigadas; é uma decisão de implementação a ser tratada no
  `plan.md` desta unidade.
- A rota e o verbo HTTP concretos do endpoint "hello world" não são fixados pelas fontes de
  negócio, por serem uma decisão de implementação substituível; tratada no `plan.md` desta
  unidade.
- Esta prova de conceito não mantém testes automatizados nem documentação adicional de API
  (convenção registrada em `discovery-answers.md` e `AGENTS.md`); a validação do fluxo descrito
  aqui é manual.
