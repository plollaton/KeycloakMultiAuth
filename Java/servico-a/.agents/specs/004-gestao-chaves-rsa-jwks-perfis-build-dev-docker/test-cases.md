# Casos de teste: Perfis de build (dev/docker) para seleção da configuração ativa

## Pré-condições

- Repositório `aplicacao-segura` com `pom.xml`, `application.yml` e `application-docker.yml`
  conforme definidos em `plan.md`: perfis Maven `dev` (ativo por padrão) e `docker`, com filtragem
  de recurso habilitada em `src/main/resources` e `application.yml` declarando
  `spring.profiles.active: @spring.profiles.active@`.
- JDK 21 e Maven instalados, para os builds e execuções locais (casos do perfil `dev`).
- Docker e Docker Compose instalados, com o stack de `docker/dockercompose.yml` (`postgres`,
  `keycloak`, `api-a`) disponível para subir (casos do perfil `docker`).
- Endpoint diagnóstico `GET /diagnostics/oauth2-client-assertion` disponível — já existente no
  domínio "Cliente OAuth2 com Client Assertion JWT (RFC 7523)" — usado para observar, sem expor
  segredos, qual `token-uri` a aplicação resolveu ao tentar a troca `client_credentials` com o
  Keycloak.

## Story 1 — Perfil dev (padrão)

### TC-1 (mandatory) — Build sem `-P` grava `spring.profiles.active: dev` no artefato

1. A partir da raiz do projeto `aplicacao-segura`, executar `mvn clean package` sem informar a
   flag `-P`.
2. Abrir o `application.yml` empacotado em `target/classes/application.yml`.

**Expected:** o arquivo contém a chave `spring.profiles.active: dev`; os demais valores
(`token-uri`, `issuer-uri`, `expected-audience`) permanecem `http://localhost:8080/realms/master`,
iguais aos hoje presentes em `application.yml`.

### TC-2 (mandatory) — Aplicação compilada com o perfil dev sobe apontando para `localhost`

1. Com o artefato gerado pelo TC-1, executar `java -jar target/aplicacao-segura-*.jar`
   localmente, sem definir nenhuma variável de ambiente adicional.
2. Aguardar a inicialização completa da aplicação.
3. Chamar `GET /diagnostics/oauth2-client-assertion`.

**Expected:** a aplicação sobe sem erro e o `token-uri` usado na troca `client_credentials` é
`http://localhost:8080/realms/master`, valor de `application.yml`.

### TC-3 (recommended) — Especificar `-P dev` explicitamente produz o mesmo resultado do build padrão

1. Executar `mvn clean package -P dev`.
2. Abrir `target/classes/application.yml`.

**Expected:** conteúdo idêntico ao gerado pelo TC-1 (`spring.profiles.active: dev`), confirmando
que `dev` é o perfil ativo por padrão quando nenhum perfil é informado.

## Story 2 — Perfil docker

### TC-4 (mandatory) — Build com `-P docker` grava `spring.profiles.active: docker` no artefato

1. A partir da raiz do projeto, executar `mvn clean package -P docker`.
2. Abrir o `application.yml` empacotado em `target/classes/application.yml`.

**Expected:** o arquivo contém a chave `spring.profiles.active: docker`; os demais valores
(`token-uri`, `issuer-uri`, `expected-audience`) do próprio `application.yml` permanecem os
originais de `http://localhost:8080/realms/master` — a seleção do ambiente Docker é feita pela
ativação do perfil Spring `docker`, mesclado a partir de `application-docker.yml` em tempo de
execução, e não por alteração dos valores de `application.yml` no momento do build.

### TC-5 (mandatory) — Aplicação compilada com o perfil docker resolve para o Keycloak do ambiente Docker

1. Com o artefato gerado pelo TC-4, subir o container `api-a` definido em
   `docker/dockercompose.yml` (que monta `target/` como volume, conforme `Dockerfile-api`).
2. Aguardar a inicialização completa da aplicação dentro do container.
3. Chamar `GET /diagnostics/oauth2-client-assertion` (pela porta publicada `8085`).

**Expected:** o `token-uri` resolvido pela aplicação é `http://keycloak:8080/realms/master`,
valor de `application-docker.yml` aplicado pelo Spring Boot ao mesclá-lo sobre `application.yml`
porque `spring.profiles.active=docker`; a troca `client_credentials` com o Keycloak do cenário
Docker é concluída com sucesso.

## Story 3 — Ativação automática, sem variável externa

### TC-6 (mandatory) — Nenhuma variável `SPRING_PROFILES_ACTIVE` externa é definida para o serviço `api-a`

1. Inspecionar `Dockerfile-api` e `docker/dockercompose.yml`.

**Expected:** nenhum dos dois arquivos define a variável de ambiente `SPRING_PROFILES_ACTIVE`
(nem equivalente) para o serviço `api-a`.

### TC-7 (mandatory) — Container sobe com a configuração correta apenas pelo perfil de build, sem variável externa

1. Repetir a subida do container `api-a` com o artefato compilado com o perfil `docker` (mesmo
   artefato do TC-4), sem adicionar manualmente `SPRING_PROFILES_ACTIVE` nem qualquer outra
   variável de ambiente ao serviço.
2. Chamar `GET /diagnostics/oauth2-client-assertion` (pela porta publicada `8085`).

**Expected:** mesmo resultado do TC-5 — o `token-uri` resolvido é
`http://keycloak:8080/realms/master` — confirmando que a seleção do ambiente decorre
inteiramente do perfil Maven usado na compilação, sem depender de nenhuma variável de ambiente
externa. Compilando e executando com o perfil `dev` (artefato do TC-1, execução do TC-2), o mesmo
se verifica para `http://localhost:8080/realms/master`.
