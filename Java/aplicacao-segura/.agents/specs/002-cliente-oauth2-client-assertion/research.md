# Research: Cliente OAuth2 com Client Assertion JWT

## Mecanismo de autenticação do client junto ao Keycloak

**Contexto.** A skill de domínio exige montar um `client_assertion` JWT (claims `iss`, `sub`,
`aud`, `exp`, `iat`, `jti`, assinado em RS256) e trocá-lo por um `access_token` via
`POST /oauth2/token` com `grant_type=client_credentials`. O `AGENTS.md` já fixa
`spring-boot-starter-oauth2-client` como dependência da stack, sem reserva.

**Alternativas:**

- **Suporte nativo do `spring-boot-starter-oauth2-client` para `private_key_jwt`** — um
  `ClientRegistration` com `ClientAuthenticationMethod.PRIVATE_KEY_JWT`, associado a um
  `NimbusJwtClientAuthenticationParametersConverter` no token response client do grant
  `client_credentials`. O próprio converter monta e assina o `client_assertion` com as claims
  exigidas pela RFC 7523 a partir dos dados do `ClientRegistration` e de um resolver de `JWK`.
- **Chamada manual** — montar o JWT diretamente com Nimbus (sem usar a camada de client OAuth2 do
  Spring Security) e disparar `POST /oauth2/token` com um `RestClient` genérico.

**Decisão:** suporte nativo do `spring-boot-starter-oauth2-client` para `private_key_jwt`.

**Confirmação:** `spring-boot-starter-oauth2-client` está fixado sem ressalva em `AGENTS.md`; a
autenticação de client via JWT assinado (`private_key_jwt`) é o caso de uso documentado desse
módulo do Spring Security para exatamente o cenário da RFC 7523 descrito pela skill de domínio.
Optar pela chamada manual deixaria essa dependência já fixada sem função nesta aplicação (nenhum
outro domínio a consome), o que não é uma leitura razoável de uma dependência declarada sem
reserva.

**Consequências:** a montagem das claims (`iss`, `sub`, `aud`, `exp`, `iat`, `jti`) fica a cargo do
converter do framework, não de código próprio desta unidade; qualquer ajuste futuro nas claims
exigidas passaria por configuração do `ClientRegistration`/converter, não por um builder de JWT
mantido manualmente.

## Origem da chave usada para assinar o `client_assertion`

**Contexto.** O `NimbusJwtClientAuthenticationParametersConverter` precisa de um resolver
(`Function<ClientRegistration, JWK>`) que devolva a chave de assinatura. O domínio "Gestão de
Chaves RSA e Publicação JWKS" já publica essa chave como bean `RSAKey activeRsaKey()` em
`RsaKeyPairConfig`.

**Alternativas:**

- **Reaproveitar o bean `activeRsaKey()`** — o resolver devolve diretamente a instância injetada
  desse bean.
- **Ler os arquivos `api-a-private.pem`/`api-a-cert.pem` novamente nesta unidade** — duplicaria a
  lógica de carregamento já implementada por `RsaKeyPairConfig`.

**Decisão:** reaproveitar o bean `activeRsaKey()`.

**Confirmação:** o bean já existe no contexto Spring desta mesma aplicação e é exatamente a chave
que a skill de domínio exige para a assinatura; duplicar o carregamento criaria duas fontes
independentes de verdade para o mesmo par de chaves, com risco de divergirem entre si (ex.: em uma
rotação de arquivo sem reinício coordenado das duas leituras).

**Consequências:** este domínio depende do bean público de `RsaKeyPairConfig`
(`com.aplicacaosegura.jwks`) permanecer estável em nome e tipo; uma mudança de assinatura desse
bean pelo domínio 1 exige ajuste correspondente aqui.

## Disponibilização do `access_token` para uso futuro

**Contexto.** A skill de domínio fixa que a funcionalidade obrigatória termina na obtenção do
`access_token`, mantendo-o disponível para uso em chamadas subsequentes a outras aplicações —
chamadas essas que não fazem parte do escopo desta unidade.

**Alternativas:**

- **`OAuth2AuthorizedClientManager`** — bean padrão do `spring-boot-starter-oauth2-client`
  (`AuthorizedClientServiceOAuth2AuthorizedClientManager` +
  `ClientCredentialsOAuth2AuthorizedClientProvider`) que qualquer componente futuro pode chamar
  para obter um `access_token` válido pelo id da registration, com renovação automática quando
  expirado.
- **Serviço próprio com cache manual** — uma classe desta unidade que executa a troca, guarda o
  `access_token` em um campo/cache próprio e expõe um método de acesso, reimplementando o controle
  de expiração.

**Decisão:** `OAuth2AuthorizedClientManager`.

**Confirmação:** é o mecanismo padrão do `spring-boot-starter-oauth2-client` (já fixado na stack)
para exatamente este propósito — obter e reutilizar um `access_token` de um grant
`client_credentials` — e evita reimplementar controle de expiração/renovação que o framework já
resolve.

**Consequências:** um componente futuro que precise chamar outra aplicação (ex.: api-b) injeta o
`OAuth2AuthorizedClientManager` e solicita o `access_token` pelo id da registration configurado
aqui, sem depender de nenhuma classe própria desta unidade além dessa configuração.

## Dependência direta Nimbus JOSE+JWT após o starter `oauth2-client`

**Contexto.** O domínio "Gestão de Chaves RSA e Publicação JWKS" já traz `com.nimbusds:nimbus-jose-jwt`
como dependência Maven direta, usada por `RsaKeyPairConfig`. O `spring-boot-starter-oauth2-client`,
adicionado por este domínio, traz a mesma biblioteca transitivamente.

**Alternativas:**

- **Manter a dependência direta** — sem alteração no `pom.xml` além da adição do novo starter.
- **Remover a dependência direta**, passando a depender apenas da versão transitiva trazida pelo
  starter.

**Decisão:** manter a dependência direta.

**Confirmação:** `RsaKeyPairConfig` já importa classes do Nimbus diretamente; ambas as declarações
são gerenciadas pelo mesmo BOM do `spring-boot-starter-parent`, então não há conflito de versão.
Remover uma dependência declarada explicitamente por um domínio que a usa diretamente, só porque
outro domínio passou a trazê-la transitivamente, tornaria implícita uma dependência que o código
usa de forma direta.

**Consequências:** nenhuma mudança no `pom.xml` do domínio 1; o novo starter é adicionado como
entrada independente.
