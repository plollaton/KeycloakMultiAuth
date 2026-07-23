# Research: Credencial do cliente por Signed JWT com rotação de chaves

Decisões técnicas não triviais desta unidade, com contexto, alternativas, decisão, base de
confirmação (proveniência) e consequências. Complementa o [`plan.md`](./plan.md).

## Mecanismo de autenticação `private_key_jwt` no Spring Security

**Contexto.** O fluxo `client_credentials` deve deixar de autenticar por segredo compartilhado e
passar a autenticar por asserção JWT assinada (`private_key_jwt`), sem trocar o grant.

**Alternativas:**

- **Suporte nativo do Spring Security** — o token response client do provider `clientCredentials()`
  recebe um `NimbusJwtClientAuthenticationParametersConverter`, que resolve o JWK por
  `ClientRegistration` e injeta os parâmetros `client_assertion_type`/`client_assertion`. O
  registration usa `client-authentication-method: private_key_jwt`.
- **Montar a `client_assertion` manualmente** (assinar o JWT e adicioná-lo ao corpo por conta
  própria) — reimplementa o que o framework já faz; mais código e mais risco de divergir do padrão
  OAuth2.

**Decisão:** suporte nativo do Spring Security com `NimbusJwtClientAuthenticationParametersConverter`
aplicado ao token response client baseado em `RestClient` do fluxo `client_credentials`.

**Base de confirmação:** em uso no código — `spring-boot-starter-oauth2-client` já é dependência
(evidência no `pom.xml` e no `OAuth2RestClientConfig`), e o `spring-security-oauth2-jose` (Nimbus
JOSE) vem transitivo. Decisão firme, sem lacuna.

**Consequências.** Ajuste concentrado no `OAuth2RestClientConfig` (configurar o token response
client do provider) e no `application.yml` (`client-authentication-method: private_key_jwt`, remoção
do `client-secret`). Atenção de versão: na linha do Spring Security empacotada pelo Spring Boot 4.0.6,
o token response client de `client_credentials` é a variante baseada em `RestClient`
(`RestClientClientCredentialsTokenResponseClient`); confirmar o nome exato da classe na versão
empacotada no momento da implementação — é detalhe de versão do framework já adotado, não nova
tecnologia.

## Tipo/algoritmo de chave e formato de armazenamento

**Contexto.** A `client_assertion` precisa de um par de chaves; a chave pública é exposta via JWKS e
buscada pelo Keycloak. A spec delegou o tipo/algoritmo/formato a esta fase.

**Alternativas:**

- **RSA-2048 + RS256, X.509 autoassinado em keystore PKCS12** — padrão amplamente suportado; PKCS12 é
  o formato de keystore default da JVM moderna; casa com o termo "certificado" do pedido.
- **EC (P-256) + ES256** — chaves menores, mas sem ganho relevante para a POC e menos ubíquo em
  configurações default.
- **Guardar a chave como JWK em arquivo JSON** (sem keystore) — simples para o JWKS, mas foge do
  formato de keystore mencionado na decisão humana e do ferramental padrão da JVM.

**Decisão:** RSA-2048, assinatura RS256, chave pública em certificado X.509 autoassinado, entrada
persistida em keystore **PKCS12**. O JWK público (`kty=RSA`, `use=sig`, `alg=RS256`, `kid`) é derivado
da entrada do keystore.

**Base de confirmação:** decisão humana (lacuna `persistencia-chave-privada`: "keystore/arquivo") fixa
a persistência em keystore; o tipo/algoritmo é escolha de engenharia com respaldo em padrão de
indústria e no default do Keycloak para Signed JWT, sem introduzir dependência nova (JDK + Nimbus já
presentes). A spec explicitou que estes parâmetros seriam decididos no plan/research.

**Consequências.** Uso do `KeyStore` da JDK (PKCS12) para persistir/carregar e do Nimbus
(`RSAKey`/`JWKSet`) para expor o JWKS. Sem novas dependências.

## Rotação e janela de cache do JWKS

**Contexto.** Ao rotacionar, o Keycloak pode ainda ter o JWKS anterior em cache; assinar já com a nova
chave provocaria falha de validação até ele re-buscar o endereço.

**Alternativas:**

