# Plan: Gestão de Chaves RSA e Publicação JWKS

## Stack and structure

Aplicação Spring Boot 4.1.x / Java 21 / Jakarta EE 11, conforme `AGENTS.md`. Este domínio é o
primeiro a ser implementado no repositório, hoje sem `pom.xml` nem código-fonte, e por isso é
quem cria o scaffold Maven do projeto:

- `pom.xml` na raiz, `groupId` `com.aplicacaosegura`, `artifactId` `aplicacao-segura`,
  empacotamento `jar`, parent `spring-boot-starter-parent` na linha 4.1.x.
- Layout padrão `src/main/java/com/aplicacaosegura/...` e `src/main/resources/`.
- Pacotes por domínio de negócio, sem camada arquitetural adicional (não há hexagonal/clean
  architecture fixada em `AGENTS.md`, e o volume de classes desta POC não justifica a
  complexidade): `com.aplicacaosegura.jwks` para este domínio (bean do par de chaves,
  controller do JWKS), `com.aplicacaosegura.web` para a classe principal `@SpringBootApplication`
  e configuração REST/OpenAPI comuns aos três domínios. Domínios futuros (`oauth2client`,
  `resourceserver`) recebem seus próprios pacotes quando forem implementados. Detalhes da decisão
  em [`research.md`](./research.md#estrutura-de-pacotes).

## Technical decisions

Dependências técnicas do domínio (`.agents/skills/gestao-chaves-rsa-jwks/references/technical-dependencies.md`)
que este spec cria, e como cada uma é montada:

- **Nimbus JOSE+JWT** — dependência Maven direta `com.nimbusds:nimbus-jose-jwt` (versão gerenciada
  pelo BOM do `spring-boot-starter-parent`). Usada em um `@Configuration` que constrói o par de
  chaves ativo como bean `RSAKey` (2048 bits, RS256, `keyUse` `sig`) na inicialização, carregando a
  chave privada do arquivo `api-a-private.pem` (PKCS8) e a chave pública do certificado
  `api-a-cert.pem` (X.509), ambos em `src/main/resources`, com `kid` igual ao número de série do
  certificado; e em um `@RestController` que serializa `new JWKSet(rsaKey.toPublicJWK())` em
  `GET /oauth2/jwks`. Escopo da dependência (agora, via biblioteca direta, e não pelos starters
  OAuth2) detalhado em
  [`research.md`](./research.md#escopo-de-dependencias-maven-do-scaffold).
- **Endpoint público fora de qualquer regra de autenticação** — `SecurityFilterChain` mínima
  (`spring-boot-starter-security` como dependência direta), liberando
  `GET /oauth2/jwks`, `GET /v3/api-docs/**`, `GET /swagger-ui/**` e `GET /swagger-ui.html` com
  `permitAll()`; qualquer outro caminho fica com postura padrão restritiva
  (`anyRequest().authenticated()`, sem `AuthenticationProvider` configurado nesta unidade —
  bloqueia por padrão em vez de liberar). Rationale em
  [`research.md`](./research.md#postura-padrao-da-securityfilterchain).
- **OpenAPI/Swagger** — dependência `springdoc-openapi-starter-webmvc-ui` (linha 2.x compatível
  com Spring Boot 4.1.x, conforme a skill técnica `documentacao-api-openapi`); o controller do
  JWKS recebe `@Tag`, `@Operation` e `@ApiResponse`, e o DTO de resposta recebe `@Schema` nos
  campos `kty`/`kid`/`use`/`alg`/`n`/`e`, seguindo o padrão daquela skill.

Decisão explícita de escopo do scaffold (o que este unit traz para o `pom.xml` agora vs. o que
fica pendente para os domínios "Cliente OAuth2" e "Servidor de Recursos OAuth2"): apenas
`spring-boot-starter-web`, `spring-boot-starter-security`, `springdoc-openapi-starter-webmvc-ui` e
`com.nimbusds:nimbus-jose-jwt` entram agora — `spring-boot-starter-oauth2-client` e
`spring-boot-starter-oauth2-resource-server` ficam explicitamente pendentes para quando os
domínios que os configuram (client_assertion e validação de access token, respectivamente) forem
implementados. Justificativa completa em
[`research.md`](./research.md#escopo-de-dependencias-maven-do-scaffold).

Lombok não é adotado neste domínio (opcional conforme `AGENTS.md`); ver
[`research.md`](./research.md#lombok).

## Data model

Não se aplica: o par de chaves RSA é mantido como bean carregado dos arquivos `api-a-private.pem`
e `api-a-cert.pem` em `src/main/resources`, não como entidade persistida em banco de dados.
`data-model.md` dispensado — não há tabelas, relações nem constraints para este domínio.

## External contracts

O contrato de `GET /oauth2/jwks` já está totalmente especificado nos critérios de aceite do
`spec.md` (campos `kty`/`kid`/`use`/`alg`/`n`/`e`, exemplo de JSON incluído). Pasta `contracts/`
dispensada: por convenção do projeto (skill técnica `documentacao-api-openapi`), o contrato
OpenAPI real é gerado em tempo de execução pelo `springdoc-openapi` a partir das anotações do
controller — não há arquivo `.yaml`/`.json` mantido manualmente no repositório, e duplicar o
mesmo contrato em `contracts/` não reduziria ambiguidade adicional para as tasks.

## Interface

Não se aplica — domínio puramente backend, sem interface própria. `ui/` dispensada.

## Testing strategy

Esta POC não implementa testes automatizados (unitários, integração ou e2e), conforme decisão
registrada em `.agents/context/discovery-answers.md`. Nenhuma infraestrutura de teste
(`spring-boot-starter-test`, runner) é introduzida por esta unidade.

## Impact on the authoritative documentation

A skill `gestao-chaves-rsa-jwks` e o `AGENTS.md` foram atualizados para descrever o par de chaves
como carregado do par fixo de arquivos `api-a-private.pem`/`api-a-cert.pem`, substituindo a
descrição anterior de geração em memória por padrão com carregamento opcional via PKCS12 — decisão
humana registrada a partir de um gap levantado durante a implementação, já refletida neste plano
e no `spec.md`. Não há divergência remanescente entre este plano e a documentação autoritativa do
domínio.
