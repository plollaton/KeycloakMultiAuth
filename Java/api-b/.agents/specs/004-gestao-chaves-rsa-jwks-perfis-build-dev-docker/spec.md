# Spec: Perfis de build (dev/docker) para seleção da configuração ativa

## Overview

Extensão do scaffold Maven da aplicação com dois perfis de build — `dev` (desenvolvimento
local) e `docker` (homologação em contêiner) — que selecionam, no momento da compilação, qual
diretório de recursos específico do ambiente (`src/main/resources-dev` ou
`src/main/resources-docker`) é somado ao diretório de recursos base (`src/main/resources`) no
artefato empacotado, eliminando a necessidade de definir manualmente a variável
`SPRING_PROFILES_ACTIVE` no ambiente de execução.

## Domain

- Slug: `gestao-chaves-rsa-jwks`
- Skill: [`.agents/skills/gestao-chaves-rsa-jwks/SKILL.md`](../../skills/gestao-chaves-rsa-jwks/SKILL.md)

Esta unidade estende o scaffold Maven (`pom.xml`) que o domínio já mantém como infraestrutura
transversal desde sua primeira materialização (spec `001-gestao-chaves-rsa-jwks`). O conteúdo
funcional selecionado por cada perfil (URL de resource server) pertence ao domínio "Servidor de
Recursos OAuth2 (Validação de Access Token)" — ver "Cross-domain dependencies".

## Current code state

- `pom.xml` já existe (criado pela spec `001-gestao-chaves-rsa-jwks`), com dois perfis Maven
  declarados: `dev` (`activeByDefault`) e `docker`. Cada perfil soma seu próprio diretório de
  recursos (`src/main/resources-dev` ou `src/main/resources-docker`) ao diretório base
  `src/main/resources`, que permanece incluído em qualquer perfil — é onde vivem os arquivos
  `api-b.pem`/`api-b-cert.pem` do domínio de chaves RSA. Não há plugin de cópia/filtragem de
  recursos nem token de substituição: cada diretório de perfil contém seu próprio
  `application.yml`, já com os valores finais do ambiente.
- `src/main/resources-dev/application.yml` já existe e contém a configuração para desenvolvimento
  local: porta `8081` e `issuer-uri` apontando para `http://localhost:8080/realms/master`.
- `src/main/resources-docker/application.yml` já existe com a mesma estrutura, porta `8080` e
  `issuer-uri` apontando para `http://keycloak:8080/realms/master` — o hostname do serviço
  Keycloak na rede Docker definida em `docker/dockercompose.yml`.
- `Dockerfile-api` não define `SPRING_PROFILES_ACTIVE` nem qualquer outra variável de seleção de
  configuração; a seleção do ambiente é inteiramente decidida no momento da build Maven, pelo
  perfil (`-P docker` ou `-P dev`/nenhum) usado para gerar o artefato.
- Os dois arquivos `application.yml` de ambiente (`resources-dev` e `resources-docker`) já
  declaram estaticamente `spring.profiles.active` (`dev`/`docker`, respectivamente); não há
  substituição de token nem filtragem de recurso Maven envolvida.

## Scope

**In:**

- Dois perfis de build no `pom.xml`: `dev` (desenvolvimento local) e `docker` (homologação em
  contêiner).
- Perfil `dev` ativo por padrão quando a build é executada sem informar um perfil explicitamente.
- Seleção, durante a build, do diretório de recursos correspondente ao perfil ativo
  (`src/main/resources-dev` para `dev`, `src/main/resources-docker` para `docker`), somado ao
  diretório base `src/main/resources`, de forma que o artefato empacotado suba já apontando para
  o ambiente correto sem exigir configuração adicional no momento da execução.

**Out:**

- Conteúdo das URLs/hosts do Keycloak em `src/main/resources-dev/application.yml` e
  `src/main/resources-docker/application.yml` — já definidos e fora do escopo desta unidade.
- Alterações em `Dockerfile-api`, `Dockerfile-servico` ou `docker/dockercompose.yml` — o artefato
  já é compilado fora da imagem (volume somente leitura); a escolha do perfil ocorre no comando
  Maven executado por quem faz a build, antes de subir o contêiner.
- Testes automatizados — esta POC não os implementa, conforme `discovery-answers.md`.
- Qualquer outro perfil de ambiente (produção, staging) além de `dev` e `docker` — não solicitados.

