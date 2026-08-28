---
name: cliente-oauth2-client-assertion
description: >
  Esta é a documentação autoritativa do domínio Cliente OAuth2 com Client
  Assertion JWT (RFC 7523): geração do JWT client_assertion assinado com a
  chave privada RSA da aplicação e troca desse JWT por um access_token junto
  ao Keycloak via client_credentials. Usar ao implementar, alterar ou revisar
  a montagem do client_assertion, as claims iss/sub/aud/exp/iat/jti, a chamada
  a POST /oauth2/token do Keycloak, o parâmetro client_assertion_type, ou o
  uso do access_token obtido para chamar outras aplicações.
metadata:
  author: clovis-cli
  type: domain-skill
---

# Cliente OAuth2 com Client Assertion JWT

> **Mantendo esta skill**
>
> Atualizar sempre que o comportamento deste domínio mudar de intenção, mantendo a
> skill fiel ao comportamento implementado. Uma divergência semântica entre esta
> skill e o código, sem decisão humana registrada que a resolva, é escalada para
> decisão humana — nunca ajustada unilateralmente na skill ou no código.

## Visão geral do domínio

Este domínio autentica a própria aplicação como client OAuth2 perante o Keycloak,
usando o fluxo `client_credentials` com autenticação do client via JWT assertion
(RFC 7523), em vez de um `client_secret` compartilhado. A aplicação monta um JWT
(`client_assertion`) assinado com sua própria chave privada RSA e o envia ao
Keycloak em troca de um `access_token`.

A assinatura do `client_assertion` depende da chave privada mantida pelo domínio
de Gestão de Chaves RSA e Publicação JWKS: este domínio consome essa chave (e o
identificador de chave — `kid` — vigente), mas não gera nem armazena chaves por
conta própria. Do lado do Keycloak, a validação da assinatura do
`client_assertion` depende de o Keycloak conseguir buscar a chave pública
correspondente no endpoint de publicação daquele mesmo domínio — este domínio
apenas consome esse contrato, não o expõe.

A obtenção do `access_token` é a funcionalidade obrigatória deste domínio; usar
esse `access_token` para chamar uma outra aplicação (por exemplo, uma api-b) é
apenas o cenário de contexto que motiva o fluxo, e não uma funcionalidade
obrigatória desta aplicação — o material de negócio não descreve, entre suas
funcionalidades obrigatórias, a implementação dessa chamada subsequente.

Este domínio expõe um único endpoint REST, `GET /diagnostics/oauth2-client-assertion`,
exclusivamente diagnóstico: aciona manualmente a montagem do `client_assertion` e a
troca por `access_token` junto ao Keycloak, e responde confirmando o sucesso ou a
falha dessa troca. Não é uma funcionalidade de negócio — existe apenas para permitir
a validação manual do fluxo sem depender de um consumidor real do `access_token` — e
nunca devolve o `access_token` obtido nem qualquer material da chave privada no corpo
da resposta.

## Regras de negócio

1. Para obter um `access_token`, a aplicação monta um JWT (`client_assertion`)
   assinado em RS256 com a chave privada RSA atualmente mantida pelo domínio de
   Gestão de Chaves RSA e Publicação JWKS.
2. O `client_assertion` carrega as claims `iss`, `sub`, `aud`, `exp`, `iat` e
   `jti`:
   - `iss` (issuer) e `sub` (subject) identificam esta aplicação como o client
     OAuth2 que está se autenticando — ambas carregam o identificador do client
     (`client_id`) registrado no Keycloak para esta aplicação;
   - `aud` (audience) identifica o destinatário pretendido do JWT — o endpoint
     de token do Keycloak (ou o identificador do emissor que o Keycloak exige),
     conforme a semântica padrão da RFC 7523;
   - `exp` (expiration) e `iat` (issued at) delimitam a janela de validade do
     `client_assertion`;
   - `jti` (JWT ID) é um identificador único por `client_assertion` gerado,
     usado para impedir que o mesmo JWT seja reaproveitado (replay) em uma nova
     chamada.
   Os valores concretos de `iss`/`sub` (o `client_id` registrado no Keycloak) e
   de `aud` (o identificador do endpoint de token do Keycloak) são específicos
   do ambiente de implantação e não são fixados pelo material de negócio
   disponível; o que este domínio fixa é que essas claims devem estar presentes
   e carregar esses papéis semânticos. Da mesma forma, a duração concreta da
   janela de validade (diferença entre `exp` e `iat`) não é fixada pelo
   material de negócio — a exigência de negócio é a presença das duas claims,
   não um valor específico de duração.
3. O `client_assertion` montado é enviado ao Keycloak em uma requisição
   `POST /oauth2/token` com os seguintes parâmetros:
   - `grant_type=client_credentials`;
   - `client_id=<client_id registrado no Keycloak para esta aplicação>`;
   - `client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer`;
   - `client_assertion=<JWT assinado no passo 1>`.
4. O Keycloak, ao receber essa requisição, consulta o endpoint de publicação da
   chave pública desta aplicação (domínio de Gestão de Chaves RSA e Publicação
   JWKS) para validar a assinatura do `client_assertion` recebido antes de
   emitir o `access_token`. Este domínio não implementa essa validação — apenas
   depende dela ocorrer do lado do Keycloak para que a troca seja concluída.
