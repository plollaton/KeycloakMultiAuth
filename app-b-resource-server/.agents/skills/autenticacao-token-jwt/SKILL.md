---
name: autenticacao-token-jwt
description: >
  Esta é a documentação autoritativa do domínio Autenticação e validação offline do
  token JWT. Cobre como o resource server exige e valida, sem consultar o Keycloak em
  tempo de requisição, o access token Bearer (JWT) de cada chamada a rotas protegidas:
  verificação criptográfica da assinatura contra chaves públicas locais, validação de
  emissor (iss), de audience (aud) e de validade temporal (exp/nbf/iat) com clock skew,
  além da postura stateless com CSRF desabilitado. Carregue em tarefas sobre autenticação,
  validação offline de JWT, token Bearer/Authorization, claims iss/aud/exp, clock skew,
  audience esperada (app-b) e Audience Mapper, respostas 401/não autorizado, decoder de
  JWT, resource server OAuth2 ou a liberação do endpoint de health.
metadata:
  author: clovis-cli
  type: domain-skill
---

# Autenticação e validação offline do token JWT

> **Manutenção desta skill**
>
> Atualize este documento sempre que o comportamento do domínio mudar de propósito,
> mantendo a skill fiel ao comportamento implementado. Uma mudança deliberada de regra
> (novo validador, novo claim exigido, nova política de sessão) é maintenance e deve ser
> refletida aqui. Uma divergência semântica entre skill e código sem decisão registrada
> que a resolva é escalada para decisão humana — nunca ajuste a skill nem o código por
> conta própria.

## Visão geral do domínio

Este domínio é o **coração de segurança** do resource server: garante que **toda**
requisição a rotas protegidas apresente um access token (JWT) legítimo, emitido pelo
**Keycloak**, e o valide **offline** — verificando assinatura, emissor, audience e validade
temporal — antes de permitir o acesso. É uma POC cujo propósito é demonstrar exatamente
esse fluxo de autenticação ponta a ponta.

**Offline** significa que a verificação **não faz chamada de rede ao Keycloak no momento da
requisição**. A assinatura é conferida contra um conjunto de chaves públicas (JWKS)
mantido **localmente**, fornecido por outro domínio (a gestão da fonte local de chaves).
A motivação de negócio declarada é que o resource server roda em um segmento de rede **sem
saída direta para o Keycloak**.

O escopo deste domínio começa na chegada da requisição autenticada por Bearer token e
termina em **decidir se a requisição está autenticada** (produzindo um principal com os
claims validados) ou **rejeitá-la**. A obtenção/recarga do material de chaves pertence ao
domínio de gestão local do JWKS; o consumo dos metadados do token por um recurso protegido
pertence ao domínio do recurso protegido — nenhum dos dois é descrito aqui.

## Regras de negócio

As regras abaixo usam numeração própria desta skill, apenas para referência de leitura.

1. **Token Bearer obrigatório em rotas protegidas.** Toda requisição, exceto as
   explicitamente liberadas (regra 8), deve apresentar um access token JWT no cabeçalho
   `Authorization: Bearer <JWT>`. Sem token válido, a requisição é rejeitada com
   **401 (não autorizado)** e não alcança o recurso.

2. **Validação offline da assinatura.** A assinatura do token é verificada
   criptograficamente contra as **chaves públicas locais (JWKS)**, sem qualquer chamada
   HTTP ao Keycloak em tempo de requisição. A chave correta é selecionada pelo identificador
   de chave (`kid`) presente no cabeçalho do token; se o `kid` do token **não** estiver
   entre as chaves disponíveis, a assinatura não pode ser verificada e o token é rejeitado.

3. **Validação do emissor (`iss`).** O claim `iss` do token deve ser **idêntico** ao
   emissor configurado — o realm do Keycloak esperado (tipicamente
   `https://<host-keycloak>/realms/<realm>`). Emissor divergente rejeita o token.

4. **Validação da audience (`aud`).** O claim `aud` do token deve **conter** a audience
   esperada (valor padrão `app-b`). A checagem é de **contém**, não de igualdade: o `aud`
   pode ser uma lista e basta que a audience esperada esteja nela. Um `aud` ausente ou que
   não contenha o valor esperado rejeita o token.

