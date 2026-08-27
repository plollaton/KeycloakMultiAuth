---
name: acesso-cruzado-api-externa
description: >
  Esta é a documentação autoritativa do domínio Acesso Cruzado a Aplicação
  Externa: o endpoint público GET /api/cross, que aciona uma chamada de saída
  a GET /api/protected de uma aplicação externa usando um access_token obtido
  junto ao Keycloak. Usar ao implementar, alterar ou revisar o endpoint
  GET /api/cross, a chamada de saída a uma aplicação externa, a propriedade
  app.cross.external-base-url, ou o tratamento de sucesso/falha dessa chamada.
metadata:
  author: clovis-cli
  type: domain-skill
---

# Acesso Cruzado a Aplicação Externa

> **Mantendo esta skill**
>
> Atualizar sempre que o comportamento deste domínio mudar de intenção, mantendo a
> skill fiel ao comportamento implementado. Uma divergência semântica entre esta
> skill e o código, sem decisão humana registrada que a resolva, é escalada para
> decisão humana — nunca ajustada unilateralmente na skill ou no código.

## Visão geral do domínio

Este domínio expõe um único endpoint, `GET /api/cross`, que demonstra o cenário de
acesso cruzado entre aplicações do material de negócio: usando o `access_token`
obtido pelo domínio Cliente OAuth2 com Client Assertion JWT junto ao Keycloak, a
própria aplicação chama `GET /api/protected` de uma aplicação externa (cenário
`servico-a` → `api-b`), apresentando esse token como cliente daquela API.

`GET /api/cross` é processado sem exigir autenticação de quem o chama — a
autenticação relevante é a que esta aplicação apresenta, como client, à aplicação
externa, não a que o chamador de `GET /api/cross` apresenta a esta aplicação. A
liberação desse caminho como público vive na `SecurityFilterChain` do domínio
Servidor de Recursos OAuth2, que a implementa como pré-requisito deste domínio,
da mesma forma como já faz para `GET /oauth2/jwks`.

## Regras de negócio

1. `GET /api/cross` é um endpoint público: processado sem exigir autenticação do
   chamador.
2. Ao ser chamado, o endpoint obtém um `access_token` junto ao Keycloak através do
   `OAuth2AuthorizedClientManager` já configurado pelo domínio Cliente OAuth2 com
   Client Assertion JWT, e usa esse token para chamar `GET /api/protected` da
   aplicação externa configurada na propriedade `app.cross.external-base-url`.
3. Se a obtenção do `access_token` e a chamada de saída são aceitas pela aplicação
   externa, o endpoint responde `200` com um corpo `CrossAccessResponse`
   (`status: "ok"`).
4. Se a obtenção do `access_token` falha, ou a chamada de saída falha (token
   rejeitado, aplicação externa inacessível, ou qualquer outro erro na chamada), o
   endpoint responde `502`, sem expor o `access_token` nem material de chave
   privada no corpo da resposta.
5. A propriedade `app.cross.external-base-url` não tem valor padrão em nenhum
   `application.yml`: é resolvida apenas pela variável informada na linha de
   comando de inicialização da aplicação.

## Fluxos e ciclo de vida

1. Um chamador executa `GET /api/cross`, sem apresentar `access_token` próprio.
2. A aplicação obtém um `access_token` junto ao Keycloak, reaproveitando o
   `OAuth2AuthorizedClientManager` do domínio Cliente OAuth2 com Client Assertion
   JWT (regra 2).
3. A aplicação chama `GET /api/protected` da aplicação externa configurada em
   `app.cross.external-base-url`, apresentando esse `access_token`.
4. Se a chamada é aceita, o endpoint responde `200` com `CrossAccessResponse`
   (regra 3). Se a obtenção do token ou a chamada falham, o endpoint responde
   `502` (regra 4).

## Entidades e dados

**`CrossAccessResponse`** — corpo de sucesso de `GET /api/cross`:

| Campo | Descrição |
|---|---|
| `status` | Resultado do acesso cruzado; valor `"ok"` na única resposta de sucesso (`200`) definida por este domínio |

**Classificação do endpoint deste domínio**

| Endpoint | Classificação | Regra de negócio |
|---|---|---|
| `GET /api/cross` | Público | Sem exigência de autenticação do chamador (regra 1); aciona chamada de saída a uma aplicação externa (regras 2 a 4) |

## Restrições e validações

- A resposta de `GET /api/cross`, em qualquer código de status, nunca expõe o
  `access_token` obtido nem material de chave privada.
- A liberação de `GET /api/cross` como caminho público na `SecurityFilterChain`
  pertence ao domínio Servidor de Recursos OAuth2, que a implementa como
  pré-requisito deste domínio — este domínio não define nem altera essa cadeia.
- O `access_token` usado na chamada de saída é obtido pelo mesmo
  `OAuth2AuthorizedClientManager` configurado pelo domínio Cliente OAuth2 com
  Client Assertion JWT; este domínio não monta nem assina o `client_assertion`
  por conta própria.
- `GET /api/cross` integra a documentação OpenAPI/Swagger do projeto, como os
  demais endpoints expostos pela aplicação.

## Integrações e dependências externas

- **Cliente OAuth2 com Client Assertion JWT** (domínio desta mesma aplicação):
  fornece o `OAuth2AuthorizedClientManager` já configurado, usado para obter o
  `access_token` apresentado na chamada de saída.
- **Servidor de Recursos OAuth2** (domínio desta mesma aplicação): mantém
  `GET /api/cross` público na `SecurityFilterChain` que possui.
- **Aplicação externa** (cenário `servico-a` → `api-b`): expõe `GET /api/protected`
  como seu próprio resource server e valida o `access_token` apresentado; fora do
  controle deste repositório.
- **OpenAPI/Swagger**: documenta o contrato de `GET /api/cross`.

As dependências técnicas detalhadas deste domínio — o que cada uma é e o que fica
comprometido na sua ausência — estão descritas em
`references/technical-dependencies.md`.