5. Em caso de sucesso, o Keycloak emite um `access_token`. O formato exato da
   resposta do endpoint de token é um contrato do Keycloak, não deste domínio.
6. O `access_token` obtido fica disponível para a aplicação usar em chamadas
   subsequentes a outras aplicações (por exemplo, uma api-b) que atuem como
   resource server. Implementar essa chamada subsequente a uma API específica
   não é uma funcionalidade obrigatória deste domínio nesta aplicação — o
   comportamento obrigatório deste domínio termina na obtenção do
   `access_token`.

## Fluxos e ciclo de vida

O fluxo deste domínio ocorre toda vez que a aplicação precisa se autenticar
como client OAuth2 perante o Keycloak (por exemplo, antes de chamar outra
aplicação como resource server):

1. A aplicação monta o `client_assertion`: preenche as claims `iss`, `sub`,
   `aud`, `exp`, `iat`, `jti` (regra 2) e assina o JWT em RS256 usando a chave
   privada e o `kid` atualmente vigentes no domínio de Gestão de Chaves RSA e
   Publicação JWKS.
2. A aplicação envia `POST /oauth2/token` ao Keycloak com os parâmetros da
   regra 3.
3. O Keycloak busca a chave pública desta aplicação no endpoint de publicação
   daquele domínio, valida a assinatura do `client_assertion` recebido e, se
   válida, emite o `access_token` (regras 4 e 5).
4. A aplicação recebe o `access_token` e o mantém disponível para uso em
   chamadas subsequentes a outras aplicações (regra 6).

Como o endpoint de publicação da chave pública sempre reflete a chave
atualmente vigente (rotação automática, regra do domínio de Gestão de Chaves
RSA e Publicação JWKS), uma rotação de chaves nesse domínio não interrompe este
fluxo: basta que o `client_assertion` seja assinado com a chave privada e o
`kid` correspondentes ao par vigente no momento da assinatura, pois é esse par
que o Keycloak encontrará ao consultar o endpoint de publicação.

## Entidades e dados

**Client assertion JWT** — JWT assinado em RS256, com as claims:

| Claim | Papel semântico |
|---|---|
| `iss` | Identificador do client (`client_id`) desta aplicação no Keycloak |
| `sub` | Identificador do client (`client_id`) desta aplicação no Keycloak (mesmo valor de `iss`) |
| `aud` | Identificador do endpoint de token do Keycloak, destinatário pretendido do JWT |
| `exp` | Instante de expiração da validade do `client_assertion` |
| `iat` | Instante de emissão do `client_assertion` |
| `jti` | Identificador único deste `client_assertion`, usado para prevenir reaproveitamento |

**Requisição de troca de token** (contrato consumido, não exposto por esta
aplicação):

`POST /oauth2/token` (Keycloak) — corpo com os parâmetros `grant_type`,
`client_id`, `client_assertion_type` e `client_assertion` descritos na regra 3.

Resposta: um `access_token` emitido pelo Keycloak; o formato exato dessa
resposta é definido pelo contrato do Keycloak, não por este domínio.

## Restrições e validações

- O `client_assertion` deve ser assinado em RS256, com a mesma chave privada
  (e `kid` correspondente) cuja chave pública está publicada, no momento da
  assinatura, pelo domínio de Gestão de Chaves RSA e Publicação JWKS — caso
  contrário, a validação de assinatura no Keycloak falha e o `access_token` não
  é emitido.
- Cada `client_assertion` deve carregar um `jti` único; a estratégia concreta
  de geração desse identificador não é fixada pelo material de negócio
  disponível.
- O endpoint `GET /diagnostics/oauth2-client-assertion` integra a documentação
  OpenAPI/Swagger do projeto, como os demais endpoints expostos pela aplicação.
- A chamada subsequente ao `access_token` obtido, para consumir uma outra
  aplicação como resource server, é informação de cenário/contexto; o material
  de negócio não fixa qual API é chamada nem o contrato dessa chamada — isso
  está fora do escopo obrigatório desta aplicação.

## Integrações e dependências externas

- **Keycloak**: recebe o `client_assertion` em `POST /oauth2/token`, valida sua
  assinatura consultando o endpoint de publicação da chave pública desta
  aplicação e emite o `access_token`. O Keycloak precisa ter esta aplicação
  registrada como client configurado para autenticação via JWT assinado
  (apontando para o endpoint de publicação da chave pública desta aplicação
  como fonte de validação); essa configuração do lado do Keycloak é um
  pré-requisito para que a troca funcione, mas não é implementada por esta
  aplicação.
- **Gestão de Chaves RSA e Publicação JWKS** (domínio desta mesma aplicação):
  fornece a chave privada e o `kid` vigentes usados para assinar o
  `client_assertion`, e expõe o endpoint que o Keycloak consulta para validar
  essa assinatura.
- **Nimbus JOSE+JWT**: biblioteca usada para montar e assinar o
  `client_assertion` no formato JWT exigido pelo Keycloak.

As dependências técnicas detalhadas deste domínio — o que cada uma é e o que
fica comprometido na sua ausência — estão descritas em
`references/technical-dependencies.md`.
