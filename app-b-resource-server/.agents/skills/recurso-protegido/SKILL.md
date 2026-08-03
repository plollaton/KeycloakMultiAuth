---
name: recurso-protegido
description: >
  Esta é a documentação autoritativa do domínio Recurso protegido (API demonstrativa).
  Cobre o endpoint REST demonstrativo GET /api/protegido do resource server, acessível
  somente com requisição autenticada, que devolve — como prova da autenticação offline —
  apenas metadados não sensíveis do token validado (mensagem, subject, clientId/azp,
  issuer, audience, scope, expiresAt), nunca o JWT bruto. Documenta o contrato de resposta,
  a natureza demonstrativa (POC) do recurso e a manutenção da documentação de API via
  OpenAPI/Swagger. Carregue em tarefas sobre o recurso/endpoint protegido, /api/protegido,
  corpo/contrato de resposta, metadados do token expostos, subject/clientId/issuer/
  audience/scope/expiresAt, endpoint demonstrativo e documentação OpenAPI/Swagger do App B.
metadata:
  author: clovis-cli
  type: domain-skill
---

# Recurso protegido (API demonstrativa)

> **Manutenção desta skill**
>
> Atualize este documento sempre que o comportamento do domínio mudar de propósito,
> mantendo a skill fiel ao comportamento implementado. Uma mudança deliberada de regra
> (novo campo na resposta, nova rota, nova forma de documentação de API) é maintenance e
> deve ser refletida aqui. Uma divergência semântica entre skill e código sem decisão
> registrada que a resolva é escalada para decisão humana — nunca ajuste a skill nem o
> código por conta própria.

## Visão geral do domínio

Este domínio expõe o **recurso de negócio protegido** do resource server e serve de **prova
concreta da autenticação offline**: um endpoint alcançável somente por uma requisição já
autenticada que, em resposta, devolve os **metadados não sensíveis do token validado**
(subject, clientId, issuer, audience, scope e expiração).

Trata-se de uma **POC** cujo propósito é demonstrar o fluxo de autenticação ponta a ponta —
não há regra de negócio financeiro própria. O endpoint `GET /api/protegido` é
**demonstrativo**: em um sistema real, ele representaria o(s) verdadeiro(s) recurso(s) de
negócio protegido(s) do serviço. Seu valor aqui é confirmar, para quem chama, que o token
apresentado foi aceito e mostrar quais claims foram lidos dele.

O escopo deste domínio **começa** depois que a requisição já foi autenticada — ele **consome**
o resultado da autenticação, não o produz. A exigência e a validação do token Bearer
(assinatura, emissor, audience, validade temporal) pertencem ao domínio de autenticação e
**não** são descritas aqui. A obtenção do material de chaves pertence ao domínio de gestão
local das chaves públicas. Este domínio cuida apenas de **o que o recurso expõe** e de **qual
contrato de resposta** entrega.

## Regras de negócio

As regras abaixo usam numeração própria desta skill, apenas para referência de leitura.

1. **Endpoint único e demonstrativo.** O domínio expõe um único recurso HTTP:
   `GET /api/protegido`. É um endpoint demonstrativo (prova da autenticação offline); em uma
   POC não carrega lógica de negócio própria.

2. **Acesso somente autenticado.** O recurso só é alcançável por uma requisição **autenticada**
   com um token Bearer válido; ele não faz parte das rotas públicas. Uma requisição sem token
   válido é **rejeitada com 401 (não autorizado)** e **não** chega ao recurso. Essa rejeição é
   aplicada pelo mecanismo de autenticação do resource server **antes** deste domínio — o
   recurso em si assume que a requisição já está autenticada e apenas lê os dados do token.

3. **Resposta com sucesso.** Quando a requisição está autenticada, o recurso responde
   **200 (OK)** com um corpo **JSON** contendo os metadados do token (ver "Entidades e dados").

4. **Somente metadados não sensíveis; nunca o token bruto.** A resposta jamais inclui o JWT
   bruto, `client_secret` ou qualquer dado sensível — apenas metadados não sensíveis do token
   validado. Essa é uma regra de segurança da instituição financeira e vale tanto para logs
   quanto para respostas.

5. **Conjunto fixo de campos da resposta.** O corpo devolvido contém exatamente os campos
   `mensagem`, `subject`, `clientId`, `issuer`, `audience`, `scope` e `expiresAt`, nessa ordem,
   cada um com a origem e o significado descritos em "Entidades e dados". Um campo cuja origem
   (claim) esteja ausente no token é devolvido como `null` (nunca omitido nem inventado).

