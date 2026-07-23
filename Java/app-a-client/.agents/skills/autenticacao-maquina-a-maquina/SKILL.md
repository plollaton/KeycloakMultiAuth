---
name: autenticacao-maquina-a-maquina
description: >
  Esta é a documentação autoritativa do domínio Autenticação Máquina-a-Máquina.
  Descreve como a aplicação autentica a si mesma no Keycloak pelo fluxo OAuth2
  client_credentials (sem usuário ou sessão), obtendo e renovando um access token
  JWT que autoriza chamadas a serviços protegidos a jusante, incluindo o escopo
  app-b.invoke, a descoberta OpenID Connect via issuer-uri e a manutenção do token
  em memória. Carregue esta skill em tarefas sobre autenticação de serviço,
  credencial máquina-a-máquina (M2M), client_credentials, obtenção ou renovação de
  token, access token JWT, client-id/client-secret, escopo do token, issuer-uri ou
  realm do Keycloak.
metadata:
  author: clovis-cli
  type: domain-skill
---

# Autenticação Máquina-a-Máquina

> **Manutenção desta skill**
>
> Atualize este documento sempre que o comportamento do domínio mudar de propósito
> (novo passo do fluxo, novo critério de obtenção/renovação do token, mudança de
> escopo, credencial ou emissor), mantendo a skill fiel ao comportamento
> efetivamente implementado. Refatoração técnica que preserva a regra (renomear,
> extrair configuração, trocar biblioteca equivalente) não exige alteração. Quando a
> skill afirmar X e o código fizer Y sem uma decisão registrada que resolva a
> divergência, trate como lacuna e escale para decisão humana — não ajuste a skill
> nem o código por conta própria.

## Visão geral do domínio

Este domínio autentica **a própria aplicação** (não um usuário final) junto ao
Keycloak, usando o fluxo OAuth2 **`client_credentials`** (máquina-a-máquina, M2M).
Não há usuário, login interativo nem sessão HTTP: a aplicação apresenta a sua
identidade de cliente (um par identificador + segredo) e recebe do Keycloak um
**access token** no formato **JWT**. Esse token é a credencial que autoriza a
aplicação a invocar serviços protegidos a jusante em seu próprio nome.

É o domínio **fundacional** da aplicação: não depende de nenhum outro domínio, e a
credencial que ele produz é o pré-requisito para qualquer integração autenticada
com serviços protegidos.

O escopo deste domínio termina na **produção e disponibilização** do token válido.
O ato de anexar o token a uma requisição concreta a um serviço a jusante e o
consumo desse serviço pertencem ao domínio consumidor, não a este.

## Regras de negócio

1. **Fluxo exclusivamente `client_credentials`.** A concessão (grant) usada é
   `client_credentials`. Não há fluxo de código de autorização, senha de usuário nem
   refresh token baseado em sessão de usuário — a aplicação se autentica apenas com a
   própria identidade de cliente. Preservar este fluxo é uma restrição do projeto;
   qualquer mudança de fluxo exige decisão humana.

2. **Identidade de cliente = identificador + segredo.** A aplicação se apresenta ao
   Keycloak com um **client-id** (identificador público do cliente) e um
   **client-secret** (segredo). Ambos são parametrizáveis por ambiente (ver
   "Variáveis de ambiente do domínio").

3. **Descoberta automática de endpoints (OpenID Connect).** Os endereços do endpoint
   de token e do conjunto de chaves públicas (JWKS) **não** são configurados
   manualmente: são resolvidos automaticamente a partir do **issuer-uri** do realm,
   pelo documento de descoberta padrão OpenID Connect publicado em
   `<issuer-uri>/.well-known/openid-configuration`. Configurar esses endpoints à mão
   contraria a convenção do domínio.

4. **Escopo do token restrito a `app-b.invoke`.** A obtenção do token solicita o
   escopo **`app-b.invoke`**. Esse escopo (definido como client scope no próprio
   Keycloak) delimita o que o token concede — isto é, para quais operações a
   credencial de máquina é autorizada. O token não deve ser emitido com escopo mais
   amplo que o necessário para a integração pretendida.

5. **Token obtido sob demanda e reutilizado enquanto válido.** O token é obtido na
   primeira vez em que uma chamada autenticada a jusante precisa dele e mantido para
   reúso. Enquanto continuar válido, o mesmo token é reaproveitado; não se solicita um
   token novo a cada chamada desnecessariamente.

6. **Renovação automática ao expirar.** Quando o token vigente está expirado (ou
   prestes a expirar), um novo token é obtido automaticamente junto ao Keycloak antes
   da próxima chamada, de forma transparente. Não há renovação manual nem intervenção
   externa: a expiração é detectada e um novo `client_credentials` é executado.

7. **Token mantido apenas em memória.** A credencial obtida é guardada **em memória**
   no processo da aplicação, associada à identidade de cliente. Não há banco de dados
   nem persistência em disco; ao reiniciar a aplicação, um novo token é obtido no
   primeiro uso. Essa escolha é adequada ao caráter de POC do projeto; não introduza
   persistência do token sem decisão humana.

8. **Segredo mantido em configuração externalizada (débito conhecido).** Por decisão
   validada, o `client-secret` é, por ora, fornecido via configuração externalizada
   (variável de ambiente). É uma solução provisória a evoluir depois para um mecanismo
   mais seguro (ex.: cofre de segredos). Ver "Débito técnico conhecido".

