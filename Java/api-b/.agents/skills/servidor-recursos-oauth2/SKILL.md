---
name: servidor-recursos-oauth2
description: >
  Esta é a documentação autoritativa do domínio Servidor de Recursos OAuth2:
  classificação dos endpoints da aplicação em públicos e protegidos, validação
  de access_tokens emitidos pelo Keycloak (assinatura via JWKS do Keycloak e
  claims iss/exp/aud/iat) na SecurityFilterChain. Usar ao implementar, alterar ou
  revisar a SecurityFilterChain de validação de token, os endpoints GET
  /api/protected, GET /api/public e GET /actuator/health, a liberação pública
  de GET /oauth2/jwks e dos caminhos do Swagger/OpenAPI, ou as regras de
  validação de assinatura e claims de um access_token recebido.
metadata:
  author: clovis-cli
  type: domain-skill
---

# Servidor de Recursos OAuth2

> **Mantendo esta skill**
>
> Atualizar sempre que o comportamento deste domínio mudar de intenção, mantendo a
> skill fiel ao comportamento implementado. Uma divergência semântica entre esta
> skill e o código, sem decisão humana registrada que a resolva, é escalada para
> decisão humana — nunca ajustada unilateralmente na skill ou no código.

## Visão geral do domínio

Este domínio protege os endpoints HTTP desta aplicação, atuando como OAuth2
Resource Server: recebe requisições, classifica cada endpoint como público ou
protegido, e — para os protegidos — valida o `access_token` apresentado pelo
chamador antes de permitir o processamento da requisição. A validação usa a
chave pública publicada pelo próprio Keycloak (não a chave pública publicada
pelo domínio de Gestão de Chaves RSA e Publicação JWKS desta aplicação, que
serve a um propósito diferente: publicar a chave pública própria desta
aplicação).

A aplicação tem um único papel neste cenário: OAuth2 Resource Server,
coberto por este domínio. Este domínio não gera nem envia tokens, apenas
recebe e valida `access_tokens` emitidos pelo Keycloak para quem quer que
chame esta aplicação.

Este domínio é também o dono da `SecurityFilterChain` da aplicação como um
todo: além de decidir quais dos seus próprios endpoints exigem token, ele
incorpora, na mesma cadeia de segurança, a liberação pública de endpoints cuja
regra de negócio pertence a outros domínios ou convenções — o endpoint de
publicação da chave pública (domínio de Gestão de Chaves RSA e Publicação
JWKS) e os caminhos do Swagger/OpenAPI (convenção de documentação de API).
Este domínio implementa essas liberações porque é quem detém a cadeia de
segurança única da aplicação, mas a justificativa de negócio de cada
liberação pertence ao domínio ou à convenção de origem, não a este.

## Regras de negócio

1. A `SecurityFilterChain` classifica cada endpoint HTTP exposto pela
   aplicação em duas categorias, mutuamente exclusivas: público (processado
   sem exigir autenticação) ou protegido (exige um `access_token` válido
   apresentado pelo chamador).
2. `GET /api/protected` é um endpoint protegido: exige um `access_token`
   válido para ser processado.
3. `GET /api/public` é um endpoint público: processado sem exigir
   autenticação.
4. `GET /actuator/health` é público: endpoint de verificação de saúde da
   aplicação, liberado como exemplo de endpoint de infraestrutura sem
   exigência de autenticação.
5. `GET /oauth2/jwks` — o endpoint de publicação da chave pública desta
   aplicação, cuja regra de negócio pertence ao domínio de Gestão de Chaves
   RSA e Publicação JWKS — permanece público dentro desta mesma
   `SecurityFilterChain`. Essa liberação é um pré-requisito daquele domínio,
   implementado por este.
6. Para considerar um `access_token` válido em um endpoint protegido, a
   aplicação valida:
   - a **assinatura** do token, contra o JWK Set publicado pelo **Keycloak**
     (o JWKS do emissor do token — nunca o JWKS próprio desta aplicação,
     usado para outro propósito);
   - a claim `iss` (issuer): deve corresponder ao emissor esperado (o
     Keycloak/realm que esta aplicação reconhece como fonte confiável de
     tokens);
   - a claim `exp` (expiration): deve estar presente — um `access_token`
     sem `exp` é rejeitado — e o token não pode estar expirado no momento
     da requisição;
   - a claim `aud` (audience): deve corresponder ao destinatário esperado por
     esta aplicação;
   - a claim `iat` (issued at): deve estar presente e não pode ser posterior
     ao instante atual da validação (dentro da tolerância de relógio de 60
     segundos aplicada a esta e às demais claims de tempo).
   Os valores concretos de `iss` e `aud` esperados (a URL/identificador do
   realm do Keycloak e o identificador de audiência desta aplicação) são
   específicos do ambiente de implantação e não são fixados pelo material de
   negócio disponível; a exigência de negócio fixada é que essas cinco
   verificações (assinatura, `iss`, `exp`, `aud`, `iat`) ocorram para todo
   `access_token` apresentado a um endpoint protegido.