6. **Documentação de API via OpenAPI/Swagger.** Os endpoints deste domínio devem estar
   descritos por documentação **OpenAPI/Swagger gerada a partir do código**, versionada junto
   com ele — o contrato de resposta acompanha automaticamente cada mudança do endpoint. Ao
   alterar rota, campos ou respostas do recurso, a documentação de API é atualizada na mesma
   mudança para não divergir do comportamento real. O detalhe de **como** manter/gerar essa
   documentação é uma preocupação técnica transversal, fora do escopo de negócio desta skill.

## Fluxos e ciclo de vida

**Acesso bem-sucedido ao recurso protegido (caminho principal):**
1. Chega uma requisição `GET /api/protegido` já autenticada (o mecanismo de autenticação
   validou o token Bearer e produziu um principal com os claims validados).
2. O recurso lê os claims do token a partir desse principal autenticado.
3. Monta o corpo de resposta com os metadados não sensíveis (regra 5).
4. Responde **200 (OK)** com o JSON dos metadados.

**Acesso sem autenticação:**
- Requisição a `GET /api/protegido` **sem** token válido → **rejeitada com 401** pelo mecanismo
  de autenticação, **antes** de alcançar o recurso. O recurso não é executado.

Este domínio **não possui estados internos** nem transições próprias: cada requisição é
atendida isoladamente, sem sessão e sem estado persistido entre chamadas.

## Entidades e dados

Este domínio **não possui persistência própria** nem entidades de negócio armazenadas. Seu
"dado" é o **contrato do recurso HTTP**: o pedido autenticado de entrada e o corpo de metadados
de saída.

**Contrato do endpoint:** `GET /api/protegido`
- **Entrada:** requisição autenticada por token Bearer (o cabeçalho `Authorization: Bearer <JWT>`
  é exigido/validado pelo mecanismo de autenticação, não por este domínio).
- **Saída em sucesso:** `200 (OK)` com corpo `application/json`.

**Campos do corpo da resposta** (nesta ordem):

| Campo (chave JSON) | Origem no token | Significado |
|---|---|---|
| `mensagem`   | — (texto fixo) | Confirmação de acesso: `Acesso autorizado - JWT validado offline via JWKS local`. |
| `subject`    | claim `sub`   | Identificador do subject do token (no fluxo `client_credentials`, a service account do cliente). |
| `clientId`   | claim `azp` (authorized party) | Identificador do cliente OAuth2 que obteve o token. |
| `issuer`     | claim `iss`   | Emissor do token (URL do realm), como string; `null` quando ausente. |
| `audience`   | claim `aud`   | Audiences do token, como lista. |
| `scope`      | claim `scope` | Escopos concedidos ao token, como string. |
| `expiresAt`  | claim `exp`   | Instante de expiração do token. |

Todos os campos são **metadados não sensíveis** do token (regra 4). O recurso não deriva,
calcula nem enriquece dados — apenas reflete claims lidos do token já validado.

## Restrições e validações

- O recurso **não realiza validação própria** do token: ele pressupõe que a requisição já foi
  autenticada. Toda a verificação (assinatura, emissor, audience, validade temporal) e a
  eventual rejeição com 401 pertencem ao mecanismo de autenticação, **fora** deste domínio.
- A resposta é **estritamente** o conjunto de campos da regra 5; não expor campos adicionais
  que revelem dados sensíveis (não incluir o JWT bruto, segredos ou claims sensíveis) ao
  evoluir o contrato.
- O recurso é **stateless**: não cria nem depende de sessão e não guarda estado entre
  requisições.
- Sendo uma POC, o endpoint é **demonstrativo**; a lógica de negócio real de recursos
  protegidos ainda não existe e, quando existir, substitui/estende este recurso mantendo a
  regra de expor apenas o necessário.

## Variáveis de ambiente do domínio

Este domínio **não introduz variáveis de ambiente próprias** ligadas ao seu comportamento de
negócio. As configurações que influenciam se uma requisição chega ou não ao recurso (emissor
esperado, audience esperada, clock skew e caminho do JWKS) pertencem aos domínios de
autenticação e de gestão local das chaves e são documentadas por eles; por isso não são
reproduzidas aqui.

## Integrações e dependências externas

- **Mecanismo de autenticação do resource server** — precondição do recurso: só existe uma
  requisição a atender porque o token Bearer já foi validado e um principal com os claims foi
  produzido. Sem ele, o endpoint ficaria inacessível (ou, pior, aberto sem verificação). Este
  domínio **consome** esse resultado; não o produz.
- **Camada web REST (servidor de aplicação HTTP)** — expõe o endpoint `GET /api/protegido` e
  serializa o corpo de metadados em JSON.
- **Documentação OpenAPI/Swagger gerada a partir do código** — forma de documentação de API
  decidida para o projeto; descreve este endpoint e acompanha o código.

O detalhamento de cada dependência técnica e o efeito de sua ausência estão em
`references/technical-dependencies.md`.

## Referências

- `references/technical-dependencies.md` — dependências técnicas do domínio e o efeito da
  ausência de cada uma.