## Fluxo e ciclo de vida do token

1. **Gatilho.** Uma operação que exige chamada autenticada a um serviço a jusante
   demanda a credencial.
2. **Verificação da credencial em memória.** Existe token válido em memória? Se sim,
   ele é reutilizado (passo 5).
3. **Obtenção do token.** Não havendo token válido, a aplicação executa o
   `client_credentials` contra o endpoint de token do Keycloak (resolvido por
   descoberta), enviando o client-id, o client-secret e o escopo `app-b.invoke`.
4. **Emissão e armazenamento.** O Keycloak valida a identidade de cliente e emite o
   access token JWT com seu prazo de validade. O token é armazenado em memória,
   associado à identidade de cliente.
5. **Disponibilização.** O token válido fica disponível para o domínio consumidor
   anexá-lo à requisição a jusante.
6. **Expiração e renovação.** Quando o prazo do token vence, ele é considerado
   inválido; a próxima demanda repete a partir do passo 3, obtendo um token novo.

O documento de descoberta e o conjunto de chaves públicas (JWKS) publicados pelo
issuer resolvem tanto o endpoint de token quanto o de chaves. Este domínio usa o
endpoint de token para **obter** a credencial; a **validação** da assinatura do JWT
via JWKS é responsabilidade do serviço protegido a jusante (resource server), não
desta aplicação.

## Entidades e contratos

- **Identidade de cliente (client registration).** Registro lógico da aplicação como
  cliente OAuth2 do realm, reunindo: identificador do cliente (client-id), segredo
  (client-secret), tipo de concessão `client_credentials`, escopo `app-b.invoke` e o
  emissor (issuer-uri) do realm. É a configuração que descreve "quem" a aplicação é
  perante o Keycloak.
- **Access token (JWT).** Credencial emitida pelo Keycloak, com prazo de validade,
  que carrega o escopo concedido. É a saída deste domínio.
- **Endpoint de token do Keycloak.** Endereço (resolvido por descoberta a partir do
  issuer-uri) onde o `client_credentials` é executado para emitir o token.
- **Documento de descoberta OpenID Connect.** `<issuer-uri>/.well-known/openid-configuration`,
  publicado pelo realm; fonte dos endereços de token e de JWKS.

## Restrições e validações

- O fluxo `client_credentials` contra o Keycloak é a base do projeto e **deve ser
  preservado**; ajustes ao fluxo dependem de decisão humana.
- Os endpoints de token/JWKS **não** devem ser configurados manualmente — apenas o
  issuer-uri, do qual são derivados.
- O token **não** é persistido fora da memória do processo.
- Segredos reais **nunca** são versionados; os defaults de configuração servem apenas
  a desenvolvimento e devem ser sobrescritos por ambiente.
- Não há suíte de testes automatizados neste domínio (projeto tratado como POC); não
  escreva/exija testes salvo decisão humana em contrário.

## Variáveis de ambiente do domínio

As variáveis abaixo estão diretamente ligadas ao comportamento de negócio deste
domínio (identidade de cliente e emissor da credencial). Os valores default existentes
são **placeholders de desenvolvimento** e devem ser sobrescritos por ambiente; nunca
registre segredos reais.

- **`APP_A_CLIENT_ID`** — identificador do cliente (client-id) usado para a aplicação
  se apresentar ao Keycloak. Default de desenvolvimento: `app-a`.
- **`APP_A_CLIENT_SECRET`** — segredo do cliente (client-secret) usado na obtenção do
  token. O default é um placeholder inseguro, válido apenas em desenvolvimento, e
  **deve** ser substituído fora dele. Nunca versione o valor real.
- **`KEYCLOAK_ISSUER_URI`** — emissor (issuer) do realm alvo do Keycloak, a partir do
  qual os endpoints de token e JWKS são descobertos. Default de desenvolvimento aponta
  para um host de exemplo, inadequado fora de desenvolvimento.

## Integrações e dependências externas

- **Keycloak** — provedor de identidade que emite o access token via
  `client_credentials` e publica a descoberta OpenID Connect. É a dependência externa
  central deste domínio: sem ele a aplicação não obtém credencial e nenhuma chamada a
  jusante é autorizada.

As demais pré-condições técnicas do domínio (biblioteca cliente OAuth2, configuração
externalizada e configuração do cliente no realm) estão detalhadas em
`references/technical-dependencies.md`.

## Débito técnico conhecido

- **Segredo do cliente em configuração externalizada.** Por decisão humana validada,
  o `client-secret` é mantido, por ora, em configuração externalizada (variável de
  ambiente com default no arquivo de configuração), e não em um mecanismo dedicado de
  segredos. Risco conhecido: exposição do segredo caso o valor real seja indevidamente
  fixado ou versionado. Regra autoritativa a que este débito aponta: evoluir para uma
  solução de gestão de segredos mais segura (ex.: cofre de segredos) em rodada futura.
  Até lá, o segredo real nunca deve ser versionado e os defaults servem apenas a
  desenvolvimento.

## Referências

- `references/technical-dependencies.md` — pré-condições técnicas do domínio (o que
  cada dependência é e o que deixa de funcionar na sua ausência).
