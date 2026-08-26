# Spec: Cliente OAuth2 com Client Assertion JWT

## Overview

Autenticação desta aplicação como client OAuth2 perante o Keycloak via `client_credentials`, usando um `client_assertion` JWT (RFC 7523) assinado com a chave privada RSA própria, e obtenção do `access_token` resultante para uso em chamadas subsequentes a outras aplicações do cenário.

## Domain

- Slug: `cliente-oauth2-client-assertion`
- Skill: [`.agents/skills/cliente-oauth2-client-assertion/SKILL.md`](../../skills/cliente-oauth2-client-assertion/SKILL.md)

## Scope

**In:**

- Autenticação desta aplicação como client OAuth2 perante o Keycloak via `client_credentials`, usando um `client_assertion` JWT assinado com a chave privada própria (RFC 7523), sem uso de `client_secret` compartilhado.
- Obtenção de um `access_token` do Keycloak a partir dessa autenticação.
- Disponibilização do `access_token` obtido para uso em chamadas subsequentes desta aplicação a outras aplicações do cenário.
- Endpoint de diagnóstico que aciona manualmente essa troca e confirma o sucesso ou a falha da obtenção do `access_token`, para validação do fluxo sem depender de um consumidor real do `access_token`.

**Out:**

- Implementação da chamada a uma API específica do cenário (ex.: api-b) usando o `access_token` obtido — o material de negócio trata essa chamada como contexto motivador do fluxo, não como funcionalidade obrigatória desta aplicação.
- Geração, rotação e publicação do par de chaves RSA usado para assinar o `client_assertion` — domínio "Gestão de Chaves RSA e Publicação JWKS".
- Validação de `access_tokens` recebidos por esta aplicação em endpoints próprios — domínio "Servidor de Recursos OAuth2 (Validação de Access Token)".
- Cache, renovação antecipada ou persistência do `access_token` entre reinicializações da aplicação — comportamento não fixado pelo material de negócio.
- Exposição do `access_token` obtido através do endpoint de diagnóstico ou de qualquer outro contrato — o endpoint apenas confirma sucesso ou falha da troca, nunca devolve o token.

## Domain boundary

**This spec implements:**

- Montagem do JWT `client_assertion` com as claims `iss`, `sub`, `aud`, `exp`, `iat` e `jti`, assinado em RS256 com a chave privada e o `kid` ativos fornecidos pelo domínio "Gestão de Chaves RSA e Publicação JWKS" (bean `RSAKey activeRsaKey()` de `RsaKeyPairConfig`).
- Execução da troca `POST /oauth2/token` junto ao Keycloak com `grant_type=client_credentials`, `client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer` e `client_assertion=<JWT assinado>`, e obtenção do `access_token` retornado.
- Disponibilização do `access_token` obtido para uso em chamadas subsequentes desta aplicação a outras aplicações do cenário.
- Dependência `spring-boot-starter-oauth2-client` e a configuração mínima de registro do client OAuth2 desta aplicação junto ao Keycloak (grant `client_credentials` com autenticação via JWT assinado) — infraestrutura transversal que este domínio cria por ser o primeiro a precisar dela, já que o `pom.xml` atual não contém essa dependência nem qualquer configuração de OAuth2 Client.
- Endpoint `GET /diagnostics/oauth2-client-assertion`, de uso exclusivamente diagnóstico, que solicita o `access_token` ao gerenciador de clients autorizados e responde com a confirmação de sucesso ou falha da troca (sem devolver o `access_token`), documentado via OpenAPI/Swagger como os demais endpoints do projeto.
- Liberação de `GET /diagnostics/oauth2-client-assertion` na `SecurityFilterChain`, sem exigir autenticação, estendendo a lista de caminhos públicos já criada pelo domínio "Gestão de Chaves RSA e Publicação JWKS" — necessária para que o endpoint permaneça utilizável antes da implementação do domínio "Servidor de Recursos OAuth2".

**Belongs to other domains (cross-domain, does not become a task here):**

- Geração, rotação e publicação do par de chaves RSA (`GET /oauth2/jwks`) → domínio "Gestão de Chaves RSA e Publicação JWKS"; este domínio apenas consome a chave privada e o `kid` vigentes.
- Validação da assinatura do `client_assertion` e emissão do `access_token` → responsabilidade do Keycloak, sistema externo; este domínio apenas depende desse resultado.
- Implementação da chamada subsequente a outra aplicação do cenário (ex.: api-b) usando o `access_token` obtido → fora do escopo obrigatório de qualquer domínio desta aplicação, conforme o material de negócio.
- Validação de `access_tokens` recebidos por esta aplicação em endpoints próprios → domínio "Servidor de Recursos OAuth2 (Validação de Access Token)".

## User stories

