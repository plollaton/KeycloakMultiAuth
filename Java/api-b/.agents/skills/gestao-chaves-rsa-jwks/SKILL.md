---
name: gestao-chaves-rsa-jwks
description: >
  Esta é a documentação autoritativa do domínio Gestão de Chaves RSA e Publicação
  JWKS: carregamento do par de chaves RSA fixo da aplicação (2048 bits, RS256, kid
  derivado do certificado), publicação da chave pública via endpoint JWKS e rotação
  de chaves refletida automaticamente. Usar ao implementar, alterar ou revisar o
  carregamento da chave RSA a partir dos arquivos PEM da aplicação, o endpoint
  GET /oauth2/jwks, o formato do JWKSet, ou a liberação pública desse endpoint na
  cadeia de segurança.
metadata:
  author: clovis-cli
  type: domain-skill
---

# Gestão de Chaves RSA e Publicação JWKS

> **Mantendo esta skill**
>
> Atualizar sempre que o comportamento deste domínio mudar de intenção, mantendo a
> skill fiel ao comportamento implementado. Uma divergência semântica entre esta
> skill e o código, sem decisão humana registrada que a resolva, é escalada para
> decisão humana — nunca ajustada unilateralmente na skill ou no código.

## Visão geral do domínio

Este domínio garante que a aplicação tenha, em todo momento, um par de chaves RSA
próprio e que a chave pública correspondente esteja disponível publicamente no
formato JWK Set, para que o Keycloak (ou qualquer outro consumidor externo) possa
validar assinaturas produzidas com a chave privada da aplicação.

A aplicação faz parte de um cenário com múltiplas aplicações (por exemplo
servico-a, api-b), cada uma responsável por manter seu próprio par de chaves e
publicar seu próprio JWKS; este domínio cobre apenas o par de chaves e o JWKS
desta aplicação, não os de outras aplicações do cenário. A definição de quais
endpoints exigem autenticação pertence ao domínio do servidor de recursos
OAuth2; este domínio apenas garante que seu próprio endpoint de publicação da
chave pública seja público.

## Regras de negócio

1. Na inicialização da aplicação, o par de chaves RSA ativo (2048 bits,
   algoritmo RS256) é carregado do par fixo de arquivos mantidos nos recursos
   da própria aplicação: a chave privada do arquivo `api-a-private.pem`
   (formato PKCS8) e a chave pública do certificado `api-a-cert.pem` (formato
   X.509).
2. O par de chaves recebe um identificador (`kid`) único, igual ao número de
   série do certificado `api-a-cert.pem`. Esse identificador acompanha a chave
   pública publicada e permite ao consumidor (Keycloak) diferenciar chaves ao
   longo do tempo — é a base do suporte à rotação de chaves.
3. A chave privada nunca é exposta pela aplicação: é mantida apenas
   internamente.
4. O par de chaves ativo é sempre o par fixo mantido pelos arquivos
   `api-a-private.pem` e `api-a-cert.pem`, incluídos nos recursos da
   aplicação; não há geração de um par de chaves aleatório nem outra fonte
   alternativa configurável.
5. A chave pública correspondente ao par de chaves ativo é exposta via um
   endpoint HTTP no formato JWK Set (ver "Entidades e dados").
6. O endpoint de publicação da chave pública é público: pode ser consultado sem
   autenticação. Essa exigência é uma regra de negócio deste domínio (não uma
   opção de configuração), pois o Keycloak precisa conseguir buscar a chave
   pública sem se autenticar previamente.
7. Rotação de chaves: ao substituir os arquivos `api-a-private.pem` e
   `api-a-cert.pem` por um novo par de chaves e reiniciar a aplicação, o
   endpoint de publicação passa a refletir automaticamente a nova chave
   pública e o novo `kid`, sem exigir nenhuma sincronização manual adicional —
   o endpoint sempre publica a chave correspondente ao par de arquivos
   atualmente presente na aplicação.

## Fluxos e ciclo de vida

O par de chaves desta aplicação tem uma única fonte: os arquivos
`api-a-private.pem` e `api-a-cert.pem` mantidos nos recursos da aplicação,
lidos a cada inicialização.

A rotação de chaves ocorre quando esses arquivos são substituídos por um novo
par e a aplicação é reiniciada. O endpoint de publicação da chave pública não
mantém estado próprio: ele lê e serializa o par de chaves atualmente carregado
a cada consulta, o que garante que a rotação seja refletida automaticamente,
sem exigir um passo adicional de republicação.

## Entidades e dados

**Par de chaves RSA da aplicação** — atributos: chave privada (uso interno,
nunca exposta), chave pública, identificador (`kid`) único, tamanho de 2048
bits, algoritmo RS256, uso `sig` (assinatura).

**Contrato do endpoint de publicação da chave pública**

`GET /oauth2/jwks` — endpoint público (sem autenticação).

Resposta (200, JSON) no formato JWK Set, contendo apenas a chave pública ativa:

```json
{
  "keys": [
    {
      "kty": "RSA",
      "kid": "<identificador único do par de chaves ativo>",
      "use": "sig",
      "alg": "RS256",
      "n": "<módulo RSA, codificado em Base64URL>",
      "e": "<expoente público, codificado em Base64URL>"
    }
  ]
}
```

Os campos `kty`, `use` e `alg` têm valor fixo (`RSA`, `sig`, `RS256`,
respectivamente, refletindo as regras 1 e 5); `kid` reflete o identificador
único do par de chaves ativo (regra 2); `n` e `e` são o módulo e o expoente
público da chave RSA ativa. O array `keys` contém apenas a chave pública ativa
desta aplicação — nunca a chave privada, e nesta POC nunca chaves de outras
aplicações do cenário nem chaves de rotações anteriores.

## Restrições e validações

- A chave privada não pode ser incluída, sob nenhuma circunstância, na resposta
  do endpoint de publicação nem em qualquer outro contrato exposto por este
  domínio.
- O par de chaves deve ter exatamente 2048 bits e algoritmo RS256; nenhum outro
  tamanho ou algoritmo é suportado por esta regra de negócio.
- O identificador (`kid`) do par de chaves deve ser único; nesta aplicação
  corresponde ao número de série do certificado `api-a-cert.pem`.

## Integrações e dependências externas

- **Keycloak**: consumidor externo do endpoint de publicação da chave pública;
  dependendo do cenário, pode usar a chave pública publicada para validar
  assinaturas produzidas com a chave privada da aplicação. Este domínio não
  implementa nenhuma chamada ao Keycloak — apenas expõe o contrato que o
  Keycloak consulta.
- **Nimbus JOSE+JWT**: biblioteca usada para montar o par de chaves RSA e
  serializar a chave pública ativa no formato JWK Set exigido pelo contrato do
  endpoint.
- **OpenAPI/Swagger**: o endpoint de publicação da chave pública é documentado
  nesse formato, junto dos demais endpoints REST do projeto.
- Este domínio depende de seu próprio endpoint de publicação permanecer
  liberado de autenticação na cadeia de segurança da aplicação — a definição de
  quais outros endpoints exigem ou não autenticação pertence ao domínio do
  servidor de recursos OAuth2, mas a liberação deste endpoint específico é um
  pré-requisito deste domínio.

As dependências técnicas detalhadas deste domínio — o que cada uma é e o que
fica comprometido na sua ausência — estão descritas em
`references/technical-dependencies.md`.