- **Publicar corrente + anterior no JWKS, assinar com a corrente** — o Keycloak valida tanto com a
  chave nova (após re-buscar) quanto com a anterior (enquanto o cache não expira). Janela de falha
  eliminada na prática.
- **Publicar só a corrente e assinar com ela** — simples, mas cria a janela de falha durante o cache.
- **Atraso na troca da chave de assinatura** (publicar a nova, só assinar com ela após um tempo) —
  mais complexo e dependente de temporização.

**Decisão:** o keystore mantém **corrente** e **anterior**; o `JWKSet` publica ambas as chaves
públicas; a assinatura usa sempre a **corrente**. Na rotação: corrente → anterior, nova → corrente
(a "anterior" anterior é descartada). Mantêm-se no máximo duas chaves.

**Base de confirmação:** decisão de engenharia derivada do risco já registrado na spec ("Janela de
inconsistência na rotação"); sem nova dependência.

**Consequências.** O componente de chaves gerencia dois aliases no keystore e um marcador de
corrente. Simples e suficiente para a POC; não há expiração temporal automática das chaves antigas
(descartadas na rotação seguinte).

## Persistência do keystore em arquivo/volume

**Contexto.** O par de chaves deve sobreviver a reinícios (senão o `kid` muda a cada boot e a
validação falha enquanto o Keycloak tem o JWKS antigo em cache).

**Decisão:** persistir o keystore PKCS12 em arquivo (caminho externalizado por configuração,
padrão de variáveis de ambiente com default de desenvolvimento). No boot: se o arquivo existe,
carrega; senão, gera o par inicial e o persiste.

**Base de confirmação:** decisão humana registrada (lacuna `persistencia-chave-privada`:
"Persistir o par de chaves (keystore/arquivo) para sobreviver a reinícios"). Constitui **drift
deliberado** ante a convenção "sem persistência" — registrado no `plan.md`.

**Consequências.** O access token segue só em memória; apenas o material de chave é persistido. A
senha do keystore e o caminho entram como configuração externalizada; o material real nunca é
versionado (mesmo princípio dos segredos). Em ambientes efêmeros, o arquivo precisa de volume
persistente para o `kid` se manter estável.

## Endpoint de rotação aberto (POC)

**Contexto.** O `POST /credencial/rotacionar-chave` rotaciona a própria credencial da aplicação.

**Decisão:** mantê-lo **aberto**, sem autenticação, como o `GET /demo/chamar-app-b` atual.

**Base de confirmação:** decisão humana registrada (lacuna `protecao-endpoint-rotacao`: "Aberto,
coerente com a POC"). Sem `spring-security-web`/filtros nesta unidade.

**Consequências.** Risco conhecido de DoS de autenticação (forçar rotações e provocar janelas de
falha); aceito para a POC e a reavaliar fora dela. A estratégia "corrente + anterior no JWKS" mitiga
parcialmente uma única rotação inadvertida, mas não rajadas.

## Documentação OpenAPI (springdoc) — adoção e versão

**Contexto.** Os novos endpoints expostos precisam entrar no contrato REST, mantido via
OpenAPI/springdoc (decisão de descoberta); springdoc ainda não está no `pom.xml`.

**Decisão:** adicionar `springdoc-openapi-starter-webmvc-ui` ao `pom.xml` e anotar os endpoints
(`@Operation`/`@ApiResponse`/`@Schema`), incluindo ao menos `@Operation` no `GET /demo/chamar-app-b`
existente. Esta unidade é a primeira a materializar o OpenAPI, por ser a primeira a precisar dele.

**Base de confirmação:** decisão de descoberta `doc-forms` (manter Swagger/OpenAPI via springdoc,
skill técnica `documentacao-api-openapi`) — confirmada sem reserva. Escolha firme.

**Consequências.** A **versão** do springdoc deve casar com a linha do Spring Boot 4.0.6 pela matriz
de compatibilidade do springdoc no momento da adoção (a linha `2.x`, voltada ao Spring Boot 3.x, pode
não subir); é passo de compatibilidade na implementação, não escolha de tecnologia em aberto.
Avaliar restringir/desabilitar o Swagger UI fora de desenvolvimento (`springdoc.swagger-ui.enabled`),
conforme a skill.
