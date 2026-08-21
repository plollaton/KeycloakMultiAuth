# Research: Validação de Autenticação e Permissões

## Biblioteca de descriptografia do token JWE

**Context.** O Keycloak entrega o token de acesso cifrado em JWE; a aplicação precisa
descriptografá-lo com a chave privada local antes de qualquer decisão de autorização (regra 2 e
3 do domínio), sem aplicar verificação adicional de assinatura.

**Alternatives:**

- **Nimbus JOSE+JWT** (`com.nimbusds:nimbus-jose-jwt`) — biblioteca de referência para JOSE/JWT
  em Java; já é dependência transitiva obrigatória de
  `spring-boot-starter-oauth2-resource-server` (o suporte a resource server do Spring Security
  usa Nimbus internamente para decodificar/validar JWT). Expõe `RSADecrypter` para
  descriptografar um `JWEObject` com uma `PrivateKey` do JCA, sem exigir nenhuma dependência
  nova.
- **jose4j** — biblioteca JOSE alternativa; adicionaria uma segunda dependência JOSE ao
  classpath ao lado da que o Spring Security resource server já traz, sem ganho funcional para
  este domínio.
- **Implementação manual via JCA puro** — manipular a estrutura compact do JWE (cabeçalho,
  chave de conteúdo cifrada, IV, ciphertext, tag) diretamente com `javax.crypto`; reimplementa o
  que o Nimbus já resolve, sem necessidade.

**Decision:** Nimbus JOSE+JWT, via um `JwtDecoder` customizado que descriptografa o JWE com
`RSADecrypter` e interpreta o payload decifrado como o claims set do token, sem verificação de
assinatura adicional.

**Confirmation basis:** dependência transitiva obrigatória de
`spring-boot-starter-oauth2-resource-server` — que por sua vez é a camada de resource server já
confirmada sem ressalva em `discovery-answers.md` e `AGENTS.md`. Não introduz biblioteca nova ao
projeto; a escolha decorre diretamente de uma decisão de stack já assentada.

**Consequences:** a descriptografia usa uma API já presente no classpath assim que o starter do
resource server é adicionado ao `pom.xml`; nenhuma dependência adicional entra na "Technical
decisions" do `plan.md`.

## Extração de permissões do claims set decifrado

**Context.** Após decifrar o token, a aplicação precisa checar se o portador tem ao menos uma
permissão — papel de realm, papel de client/resource ou escopo (regra 6 do domínio) — sem exigir
uma permissão específica.

**Alternatives:**

- **`Converter<Jwt, AbstractAuthenticationToken>` customizado** — lê `realm_access.roles`,
  `resource_access.*.roles` e `scope` do claims set decifrado e os combina em uma única lista de
  autoridades, plugado no `JwtAuthenticationProvider` do resource server.
- **`JwtGrantedAuthoritiesConverter` padrão do Spring Security** — só lê um claim configurável
  por instância (por padrão `scope`/`scp`); não cobre, sem composição adicional, os três tipos de
  permissão descritos na regra 6.

**Decision:** converter customizado que combina os três claims em uma única lista de
autoridades.

**Confirmation basis:** os três tipos de permissão (papéis de realm, papéis de client/resource,
escopos) estão explicitados na skill do domínio (`SKILL.md`, regra 6 e seção "Entidades e
dados"); não é uma escolha de biblioteca em aberto, é a tradução direta da regra de negócio já
documentada.

**Consequences:** a checagem de acesso liberado/negado passa a depender de a lista de
autoridades combinada estar vazia ou não, e não de uma autoridade específica — refletindo a
regra 6 sem impor controle de acesso granular.

## Armazenamento da chave privada local

**Context.** A aplicação guarda localmente a chave privada correspondente ao certificado
registrado no Keycloak, usada para descriptografar o token JWE; a forma concreta de
armazenamento/carregamento não é fixada pelas fontes de negócio.

**Alternatives:**

- **Keystore (JKS ou PKCS12)** — formato binário com suporte nativo do Spring Boot
  (`server.ssl.key-store`-like properties), mas exige senha de keystore e alias adicionais só
  para guardar uma única chave.
- **Arquivo PEM** — texto delimitado (`-----BEGIN PRIVATE KEY-----`), formato em que
  certificados/chaves costumam circular e mais simples de gerar e inspecionar manualmente numa
  prova de conceito.
- **Variável de ambiente/propriedade em Base64** — evita arquivo em disco, mas exige codificar o
  conteúdo do arquivo de qualquer forma e complica a rotação manual da chave nesta prova de
  conceito.

**Decision:** arquivo PEM, carregado via um `org.springframework.core.io.Resource` do Spring
(propriedade de aplicação configurável, com suporte aos prefixos `classpath:` e `file:`).

**Confirmation basis:** decisão do requerente, registrada na resposta ao gap
`private-key-storage-format` desta unidade.

**Consequences:** o bean de configuração de segurança precisa de uma etapa de parsing PEM →
`PrivateKey` (Base64 decode + `KeyFactory.getInstance("RSA")` com `PKCS8EncodedKeySpec`); o
arquivo PEM em si é um segredo local e não é versionado no repositório.
