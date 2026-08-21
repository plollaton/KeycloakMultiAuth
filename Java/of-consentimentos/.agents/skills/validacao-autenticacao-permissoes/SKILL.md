---
name: validacao-autenticacao-permissoes
description: >
  Esta é a documentação autoritativa do domínio Validação de Autenticação e Permissões.
  Cobre a validação de requisições Bearer emitidas pelo Keycloak em formato JWE, a
  descriptografia do token com a chave privada local da aplicação e a checagem das
  permissões do portador antes de liberar acesso ao endpoint hello world protegido. Use
  ao tratar autenticação, autorização, token Bearer, JWE, Keycloak, chave privada,
  certificado, permissões, papéis, escopos ou o endpoint hello world desta aplicação.
metadata:
  author: clovis-cli
  type: domain-skill
---

# Validação de Autenticação e Permissões

> **Mantendo esta skill**
>
> Atualize esta skill sempre que o comportamento deste domínio mudar de propósito,
> mantendo-a fiel ao comportamento implementado. Uma divergência semântica entre esta
> skill e o código, sem decisão registrada que a resolva, é escalada para decisão
> humana — nunca ajustada unilateralmente aqui ou no código.

## Visão geral do domínio

Este domínio garante que toda requisição destinada ao recurso protegido desta aplicação
— um endpoint no estilo "hello world", que existe apenas para demonstrar e exercitar o
fluxo — carregue um token Bearer válido emitido pelo Keycloak, e que o portador desse
token possua as permissões necessárias antes de o acesso ser liberado.

A aplicação atua como resource server: mantém localmente a chave privada correspondente
ao certificado registrado no Keycloak e a usa para decifrar o token — que o Keycloak
entrega cifrado no formato JWE (JSON Web Encryption) — antes de qualquer decisão de
autorização. O objetivo desta prova de conceito é validar a viabilidade desse fluxo de
autenticação/autorização; o endpoint protegido não implementa nenhuma funcionalidade de
negócio própria além de servir de superfície mínima para o fluxo.

## Regras de negócio

1. Toda requisição ao recurso protegido deve carregar um token no header
   `Authorization: Bearer <token>`. A ausência desse header resulta em acesso negado,
   sinalizado ao chamador como falha de autenticação (código HTTP 401).
2. O token é emitido pelo Keycloak cifrado no formato JWE, usando o certificado (par de
   chaves) associado a esta aplicação. A aplicação mantém localmente a chave privada
   correspondente e a utiliza para decifrar o token antes de qualquer decisão de
   autorização.
3. A descriptografia bem-sucedida do token com a chave privada é o que estabelece, neste
   domínio, a autenticidade do token perante a aplicação — não é aplicada, além da
   descriptografia, uma verificação adicional de assinatura do token. Este é o modelo
   criptográfico adotado para o domínio, em contraste com um modelo alternativo
   (verificar apenas a assinatura do token com a chave pública do Keycloak, via JWKS,
   sem descriptografia) que foi avaliado e descartado.
4. Falha ao decifrar o token — chave incompatível, token corrompido ou malformado —
   resulta em acesso negado, sinalizado como falha de autenticação (código HTTP 401), no
   mesmo padrão de um token ausente.
5. Um token que falhe em checagens estruturais próprias de um JWT (por exemplo, estar
   expirado) também é tratado como inválido e resulta em acesso negado, com o mesmo
   sinal de falha de autenticação (401); os valores concretos dessas checagens (janela
   de expiração, emissor, audiência esperada) fazem parte da configuração da integração
   com o Keycloak, não são uma regra de negócio deste domínio.
6. Somente após a descriptografia bem-sucedida a aplicação avalia as permissões
   (papéis de realm, papéis de client/resource ou escopos) do portador contidas no token
   decifrado. Qualquer permissão presente no token decifrado já é suficiente para
   liberar o acesso ao recurso protegido — esta prova de conceito não exige uma
   permissão específica, pois seu objetivo é demonstrar que a checagem de permissões
   ocorre, e não impor um controle de acesso granular. Um portador cujo token decifrado
   não carregue nenhuma permissão tem o acesso negado, sinalizado como falha de
   autorização (código HTTP 403) — sinal distinto da falha de autenticação, já que neste
   caso o token foi validado com sucesso e apenas a checagem de permissões falhou.
7. O único recurso deste domínio é o endpoint protegido no estilo "hello world"; ele não
   carrega dado de negócio próprio, servindo apenas para demonstrar que o fluxo de
   autenticação e autorização funciona.
8. Esta prova de conceito não persiste estado: nenhuma informação sobre o portador, o
   token ou a decisão de acesso é armazenada além do processamento da própria
   requisição.

## Fluxos e ciclo de vida

O fluxo de uma requisição ao recurso protegido segue estes passos:

1. A requisição chega ao endpoint protegido ("hello world").
2. A camada de resource server intercepta a requisição e procura o header
   `Authorization: Bearer <token>`.
3. Header ausente ⇒ acesso negado, sinalizado como falha de autenticação (401); o fluxo
   termina aqui.
4. Header presente ⇒ a aplicação tenta decifrar o token (JWE) com a chave privada local.
5. Falha na descriptografia, ou token estruturalmente inválido ⇒ acesso negado,
   sinalizado como falha de autenticação (401); o fluxo termina aqui.
6. Descriptografia bem-sucedida ⇒ a aplicação lê as permissões do portador a partir do
   token decifrado.
7. Nenhuma permissão presente no token decifrado ⇒ acesso negado, sinalizado como falha
   de autorização (403).
8. Ao menos uma permissão presente no token decifrado ⇒ acesso liberado; o endpoint
   protegido responde normalmente.

## Entidades e dados

- **Token Bearer (JWE):** entidade transitória, existente apenas durante o
  processamento de cada requisição; não é persistida. Antes da descriptografia é um
  envelope cifrado; depois de decifrado, expõe a identidade do portador e as permissões
  (papéis de realm, papéis de client/resource ou escopos) usadas na autorização — a
  presença de qualquer uma delas já é suficiente para liberar o acesso.
- **Recurso protegido "hello world":** único endpoint de negócio deste domínio nesta
  prova de conceito; não possui corpo de negócio próprio além de demonstrar o fluxo de
  autenticação/autorização. A rota e o verbo HTTP concretos não são fixados pelas fontes
  de negócio, por serem uma decisão de implementação substituível.
- Não há entidades persistentes nem esquema de dados neste domínio: persistência está
  fora de escopo desta prova de conceito.

## Restrições e validações

- O token deve ser transportado no header HTTP `Authorization`, esquema `Bearer`.
- A validação (descriptografia seguida da checagem de permissão) é aplicada a toda
  requisição ao recurso protegido, sem exceção.
- O acesso só é liberado quando as duas condições se cumprem — token decifrável e ao
  menos uma permissão presente no token decifrado; a ausência de qualquer uma delas
  resulta em acesso negado, com sinais distintos ao chamador: falha de autenticação
  (401) quando o token está ausente, é inválido ou não pôde ser decifrado; falha de
  autorização (403) quando o token é válido mas não carrega nenhuma permissão.

## Integrações e dependências externas

- **Keycloak** é o único emissor de identidade e permissões deste domínio: gera o token
  Bearer cifrado (JWE) usando o certificado desta aplicação, e é a origem das permissões
  (papéis/escopos) avaliadas após a descriptografia.

A lista completa das dependências técnicas que este domínio precisa para funcionar —
incluindo o que cada uma afeta se estiver ausente — está em
`references/technical-dependencies.md`.
