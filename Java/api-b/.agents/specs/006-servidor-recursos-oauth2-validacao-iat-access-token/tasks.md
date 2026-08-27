# Tasks: Validação da claim iat do access_token em GET /api/protected

Esta POC não implementa testes automatizados (`discovery-answers.md`); a validação de cada tarefa
é manual, pelos casos correspondentes em `test-cases.md`.

- [x] **T1. Atualizar a documentação autoritativa do domínio para incluir a validação de iat**
  - Depends on: none
  - Alvo: `.agents/skills/servidor-recursos-oauth2/SKILL.md`.
  - Seções afetadas: regra 7 de "Regras de negócio" (lista de verificações de um `access_token`) e
    a linha "Access token validado por este domínio" em "Entidades e dados".
  - Mudança esperada: incluir `iat` ao lado de assinatura/`iss`/`exp`/`aud` como verificação
    obrigatória de um `access_token` em endpoint protegido, com a mesma consequência já descrita
    para as demais claims (ausência ou falha rejeita a requisição antes da lógica do endpoint).
  - Origem: história 1 e critérios de aceitação do `spec.md` desta unidade, motivados pela resposta
    humana ao gap `g1` da fase de spec (redirecionamento do pedido para este domínio com a extensão
    de escopo confirmada).
  - Acompanha a mesma mudança a frase equivalente de `AGENTS.md` ("Convenções arquiteturais
    importantes"), que hoje resume a regra 7 sem mencionar `iat`.

- [x] **T2. Validação da claim iat do access_token em GET /api/protected**
  - Depends on: T1
  - `IssuedAtValidator` (pacote `com.aplicacaosegura.resourceserver`, mesmo padrão do
    `AudienceValidator` existente), rejeitando um `access_token` sem a claim `iat` ou com `iat`
    posterior ao instante atual além da tolerância de relógio de 60 segundos registrada em
    `research.md`.
  - `ResourceServerJwtDecoderConfig.jwtDecoder` passa a compor o `IssuedAtValidator` no
    `DelegatingOAuth2TokenValidator`, ao lado do validador padrão (assinatura/`iss`/`exp`) e do
    `AudienceValidator` existente.
  - Cobre a História 1 do `spec.md` (`TC-1` a `TC-4` de `test-cases.md`).