7. Um `access_token` que falhe em qualquer uma das verificações da regra 6
   (assinatura inválida, emissor incorreto, token expirado, audiência
   incorreta, ou claim `iat` ausente ou posterior ao instante atual) — ou a
   ausência de um `access_token` na requisição — impede o processamento de um
   endpoint protegido: a requisição é rejeitada antes de alcançar a lógica de
   negócio do endpoint.
8. A classificação de público/protegido e as verificações da regra 6 se
   aplicam exclusivamente aos endpoints desta própria aplicação; este domínio
   não valida nem influencia a validação de tokens feita por outras
   aplicações do cenário (por exemplo, uma api-b atuando como seu próprio
   resource server).

## Fluxos e ciclo de vida

Toda requisição HTTP recebida por esta aplicação passa pela
`SecurityFilterChain` antes de alcançar a lógica do endpoint:

1. A cadeia identifica o caminho requisitado e o classifica como público ou
   protegido (regras 1 a 5).
2. Se o caminho é público (`GET /api/public`, `GET /actuator/health`,
   `GET /oauth2/jwks`, e os caminhos do Swagger/OpenAPI liberados pela
   convenção de documentação de API), a requisição é processada sem exigir
   `access_token`.
3. Se o caminho é protegido (`GET /api/protected`), a cadeia exige um
   `access_token` apresentado pelo chamador e executa as verificações da
   regra 6: assinatura contra o JWKS do Keycloak, `iss`, `exp`, `aud` e
   `iat`.
4. Se todas as verificações passam, a requisição é processada normalmente
   pela lógica do endpoint. Se qualquer verificação falha, ou nenhum
   `access_token` foi apresentado, a requisição é rejeitada antes de alcançar
   essa lógica (regra 7).

Os tokens validados aqui são emitidos pelo Keycloak para quem quer que chame
esta aplicação como resource server, qualquer que seja o chamador autorizado
pelo Keycloak.

## Entidades e dados

**Classificação de endpoints desta aplicação**

| Endpoint | Classificação | Regra de negócio |
|---|---|---|
| `GET /api/protected` | Protegido | Exige `access_token` válido (regras 2, 6 e 7) |
| `GET /api/public` | Público | Sem exigência de autenticação (regra 3) |
| `GET /actuator/health` | Público | Sem exigência de autenticação (regra 4) |
| `GET /oauth2/jwks` | Público | Liberado nesta cadeia por pré-requisito do domínio de Gestão de Chaves RSA e Publicação JWKS (regra 5) |

`GET /api/protected` e `GET /api/public` são endpoints de exemplo: o material
de negócio disponível fixa apenas sua classificação de acesso (protegido ou
público), não um contrato de corpo de resposta específico para cada um.

**Access token validado por este domínio** — emitido pelo Keycloak, contendo
ao menos as claims `iss`, `exp`, `aud` e `iat` (regra 6); o formato completo
do token e demais claims que o Keycloak inclua são um contrato do Keycloak,
não deste domínio.

## Restrições e validações

- A validação de assinatura de um `access_token` usa sempre o JWKS do
  **Keycloak**, nunca o JWKS publicado pelo domínio de Gestão de Chaves RSA e
  Publicação JWKS desta aplicação — os dois JWKS têm propósitos distintos e
  não são intercambiáveis.
- As cinco verificações da regra 6 (assinatura, `iss`/`exp`/`aud`/`iat`) são
  cumulativas: a falha de qualquer uma delas é suficiente para rejeitar o
  `access_token`, independentemente do resultado das demais.
- Os valores concretos esperados de `iss` e `aud` são configuração
  específica do ambiente de implantação (qual Keycloak/realm é reconhecido
  como emissor confiável, qual identificador de audiência esta aplicação
  espera); o material de negócio disponível não fixa esses valores — apenas
  que as verificações devem ocorrer.
- Os caminhos do Swagger UI e do contrato OpenAPI também precisam permanecer
  liberados nesta mesma `SecurityFilterChain` para que a documentação de API
  funcione; essa liberação é pré-requisito da convenção de documentação, não
  uma regra de negócio própria deste domínio.

## Integrações e dependências externas

- **Keycloak**: emissor dos `access_tokens` validados por este domínio;
  publica o JWK Set que este domínio consulta para validar a assinatura
  desses tokens, e é a fonte de verdade das claims `iss`/`exp`/`aud`
  esperadas.
- **Gestão de Chaves RSA e Publicação JWKS** (domínio desta mesma
  aplicação): define que seu endpoint de publicação da chave pública
  (`GET /oauth2/jwks`) deve permanecer público; este domínio implementa essa
  liberação na sua `SecurityFilterChain`, mas não é dono da regra que a
  justifica.
- **Convenção de documentação de API (OpenAPI/Swagger)**: exige que os
  caminhos do Swagger UI e do contrato OpenAPI permaneçam públicos nesta
  mesma `SecurityFilterChain`, além de documentar formalmente `GET
  /api/public` e `GET /api/protected`.
- **Spring Boot Actuator**: fornece o endpoint `GET /actuator/health`
  liberado publicamente por este domínio como exemplo de endpoint de
  infraestrutura.

As dependências técnicas detalhadas deste domínio — o que cada uma é e o que
fica comprometido na sua ausência — estão descritas em
`references/technical-dependencies.md`.
