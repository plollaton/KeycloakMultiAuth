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
  o gerenciamento do client autorizado e a obtenção/renovação do token; sem ele o
  fluxo M2M não existe.

- **Configuração externalizada por variáveis de ambiente** (`APP_A_CLIENT_ID`,
  `APP_A_CLIENT_SECRET`, `KEYCLOAK_ISSUER_URI`) — define credenciais e o realm/issuer
  alvo; se ausente, cai nos defaults do `application.yml`
  (`app-a`/`changeit`/`keycloak.example.com`), inadequados fora de desenvolvimento.

- **Configuração do cliente no realm do Keycloak** — o cliente correspondente ao
  `client-id` precisa existir no realm alvo com a concessão `client_credentials`
  habilitada (service account ativa) e com o client scope `app-b.invoke` definido e
  associado ao cliente. Sem essa configuração no lado do Keycloak, a emissão do token
  falha ou o token é emitido sem o escopo esperado, e as chamadas autorizadas por ele
  são recusadas a jusante. Esta pré-condição vive no Keycloak, fora do repositório da
  aplicação, e não é verificável apenas pela configuração local.