5. **Dependência do Audience Mapper no Keycloak.** Por padrão o Keycloak **não** inclui a
   audience `app-b` no token (emite apenas `aud: account`). É necessário um **Audience
   Mapper** configurado no client scope do Keycloak para que o token carregue a audience
   esperada. Sem esse mapper, a regra 4 falha e **toda** chamada é rejeitada, ainda que o
   token seja legítimo.

6. **Validação temporal (`exp`/`nbf`/`iat`) com tolerância de relógio.** A validade
   temporal do token é verificada com uma tolerância de relógio (**clock skew**)
   configurável, cujo valor padrão é **60 segundos**. A tolerância absorve pequenas
   diferenças de relógio entre o Keycloak e o resource server; um token expirado (ou ainda
   não válido) além dessa tolerância é rejeitado.

7. **Todos os validadores aplicados em conjunto.** Validação temporal, de emissor e de
   audience são encadeadas e aplicadas **em conjunto** a cada token. A **falha de qualquer
   uma** invalida o token — não há validação parcial nem token aceito com uma checagem
   pulada.

8. **Rota de health liberada; todo o restante exige autenticação.** O endpoint de health
   (`/actuator/health/**`) é público (`permitAll`). Qualquer outra rota exige uma requisição
   autenticada.

9. **API stateless, sem sessão.** A autenticação é **stateless** (política de sessão
   `STATELESS`): nenhuma sessão HTTP é criada ou utilizada, e cada requisição é autenticada
   isoladamente pelo seu próprio Bearer token. Não há cookie de sessão nem estado de login
   entre requisições.

10. **CSRF desabilitado de propósito.** A proteção CSRF é **desabilitada deliberadamente**,
    porque a API é stateless e autenticada por Bearer token (não por sessão/cookie), cenário
    em que CSRF não se aplica. Essa desabilitação é intencional e não deve ser "corrigida"
    reativando o CSRF.

11. **Nunca registrar/expor o token bruto.** Logs e respostas deste domínio jamais devem
    conter o JWT bruto; apenas metadados não sensíveis do token (por exemplo `sub`, `azp`,
    `jti`, timestamps) podem ser registrados.

12. **Decoder manual desativa a auto-configuração do resource server.** A construção manual
    do componente que decodifica/valida o JWT (sobre a fonte local de chaves) **desativa** a
    auto-configuração padrão do resource server. Em consequência, a propriedade
    `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` (busca das chaves por HTTP no
    Keycloak) **não** deve ser usada neste projeto — usá-la contraria a decisão de validação
    offline.

## Fluxos e ciclo de vida

**Autenticação de uma requisição protegida (caminho principal):**
1. Chega uma requisição com o cabeçalho `Authorization: Bearer <JWT>`.
2. O pipeline de resource server extrai o token e seleciona a chave pública pelo `kid`.
3. A **assinatura** é verificada contra as chaves locais (regra 2).
4. São aplicados, em conjunto, os validadores de **tempo** (`exp`/`nbf`/`iat` com clock
   skew), de **emissor** (`iss`) e de **audience** (`aud`) — regras 3 a 7.
5. **Se todas as checagens passam** → a requisição é considerada **autenticada**, gerando um
   principal que carrega os claims validados do token, e segue para o recurso.
6. **Se qualquer checagem falha** (assinatura inválida, `kid` desconhecido, `iss`/`aud`
   divergente, token expirado/fora de validade) → a requisição é **rejeitada com 401** e não
   alcança o recurso.

**Requisição sem token ou à rota de health:**
- Requisição **sem** `Authorization: Bearer` a uma rota protegida → **401**.
- Requisição ao endpoint de health → liberada sem autenticação (regra 8).

**Rotação de chave no Keycloak (impacto neste domínio):** quando o Keycloak passa a assinar
com uma nova chave, os tokens novos trazem um `kid` novo. Este domínio só consegue verificar
a assinatura desses tokens **depois** que a fonte local de chaves passa a conhecer o novo
`kid`; enquanto o material local estiver desatualizado, tokens legítimos com o novo `kid` são
rejeitados na etapa de assinatura (regra 2). A atualização do material de chaves em si
pertence ao domínio de gestão local do JWKS.

## Entidades e dados

Este domínio **não possui persistência própria** nem entidades de negócio armazenadas. Seus
"dados" são o contrato do token de entrada e o principal autenticado de saída.

