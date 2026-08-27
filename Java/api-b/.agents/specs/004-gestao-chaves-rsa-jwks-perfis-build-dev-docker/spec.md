# Spec: Perfis de build (dev/docker) para seleção da configuração ativa

## Overview

Extensão do scaffold Maven da aplicação com dois perfis de build — `dev` (desenvolvimento
local) e `docker` (homologação em contêiner) — que selecionam, no momento da compilação, qual
arquivo de configuração (`application.yml` ou `application-docker.yml`) se torna a configuração
ativa do artefato empacotado, eliminando a necessidade de definir manualmente a variável
`SPRING_PROFILES_ACTIVE` no ambiente de execução.

## Domain

- Slug: `gestao-chaves-rsa-jwks`
- Skill: [`.agents/skills/gestao-chaves-rsa-jwks/SKILL.md`](../../skills/gestao-chaves-rsa-jwks/SKILL.md)

Esta unidade estende o scaffold Maven (`pom.xml`) que o domínio já mantém como infraestrutura
transversal desde sua primeira materialização (spec `001-gestao-chaves-rsa-jwks`). O conteúdo
funcional selecionado por cada perfil (URL de resource server) pertence ao domínio "Servidor de
Recursos OAuth2 (Validação de Access Token)" — ver "Cross-domain dependencies".

## Current code state

- `pom.xml` já existe (criado pela spec `001-gestao-chaves-rsa-jwks`), sem nenhum perfil Maven
  declarado e sem plugin de cópia/filtragem de recursos.
- `src/main/resources/application.yml` já existe e contém a configuração para desenvolvimento
  local (`token-uri`/`issuer-uri`/`expected-audience` apontando para `http://localhost:8080/realms/master`).
- `src/main/resources/application-docker.yml` já foi criado (pendente de commit) com a mesma
  estrutura, apontando para `http://keycloak:8080/realms/master` — o hostname do serviço Keycloak
  na rede Docker definida em `docker/dockercompose.yml`.
- `Dockerfile-api` executa `java -jar /app/target/*.jar --port 8085` sem definir
  `SPRING_PROFILES_ACTIVE` nem qualquer outro mecanismo de seleção de configuração; o artefato em
  `target/` é compilado fora da imagem (montado como volume somente leitura pelo
  `docker/dockercompose.yml`).
- Não existe hoje nenhum mecanismo — nem perfil Maven, nem variável de ambiente, nem argumento de
  execução — que faça o artefato empacotado usar `application-docker.yml`; o container sempre
  herda a configuração de `application.yml` (voltada a `localhost`), que não é alcançável a partir
  da rede Docker.

## Scope

**In:**

- Dois perfis de build no `pom.xml`: `dev` (desenvolvimento local) e `docker` (homologação em
  contêiner).
- Perfil `dev` ativo por padrão quando a build é executada sem informar um perfil explicitamente.
- Seleção, durante a build, da configuração correspondente ao perfil ativo (`application.yml`
  para `dev`, `application-docker.yml` para `docker`), de forma que o artefato empacotado suba
  já apontando para o ambiente correto sem exigir configuração adicional no momento da execução.

**Out:**

- Conteúdo das URLs/hosts do Keycloak em `application.yml` e `application-docker.yml` — já
  definidos e fora do escopo desta unidade.
- Alterações em `Dockerfile-api`, `Dockerfile-servico` ou `docker/dockercompose.yml` — o artefato
  já é compilado fora da imagem (volume somente leitura); a escolha do perfil ocorre no comando
  Maven executado por quem faz a build, antes de subir o contêiner.
- Testes automatizados — esta POC não os implementa, conforme `discovery-answers.md`.
- Qualquer outro perfil de ambiente (produção, staging) além de `dev` e `docker` — não solicitados.

## Domain boundary

**This spec implements:**

- Perfis Maven `<profiles>` `dev` e `docker` no `pom.xml`, com `dev` marcado como
  `activeByDefault`.
- Plugin(s) Maven de cópia/filtragem de recursos vinculados a cada perfil, responsáveis por
  tornar `application.yml` (perfil `dev`) ou `application-docker.yml` (perfil `docker`) a
  configuração ativa do artefato empacotado — mecanismo concreto (plugin e binding de fase) a
  definir em `plan.md`.
- Ajuste necessário em `application.yml`/`application-docker.yml` para que a aplicação
  empacotada suba já com o perfil Spring do ambiente para o qual foi compilada, sem depender de
  `SPRING_PROFILES_ACTIVE` externo.

**Belongs to other domains (cross-domain, does not become a task here):**

- Valores de `issuer-uri` e `app.security.resource-server.expected-audience` → domínio "Servidor
  de Recursos OAuth2 (Validação de Access Token)"; esta unidade não altera esses valores, apenas
  qual arquivo os fornece.