1. Como a aplicação (no papel de client OAuth2), quero montar e assinar um JWT `client_assertion` com minha chave privada RSA vigente, para me autenticar perante o Keycloak sem depender de um `client_secret` compartilhado.
2. Como a aplicação (no papel de client OAuth2), quero trocar o `client_assertion` assinado por um `access_token` junto ao Keycloak via `POST /oauth2/token`, para obter uma credencial válida para chamar outras aplicações do cenário.
3. Como a aplicação (no papel de client OAuth2), quero manter o `access_token` obtido disponível internamente, para usá-lo em chamadas subsequentes a outras aplicações quando essa necessidade existir.
4. Como responsável por validar esta unidade, quero acionar manualmente a troca do `client_assertion` por `access_token` através de um endpoint de diagnóstico, para confirmar que o fluxo funciona de ponta a ponta sem depender de um consumidor real do `access_token`.

## Acceptance criteria

**História 1 — Montagem do client_assertion:**

- Dado o par de chaves RSA ativo mantido pelo domínio "Gestão de Chaves RSA e Publicação JWKS", quando a aplicação monta o `client_assertion`, então o JWT resultante está assinado em RS256 com essa chave privada e traz no cabeçalho o `kid` correspondente.
- Dado o `client_assertion` montado, quando suas claims são inspecionadas, então `iss` e `sub` contêm o mesmo valor — o `client_id` configurado desta aplicação no Keycloak —, `aud` contém o identificador do endpoint de token do Keycloak configurado para o ambiente, `iat` e `exp` estão presentes com `exp` posterior a `iat`, e `jti` está presente.
- Dado que dois `client_assertion` são montados em chamadas distintas, quando seus `jti` são comparados, então os valores são diferentes.

**História 2 — Troca por access_token:**

- Dado um `client_assertion` válido montado pela aplicação e um client desta aplicação configurado no Keycloak para autenticação via JWT assinado apontando para `GET /oauth2/jwks` desta aplicação, quando a aplicação executa `POST /oauth2/token` no Keycloak com `grant_type=client_credentials`, `client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer` e `client_assertion=<JWT assinado>`, então o Keycloak responde com um `access_token`.

**História 3 — Disponibilização do access_token:**

- Dado um `access_token` obtido com sucesso, quando outro componente desta aplicação precisa chamar uma aplicação do cenário (ex.: api-b) como resource server, então esse `access_token` está disponível para uso nessa chamada — a implementação da chamada em si não faz parte deste domínio.

**História 4 — Endpoint de diagnóstico:**

- Dado o client desta aplicação configurado no Keycloak para autenticação via JWT assinado, quando um cliente sem credenciais chama `GET /diagnostics/oauth2-client-assertion`, então a aplicação monta e assina o `client_assertion`, executa a troca com o Keycloak, e a resposta é `200` confirmando o sucesso da obtenção do `access_token`, sem incluir o `access_token` no corpo da resposta.
- Dado um cenário em que o Keycloak rejeita a troca (ex.: client mal configurado ou assinatura inválida), quando `GET /diagnostics/oauth2-client-assertion` é chamado, então a resposta é `502`, refletindo a falha da troca sem expor detalhes da chave privada nem o corpo de erro do Keycloak.
- Dado o endpoint de diagnóstico implementado, quando o contrato OpenAPI é consultado em `GET /v3/api-docs`, então a operação correspondente a `GET /diagnostics/oauth2-client-assertion` aparece descrita.

## Cross-domain dependencies

- **"Gestão de Chaves RSA e Publicação JWKS"** — fornece a chave privada RSA e o `kid` vigentes usados para assinar o `client_assertion`, e expõe `GET /oauth2/jwks` para que o Keycloak valide essa assinatura.
- **Keycloak** (sistema externo) — recebe `POST /oauth2/token`, consulta `GET /oauth2/jwks` desta aplicação para validar a assinatura do `client_assertion` e emite o `access_token`; precisa ter esta aplicação registrada como client configurado para autenticação via JWT assinado, pré-requisito não implementado por esta aplicação.

## Risks and observations

- Esta POC não implementa testes automatizados (unitários, integração ou e2e); a verificação deste fluxo depende de um Keycloak real, configurado com o client desta aplicação em autenticação via JWT assinado.
- Os valores concretos de `client_id` (`iss`/`sub`), do identificador do endpoint de token do Keycloak (`aud`) e da duração da janela de validade (`exp` − `iat`) são específicos do ambiente de implantação e não são fixados pelo material de negócio — a implementação os trata como configuráveis, sem fixar valores.
- A estratégia concreta de geração do `jti` não é fixada pelo material de negócio, apenas a exigência de que seja único por `client_assertion`.
- O repositório ainda não contém nenhum código deste domínio, nem a dependência `spring-boot-starter-oauth2-client`; o domínio "Gestão de Chaves RSA e Publicação JWKS", do qual este depende, já está implementado e disponível para consumo.
- O endpoint `GET /diagnostics/oauth2-client-assertion` fica público (sem autenticação) para permanecer utilizável antes da implementação do domínio "Servidor de Recursos OAuth2"; ele existe exclusivamente para validação manual desta unidade, não é uma funcionalidade de negócio, e nunca devolve o `access_token` obtido.