**Contrato de entrada — cabeçalho HTTP:** `Authorization: Bearer <JWT>`, onde `<JWT>` é um
access token emitido pelo Keycloak.

**Claims exigidos/consumidos do token:**
- `iss` — emissor; validado contra o realm esperado (regra 3).
- `aud` — audience; deve conter o valor esperado (regra 4).
- `exp`, `nbf`, `iat` — validade temporal; validados com clock skew (regra 6).
- `kid` (no cabeçalho do token) — seleciona a chave pública para verificar a assinatura
  (regra 2).

**Principal autenticado de saída:** ao passar por toda a validação, a requisição carrega um
token de autenticação que expõe os claims já validados do JWT (incluindo, além dos acima,
metadados como `sub`, `azp` e `scope`). Esse principal é o que domínios consumidores usam
para servir o recurso protegido — este domínio apenas o produz, não o expõe em endpoint
próprio.

Este domínio **não expõe endpoints HTTP próprios**; ele atua transversalmente sobre todas as
rotas protegidas do resource server.

## Restrições e validações

- O **emissor esperado** (`iss`) e a **audience esperada** (`aud`) são **configuração
  obrigatória**; um valor ausente ou incorreto aqui bloqueia **toda** a autenticação, ainda
  que os tokens sejam legítimos.
- A validação de `aud` é por **contém** (regra 4), e não igualdade — o token pode legitimar
  múltiplas audiences desde que inclua a esperada.
- A tolerância de relógio se aplica **apenas** à validação temporal; não relaxa emissor,
  audience nem assinatura.
- A rota de health é a **única** exceção padrão à exigência de autenticação; qualquer nova
  rota, por padrão, exige token válido.
- Este domínio **não** emite tokens, **não** faz refresh e **não** consulta o Keycloak em
  tempo de requisição; apenas **verifica** o token apresentado contra as chaves locais e as
  regras de claims.

## Variáveis de ambiente do domínio

Cite os valores como identificadores; nunca inclua segredos ou valores reais.

- **`KEYCLOAK_ISSUER_URI`** (propriedade `app.security.jwt.issuer`) — emissor esperado
  (`iss`); deve ser **idêntico** ao `iss` que o Keycloak coloca no token (normalmente
  `https://<host-keycloak>/realms/<realm>`). Governa a regra 3.
- **`APP_B_EXPECTED_AUDIENCE`** (propriedade `app.security.jwt.audience`) — audience
  esperada (`aud`); valor padrão `app-b`. Exige o Audience Mapper no Keycloak (regra 5).
  Governa a regra 4.
- **`APP_B_CLOCK_SKEW_SECONDS`** (propriedade `app.security.jwt.clock-skew-seconds`) —
  tolerância de relógio, em segundos, para a validação temporal; valor padrão `60`. Governa
  a regra 6.

O caminho do arquivo de chaves públicas locais (`APP_B_JWKS_PATH`) é configuração do domínio
de gestão local do JWKS (a fonte de chaves da qual este domínio depende), e não deste
domínio; por isso não é documentado aqui.

## Integrações e dependências externas

- **Keycloak** — emissor dos access tokens validados por este domínio. Determina os valores
  esperados de `iss` (regra 3) e `aud` (regra 4). A audience `app-b` só chega ao token se um
  **Audience Mapper** estiver configurado no client scope do Keycloak (regra 5). O Keycloak
  **não** é consultado por este domínio em tempo de requisição — a validação é offline.
- **Fonte local de chaves públicas (JWKS)** — fornece o conjunto de chaves públicas contra o
  qual a assinatura é verificada. Sem esse material de chaves disponível localmente, nenhuma
  assinatura pode ser conferida e nenhuma requisição autentica. A obtenção, cache e recarga
  desse material pertencem ao domínio de gestão local do JWKS.
- **Pipeline de resource server OAuth2 + biblioteca de verificação JOSE** — provêm,
  respectivamente, o fluxo de validação do JWT nas rotas protegidas e a verificação
  criptográfica da assinatura.

O detalhamento de cada dependência técnica e o impacto de sua ausência estão em
`references/technical-dependencies.md`.

## Referências

- `references/technical-dependencies.md` — dependências técnicas do domínio e o efeito da
  ausência de cada uma.
