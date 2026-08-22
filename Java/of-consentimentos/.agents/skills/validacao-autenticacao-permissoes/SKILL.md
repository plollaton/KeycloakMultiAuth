---
name: validacao-autenticacao-permissoes
description: >
  Esta é a documentação autoritativa do domínio Validação de Autenticação e Permissões.
  Cobre a validação de requisições Bearer emitidas pelo Keycloak como JWT assinado, a
  verificação da assinatura do token com a chave pública derivada da chave privada local
  da aplicação e a checagem das permissões do portador antes de liberar acesso ao
  endpoint hello world protegido. Use ao tratar autenticação, autorização, token Bearer,
  JWT, assinatura, Keycloak, chave privada, chave pública, certificado, permissões,
  papéis, escopos ou o endpoint hello world desta aplicação.
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
ao certificado registrado no Keycloak, deriva dela a chave pública de verificação e a
usa para verificar a assinatura do token — que o Keycloak emite assinado (JWT, RS256)
com o certificado desta aplicação — antes de qualquer decisão de autorização. O
objetivo desta prova de conceito é validar a viabilidade desse fluxo de
autenticação/autorização; o endpoint protegido não implementa nenhuma funcionalidade de
negócio própria além de servir de superfície mínima para o fluxo.

## Regras de negócio

1. Toda requisição ao recurso protegido deve carregar um token no header
   `Authorization: Bearer <token>`. A ausência desse header resulta em acesso negado,
   sinalizado ao chamador como falha de autenticação (código HTTP 401).
2. O token é emitido pelo Keycloak assinado (JWT, RS256), usando o certificado (par de
   chaves) associado a esta aplicação. A aplicação mantém localmente a chave privada
   correspondente, deriva dela a chave pública de verificação e a utiliza para
   verificar a assinatura do token antes de qualquer decisão de autorização.
3. A verificação bem-sucedida da assinatura do token com a chave pública derivada da
   chave local é o que estabelece, neste domínio, a autenticidade do token perante a
   aplicação. Este é o modelo criptográfico adotado para o domínio; a chave pública
   usada na verificação vem do arquivo de chave local já mantido pela aplicação, sem
   consulta em tempo de execução a um endpoint JWKS exposto pelo Keycloak.
4. Falha na verificação da assinatura do token — chave incompatível, assinatura
   inválida, token corrompido ou malformado — resulta em acesso negado, sinalizado como
   falha de autenticação (código HTTP 401), no mesmo padrão de um token ausente.
5. Um token que falhe em checagens estruturais próprias de um JWT (por exemplo, estar
   expirado) também é tratado como inválido e resulta em acesso negado, com o mesmo
   sinal de falha de autenticação (401); os valores concretos dessas checagens (janela
   de expiração, emissor, audiência esperada) fazem parte da configuração da integração
   com o Keycloak, não são uma regra de negócio deste domínio.
6. Somente após a verificação bem-sucedida da assinatura a aplicação avalia as
   permissões (papéis de realm, papéis de client/resource ou escopos) do portador
   contidas no token verificado. Qualquer permissão presente no token verificado já é
   suficiente para liberar o acesso ao recurso protegido — esta prova de conceito não
   exige uma permissão específica, pois seu objetivo é demonstrar que a checagem de
   permissões ocorre, e não impor um controle de acesso granular. Um portador cujo
   token verificado não carregue nenhuma permissão tem o acesso negado, sinalizado como
   falha de autorização (código HTTP 403) — sinal distinto da falha de autenticação, já
   que neste caso o token foi validado com sucesso e apenas a checagem de permissões
   falhou.
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
4. Header presente ⇒ a aplicação tenta verificar a assinatura do token com a chave
   pública derivada da chave privada local.
5. Falha na verificação da assinatura, ou token estruturalmente inválido ⇒ acesso
   negado, sinalizado como falha de autenticação (401); o fluxo termina aqui.
6. Verificação bem-sucedida ⇒ a aplicação lê as permissões do portador a partir do
   token verificado.
7. Nenhuma permissão presente no token verificado ⇒ acesso negado, sinalizado como
   falha de autorização (403).
8. Ao menos uma permissão presente no token verificado ⇒ acesso liberado; o endpoint
   protegido responde normalmente.

## Entidades e dados

- **Token Bearer (JWT assinado):** entidade transitória, existente apenas durante o
  processamento de cada requisição; não é persistida. Após a verificação da assinatura,
  expõe a identidade do portador e as permissões (papéis de realm, papéis de
  client/resource ou escopos) usadas na autorização — a presença de qualquer uma delas
  já é suficiente para liberar o acesso.
- **Recurso protegido "hello world":** único endpoint de negócio deste domínio nesta
  prova de conceito; não possui corpo de negócio próprio além de demonstrar o fluxo de
  autenticação/autorização. A rota e o verbo HTTP concretos não são fixados pelas fontes
  de negócio, por serem uma decisão de implementação substituível.
- Não há entidades persistentes nem esquema de dados neste domínio: persistência está
  fora de escopo desta prova de conceito.

## Restrições e validações

- O token deve ser transportado no header HTTP `Authorization`, esquema `Bearer`.
- A validação (verificação de assinatura seguida da checagem de permissão) é aplicada a
  toda requisição ao recurso protegido, sem exceção.
- O acesso só é liberado quando as duas condições se cumprem — assinatura do token
  verificável e ao menos uma permissão presente no token verificado; a ausência de
  qualquer uma delas resulta em acesso negado, com sinais distintos ao chamador: falha
  de autenticação (401) quando o token está ausente, é inválido ou não teve a
  assinatura verificada; falha de autorização (403) quando o token é válido mas não
  carrega nenhuma permissão.

## Integrações e dependências externas

- **Keycloak** é o único emissor de identidade e permissões deste domínio: emite o token
  Bearer assinado (JWT, RS256) usando o certificado desta aplicação, e é a origem das
  permissões (papéis/escopos) avaliadas após a verificação da assinatura.

A lista completa das dependências técnicas que este domínio precisa para funcionar —
incluindo o que cada uma afeta se estiver ausente — está em
`references/technical-dependencies.md`.
