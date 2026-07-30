# Dependências técnicas — Autenticação Máquina-a-Máquina

> **Manutenção deste arquivo**
>
> Atualize sempre que uma pré-condição técnica do domínio for adicionada, removida ou
> mudar de natureza. Cada item descreve o que a dependência é e o que deixa de
> funcionar no domínio se ela estiver ausente ou mal configurada. Divergência entre
> o descrito aqui e o comportamento implementado, sem decisão registrada que a
> resolva, é lacuna a escalar para decisão humana.

Cada item lista uma pré-condição técnica necessária para o domínio entregar seu
comportamento completo e utilizável (obter e renovar a credencial de máquina).

- **Keycloak** — provedor de identidade que emite o access token via
  `client_credentials` e expõe a descoberta OpenID Connect
  (`/.well-known/openid-configuration`); sem ele a aplicação não obtém credencial e
  nenhuma chamada a jusante é autorizada.

- **Spring Security OAuth2 Client** (`spring-boot-starter-oauth2-client`) — implementa
  o gerenciamento do client autorizado e a obtenção/renovação do token, incluindo a
  autenticação do cliente por `private_key_jwt` (assinatura da `client_assertion` via
  Nimbus JOSE, já transitivo); sem ele o fluxo M2M não existe.

- **Par de chaves da aplicação persistido (keystore PKCS12)** — a aplicação gera no boot
  (se ausente) e persiste um par de chaves RSA em keystore, do qual deriva a chave privada
  corrente para assinar a `client_assertion` (`private_key_jwt`) e as chaves públicas
  publicadas no JWKS. Sem o keystore acessível (leitura/escrita) e sua senha, a aplicação
  não consegue assinar a asserção nem manter o `kid` estável entre reinícios, e a
  autenticação do cliente falha.

- **Endereço JWKS da aplicação alcançável pelo Keycloak** (`GET /.well-known/jwks.json`) —
  publica as chaves públicas correntes (corrente e anterior após rotação) sem material
  privado. O Keycloak precisa alcançar esse endereço pela rede a cada renovação para
  validar a `client_assertion`; se inacessível ou desatualizado, a validação da asserção
  falha e o token não é emitido.

- **Configuração externalizada por variáveis de ambiente** (`APP_A_CLIENT_ID`,
  `APP_A_KEYSTORE_LOCATION`, `APP_A_KEYSTORE_PASSWORD`, `KEYCLOAK_ISSUER_URI`) — define o
  identificador do cliente, a localização e a senha do keystore que guarda o par de chaves,
  e o realm/issuer alvo; se ausente, cai nos defaults do `application.yml`, inadequados fora
  de desenvolvimento. Não há mais `APP_A_CLIENT_SECRET` (a autenticação passou a
  `private_key_jwt`).

- **Configuração do cliente no realm do Keycloak** — o cliente correspondente ao
  `client-id` precisa existir no realm alvo com a concessão `client_credentials`
  habilitada (service account ativa), com o client scope `app-b.invoke` definido e
  associado ao cliente e com **autenticação Signed JWT (`private_key_jwt`)** configurada,
  usando **"Use JWKS URL"** apontando para o endereço JWKS da App A
  (`GET /.well-known/jwks.json`) — não mais um segredo compartilhado. Sem essa configuração
  no lado do Keycloak, a validação da `client_assertion` ou a emissão do token falha, ou o
  token é emitido sem o escopo esperado, e as chamadas autorizadas por ele são recusadas a
  jusante. Esta pré-condição vive no Keycloak, fora do repositório da aplicação, e não é
  verificável apenas pela configuração local.
