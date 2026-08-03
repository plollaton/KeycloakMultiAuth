# AGENTS.md — App B (Resource Server)

> Fonte única de verdade das instruções para agentes neste repositório. Leia e siga este
> arquivo antes de qualquer tarefa. `CLAUDE.md` e `.github/copilot-instructions.md` são apenas
> ponteiros para cá.

## Visão geral

O **App B** é um _resource server_ Spring Boot que recebe requisições autenticadas por
`Authorization: Bearer <JWT>`, onde o token é um _access token_ emitido pelo **Keycloak**
(via `client_credentials`, obtido pela App A). O App B **valida o token de forma offline** —
verifica assinatura, `iss`, `aud` e validade temporal usando um JWKS (chaves públicas do
Keycloak) lido de um **arquivo local**, sem consultar o Keycloak a cada requisição. O
propósito é uma **POC que demonstra o fluxo de autenticação** ponta a ponta; não há regra de
negócio financeiro própria — `/api/protegido` é um recurso demonstrativo.

A App A (`app-a-client`, projeto irmão), o Keycloak e o script `sync-jwks.sh` (no
diretório-pai) estão **fora do escopo de manutenção** deste módulo; são citados apenas como
integrações externas.

## Stack principal

- **Java 21**, **Spring Boot 4.0.6** (Spring Framework 7 / Spring Security 7), **Maven**.
- `spring-boot-starter-web` (Tomcat — padrão; Undertow foi removido no Boot 4),
  `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server` (traz o Nimbus
  JOSE `com.nimbusds` transitivamente), `spring-boot-starter-actuator`.
- Dependências de teste presentes (`spring-boot-starter-test`, `spring-security-test`), porém
  sem testes escritos (ver convenções transversais).

## Estrutura do repositório

```
app-b-resource-server/
  pom.xml
  src/main/resources/application.yml           # configuração e variáveis de ambiente (com defaults)
  src/main/java/com/dbfinanceira/appb/
    AppBResourceServerApplication.java          # entrypoint Spring Boot
    config/AppBSecurityProperties.java          # @ConfigurationProperties(prefix = "app.security.jwt")
    config/JwtDecoderConfig.java                # monta o NimbusJwtDecoder + validadores encadeados
    config/SecurityConfig.java                  # filter chain stateless, CSRF off, oauth2ResourceServer
    security/LocalFileJWKSource.java            # leitura do JWKS local, hot-reload por mtime, fail-fast
    web/SecureController.java                    # GET /api/protegido — recurso demonstrativo
  .agents/                                       # artefatos para agentes (ver abaixo)
```

## Convenções arquiteturais

- **Autenticação offline via JWKS de arquivo local.** O `JwtDecoder` é construído manualmente
  a partir de um JWKS lido de arquivo local (`LocalFileJWKSource`), pois o App B roda em
  segmento de rede sem saída direta para o Keycloak. Definir o bean `JwtDecoder` desativa a
  auto-config do resource server: a propriedade `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`
  **não** deve ser usada.
- **API stateless autenticada por Bearer token.** `SessionCreationPolicy.STATELESS`; CSRF
  desabilitado **de propósito** (o Spring Security 7 passou a habilitá-lo por padrão também
  para APIs); `/actuator/health/**` é `permitAll`, todo o restante exige autenticação.
- **Validação de claims.** `iss` = realm do Keycloak; `aud` deve **conter** a audience
  esperada (`app-b` — exige Audience Mapper no Keycloak); `exp`/`nbf`/`iat` com tolerância de
  relógio configurável (`clock-skew-seconds`, padrão 60s).
- **JWKS com hot-reload e fail-fast.** O arquivo é recarregado automaticamente quando o
  `mtime` muda (rotação de chave não exige reiniciar). _Fail-fast_ na subida se o arquivo não
  existir ou for inválido; em falha de checagem posterior, mantém a última versão válida em
  memória. `getLastLoadedAt()` existe para virar um _health indicator_ de "JWKS desatualizado"
  (ainda **não** implementado — melhoria conhecida).
- **Configuração externalizada por variáveis de ambiente**, mapeadas em `AppBSecurityProperties`.

## Restrições globais

- **Nunca logar o JWT bruto.** Registrar apenas metadados não sensíveis (`sub`, `azp`, `jti`,
  timestamps). O mesmo vale para respostas: `/api/protegido` só devolve metadados do token.
- **TLS obrigatório** em todos os saltos (regra de instituição financeira; parte é
  operacional/lado-Keycloak, mas orienta qualquer mudança no App B).
- Só está exposto o endpoint `health` do Actuator (`management.endpoints.web.exposure.include: health`).

## Comandos essenciais

Não há Maven Wrapper (`mvnw`) no módulo; use o `mvn` do ambiente. O App sobe na porta `8082`.

- **Build / empacotar:** `mvn clean package`
- **Rodar localmente:** `mvn spring-boot:run`
- **Compilar apenas:** `mvn compile`
- **Testes:** `mvn test` — sem lint dedicado configurado e sem testes escritos nesta POC (ver abaixo).

## Configuração e variáveis de ambiente

As variáveis de ambiente e seus _defaults_ estão declaradas em
[`src/main/resources/application.yml`](./src/main/resources/application.yml) (prefixo
`app.security.jwt`) — consulte esse arquivo como fonte das variáveis necessárias; não
reproduza valores sensíveis fora dele. O `README.md` do diretório-pai documenta o fluxo
completo, a configuração do Keycloak e o checklist de segurança.

## Convenções transversais

- **Versionamento:** o módulo segue _SemVer_ (`version` no `pom.xml`, atualmente `1.0.0`).
- **Documentação de API mantida via OpenAPI/Swagger** (ex.: `springdoc-openapi`), versionada
  junto ao código; `/api/protegido` e `/actuator/health` devem ser descritos por ela.
- **Sem testes automatizados nesta POC:** as dependências de teste permanecem no `pom.xml`,
  mas não se prioriza escrever testes automatizados neste momento.

## Estrutura `.agents/`

- **`.agents/maps/`** — visão dos domínios (bounded contexts) do App B: fronteiras,
  dependências e ordem sugerida de entendimento/manutenção (`functional-map.md`).
- **`.agents/context/`** — memória durável da descoberta (objetivo, escopo, restrições e
  decisões transversais validadas), cuja precedência vale sobre inferências posteriores.
- **`.agents/skills/`** — _skills_ carregadas sob demanda pelo agente a partir do `description`
  no _frontmatter_ de cada uma. Contém tanto _skills_ de negócio (domínio) quanto _skills_
  técnicas (transversais). Não há índice manual aqui: basta saber que a pasta existe e o agente
  carrega a _skill_ relevante conforme a tarefa.

## Fonte de verdade do domínio

As **_skills_ locais em `.agents/skills/`** são a documentação **autoritativa** do domínio.
Consulte e leia a _skill_ correspondente **antes de implementar regras de negócio**, e
mantenha-a atualizada quando o comportamento do domínio mudar.