- Orquestração dos contêineres (`docker/dockercompose.yml`, `Dockerfile-api`,
  `Dockerfile-servico`) — infraestrutura de deployment fora de qualquer domínio de negócio desta
  POC.

## Current → new behavior

**Atual:** o artefato empacotado (`target/*.jar`) sempre embute `application.yml` (configuração
de desenvolvimento local) como única configuração; não há nenhuma seleção baseada em ambiente. Ao
rodar em contêiner via `docker/dockercompose.yml`, a aplicação tenta alcançar o Keycloak em
`localhost:8080`, host inexistente na rede Docker.

**Novo:** a build Maven aceita um perfil (`dev`, ativo por padrão, ou `docker`, explícito via
`-P docker`) que determina qual arquivo de configuração se torna ativo no artefato empacotado.
Compilar com `-P docker` produz um artefato que, ao subir, já usa os valores de
`application-docker.yml` (Keycloak acessível como `keycloak:8080`); compilar sem perfil ou com
`-P dev` preserva o comportamento atual voltado a `localhost`.

## User stories

1. Como desenvolvedor, quero compilar a aplicação sem informar nenhum perfil, para que ela suba
   localmente apontando para o Keycloak em `localhost:8080`, preservando o comportamento atual.
2. Como responsável por homologação, quero compilar a aplicação com o perfil `docker`, para que o
   artefato empacotado aponte para o Keycloak do ambiente Docker (`keycloak:8080`) sem editar
   manualmente nenhum arquivo de configuração antes do build.
3. Como responsável por homologação, quero que o contêiner suba com a configuração correta do
   ambiente para o qual o artefato foi compilado, sem precisar definir
   `SPRING_PROFILES_ACTIVE` (ou outra variável equivalente) no `docker/dockercompose.yml` ou no
   `Dockerfile-api`.

## Acceptance criteria

**Story 1 — Perfil `dev` (padrão):**

- Dado o `pom.xml` com os perfis declarados, quando a build é executada com `mvn clean package`
  sem a flag `-P`, então o perfil `dev` é o único ativo.
- Dado o artefato gerado pelo perfil `dev`, quando a aplicação é iniciada localmente, então ela
  usa os valores hoje presentes em `application.yml` (`token-uri`, `issuer-uri` e
  `expected-audience` apontando para `http://localhost:8080/realms/master`).

**Story 2 — Perfil `docker`:**

- Dado o `pom.xml` com os perfis declarados, quando a build é executada com
  `mvn clean package -P docker`, então o perfil `docker` fica ativo e o perfil `dev` fica
  inativo.
- Dado o artefato gerado pelo perfil `docker`, quando a aplicação é iniciada, então ela usa os
  valores hoje presentes em `application-docker.yml` (`token-uri`, `issuer-uri` e
  `expected-audience` apontando para `http://keycloak:8080/realms/master`).

**Story 3 — Ativação automática, sem variável externa:**

- Dado um artefato compilado com o perfil `docker`, quando ele é executado dentro do container
  definido em `docker/dockercompose.yml` (sem `SPRING_PROFILES_ACTIVE` definido no
  `Dockerfile-api` ou no `docker/dockercompose.yml`), então a aplicação sobe usando a
  configuração de `application-docker.yml`.
- Dado um artefato compilado com o perfil `dev`, quando ele é executado sem nenhuma variável de
  ambiente adicional, então a aplicação sobe usando a configuração de `application.yml`.

## Cross-domain dependencies

- **"Servidor de Recursos OAuth2 (Validação de Access Token)"** — os valores de `issuer-uri` e
  `app.security.resource-server.expected-audience` selecionados por esta unidade são consumidos
  por esse domínio; esta spec não altera esses valores, apenas qual arquivo os fornece no
  artefato final.
- **`docker/dockercompose.yml`** — depende de o artefato em `target/` (montado como volume) ter
  sido compilado com o perfil `docker` para que `api-a` alcance o Keycloak do cenário; essa
  escolha de perfil no comando de build é operacional, fora do escopo desta spec.

## Risks and observations

- O mecanismo concreto de cópia/filtragem de recursos (qual plugin Maven, binding de fase,
  estratégia de nomeação dos arquivos de origem/destino) é uma decisão técnica de `plan.md`, não
  desta spec.
- Esta POC não implementa testes automatizados (`discovery-answers.md`); a verificação dos
  critérios de aceite desta unidade é manual (inspecionar o conteúdo empacotado em
  `target/classes` e observar os logs de inicialização/conectividade com o Keycloak).
- `src/main/resources/application-docker.yml` ainda não está commitado (arquivo novo, staged) no
  momento desta spec; seu conteúdo já reflete o valor esperado para o ambiente Docker e não é
  alterado por esta unidade.
