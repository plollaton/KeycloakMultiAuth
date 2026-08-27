# Casos de teste: Perfis de build (dev/docker) para seleção da configuração ativa

## Pré-condições

- Repositório `aplicacao-segura` com `pom.xml`, `src/main/resources-dev/application.yml` e
  `src/main/resources-docker/application.yml` conforme definidos em `plan.md`: perfis Maven `dev`
  (ativo por padrão) e `docker`, cada um somando seu próprio diretório de recursos
  (`src/main/resources-dev` ou `src/main/resources-docker`) ao diretório base
  `src/main/resources`; cada `application.yml` de ambiente já declara `spring.profiles.active`
  estaticamente (`dev`/`docker`), sem filtragem de recurso nem token de substituição.
- JDK 21 e Maven instalados, para os builds e execuções locais (casos do perfil `dev`).
- Docker e Docker Compose instalados, com o stack de `docker/dockercompose.yml` (`postgres`,
  `keycloak`, `api-b`) disponível para subir (casos do perfil `docker`).
- Rota pública `GET /oauth2/jwks` disponível — usada para observar, sem expor segredos, que a
  aplicação subiu com o perfil esperado na porta esperada (`8081` para o perfil `dev`, `8080` para
  o perfil `docker`), como indício indireto de qual `issuer-uri` foi resolvido; o log de
  inicialização do Spring Boot é a fonte direta para confirmar o `issuer-uri` resolvido, já que
  essa propriedade não é exposta por nenhum endpoint HTTP desta aplicação.

## Story 1 — Perfil dev (padrão)

### TC-1 (mandatory) — Build sem `-P` grava `spring.profiles.active: dev` no artefato

1. A partir da raiz do projeto `aplicacao-segura`, executar `mvn clean package` sem informar a
   flag `-P`.
2. Abrir o `application.yml` empacotado em `target/classes/application.yml`.

**Expected:** o arquivo contém a chave `spring.profiles.active: dev`; os demais valores (porta
`8081`, `issuer-uri`) permanecem os hoje presentes em `src/main/resources-dev/application.yml`
(`issuer-uri: http://localhost:8080/realms/master`).

### TC-2 (mandatory) — Aplicação compilada com o perfil dev sobe apontando para `localhost`

1. Com o artefato gerado pelo TC-1, executar `java -jar target/aplicacao-segura-*.jar`
   localmente, sem definir nenhuma variável de ambiente adicional.
2. Aguardar a inicialização completa da aplicação.
3. Chamar `GET /oauth2/jwks` na porta `8081` (porta configurada para o perfil `dev`) e inspecionar
   o log de inicialização do Spring Boot.

**Expected:** a aplicação sobe sem erro, responde em `8081` e o `GET /oauth2/jwks` retorna com
sucesso, confirmando que subiu com o perfil `dev`; o `issuer-uri` resolvido, observável no log de
inicialização, é `http://localhost:8080/realms/master`, valor de
`src/main/resources-dev/application.yml`.

### TC-3 (recommended) — Especificar `-P dev` explicitamente produz o mesmo resultado do build padrão

1. Executar `mvn clean package -P dev`.
2. Abrir `target/classes/application.yml`.

**Expected:** conteúdo idêntico ao gerado pelo TC-1 (`spring.profiles.active: dev`), confirmando
que `dev` é o perfil ativo por padrão quando nenhum perfil é informado.

## Story 2 — Perfil docker

### TC-4 (mandatory) — Build com `-P docker` grava `spring.profiles.active: docker` no artefato

1. A partir da raiz do projeto, executar `mvn clean package -P docker`.
2. Abrir o `application.yml` empacotado em `target/classes/application.yml`.

**Expected:** o arquivo contém a chave `spring.profiles.active: docker`; os demais valores (porta
`8080`, `issuer-uri: http://keycloak:8080/realms/master`) são os hoje presentes em
`src/main/resources-docker/application.yml` — o artefato empacotado com o perfil `docker` embute
esse `application.yml` no lugar do de `src/main/resources-dev`, e não uma versão mesclada ou
alterada em tempo de build.

### TC-5 (mandatory) — Aplicação compilada com o perfil docker resolve para o Keycloak do ambiente Docker

1. Com o artefato gerado pelo TC-4, subir o container `api-b` definido em
   `docker/dockercompose.yml` (que monta `target/` como volume, conforme `Dockerfile-api`).
2. Aguardar a inicialização completa da aplicação dentro do container.
3. Chamar `GET /oauth2/jwks` na porta `8080` (porta configurada para o perfil `docker`) e
   inspecionar o log de inicialização do container.

**Expected:** a aplicação sobe sem erro dentro do container, responde em `8080` e o
`GET /oauth2/jwks` retorna com sucesso, confirmando que subiu com o perfil `docker`; o
`issuer-uri` resolvido, observável no log de inicialização, é `http://keycloak:8080/realms/master`,
valor de `src/main/resources-docker/application.yml` embutido no artefato pelo perfil `docker`; a
validação de um access token emitido pelo Keycloak do cenário Docker é concluída com sucesso.

## Story 3 — Ativação automática, sem variável externa

### TC-6 (mandatory) — Nenhuma variável `SPRING_PROFILES_ACTIVE` externa é definida para o serviço `api-b`

1. Inspecionar `Dockerfile-api` e `docker/dockercompose.yml`.

**Expected:** nenhum dos dois arquivos define a variável de ambiente `SPRING_PROFILES_ACTIVE`
(nem equivalente) para o serviço `api-b`.

### TC-7 (mandatory) — Container sobe com a configuração correta apenas pelo perfil de build, sem variável externa

1. Repetir a subida do container `api-b` com o artefato compilado com o perfil `docker` (mesmo
   artefato do TC-4), sem adicionar manualmente `SPRING_PROFILES_ACTIVE` nem qualquer outra
   variável de ambiente ao serviço.
2. Chamar `GET /oauth2/jwks` na porta `8080` e inspecionar o log de inicialização do container.

**Expected:** mesmo resultado do TC-5 — a aplicação responde em `8080` e o `issuer-uri` resolvido,
observável no log de inicialização, é `http://keycloak:8080/realms/master` — confirmando que a
seleção do ambiente decorre inteiramente do perfil Maven usado na compilação, sem depender de
nenhuma variável de ambiente externa. Compilando e executando com o perfil `dev` (artefato do
TC-1, execução do TC-2), o mesmo se verifica para a aplicação respondendo em `8081` com
`issuer-uri` resolvido igual a `http://localhost:8080/realms/master`.