## Domain boundary

**This spec implements:**

- Perfis Maven `<profiles>` `dev` e `docker` no `pom.xml`, com `dev` marcado como
  `activeByDefault`, cada um declarando seu próprio `<build><resources>` apontando para
  `src/main/resources-dev` ou `src/main/resources-docker`, somado ao `<resource>` base
  `src/main/resources` já existente.
- Os arquivos `src/main/resources-dev/application.yml` e
  `src/main/resources-docker/application.yml`, cada um já com o perfil Spring do ambiente
  correspondente (`spring.profiles.active: dev`/`docker`) declarado estaticamente, sem depender
  de `SPRING_PROFILES_ACTIVE` externo.

**Belongs to other domains (cross-domain, does not become a task here):**

- Valores de `issuer-uri` e `app.security.resource-server.expected-audience` → domínio "Servidor
  de Recursos OAuth2 (Validação de Access Token)"; esta unidade não altera esses valores, apenas
  qual arquivo os fornece.
- Orquestração dos contêineres (`docker/dockercompose.yml`, `Dockerfile-api`,
  `Dockerfile-servico`) — infraestrutura de deployment fora de qualquer domínio de negócio desta
  POC.

## Current → new behavior

A build Maven aceita um perfil (`dev`, ativo por padrão, ou `docker`, explícito via `-P docker`)
que determina qual diretório de recursos específico do ambiente é somado ao diretório base
`src/main/resources` no artefato empacotado. Compilar com `-P docker` produz um artefato cujo
`application.yml` embutido é o de `src/main/resources-docker` (Keycloak acessível como
`keycloak:8080`, porta `8080`); compilar sem perfil ou com `-P dev` embute o `application.yml` de
`src/main/resources-dev` (Keycloak acessível como `localhost:8080`, porta `8081`). Em ambos os
casos, os arquivos `api-b.pem`/`api-b-cert.pem` do diretório base `src/main/resources` são
incluídos no artefato, já que esse diretório permanece somado em qualquer perfil.

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
  usa os valores hoje presentes em `src/main/resources-dev/application.yml` (porta `8081`,
  `issuer-uri` apontando para `http://localhost:8080/realms/master`).

**Story 2 — Perfil `docker`:**

- Dado o `pom.xml` com os perfis declarados, quando a build é executada com
  `mvn clean package -P docker`, então o perfil `docker` fica ativo e o perfil `dev` fica
  inativo.
- Dado o artefato gerado pelo perfil `docker`, quando a aplicação é iniciada, então ela usa os
  valores hoje presentes em `src/main/resources-docker/application.yml` (porta `8080`,
  `issuer-uri` apontando para `http://keycloak:8080/realms/master`).

**Story 3 — Ativação automática, sem variável externa:**

- Dado um artefato compilado com o perfil `docker`, quando ele é executado dentro do container
  definido em `docker/dockercompose.yml` (sem `SPRING_PROFILES_ACTIVE` definido no
  `Dockerfile-api` ou no `docker/dockercompose.yml`), então a aplicação sobe usando a
  configuração de `src/main/resources-docker/application.yml`.
- Dado um artefato compilado com o perfil `dev`, quando ele é executado sem nenhuma variável de
  ambiente adicional, então a aplicação sobe usando a configuração de
  `src/main/resources-dev/application.yml`.

## Cross-domain dependencies

- **"Servidor de Recursos OAuth2 (Validação de Access Token)"** — os valores de `issuer-uri` e
  `app.security.resource-server.expected-audience` selecionados por esta unidade são consumidos
  por esse domínio; esta spec não altera esses valores, apenas qual arquivo os fornece no
  artefato final.
- **`docker/dockercompose.yml`** — depende de o artefato em `target/` (montado como volume) ter
  sido compilado com o perfil `docker` para que `api-b` alcance o Keycloak do cenário; essa
  escolha de perfil no comando de build é operacional, fora do escopo desta spec.

## Risks and observations

- Esta POC não implementa testes automatizados (`discovery-answers.md`); a verificação dos
  critérios de aceite desta unidade é manual (inspecionar o conteúdo empacotado em
  `target/classes` e observar os logs de inicialização/conectividade com o Keycloak).
