# Tasks: Perfis de build (dev/docker) para seleção da configuração ativa

- [x] **T1. Perfis Maven dev/docker com ativação automática da configuração de ambiente**
  - Depends on: none
  - Perfis `dev` (ativo por padrão) e `docker` no `pom.xml`, cada um declarando seu próprio
    `<build><resources>` apontando para `src/main/resources-dev` ou `src/main/resources-docker`,
    somado ao `<resource>` base `src/main/resources` (onde vivem os arquivos
    `api-b.pem`/`api-b-cert.pem`), reaproveitando o `maven-resources-plugin` já herdado de
    `spring-boot-starter-parent` — nenhuma dependência nova de build.
  - `src/main/resources-dev/application.yml` e `src/main/resources-docker/application.yml`, cada
    um já com `spring.profiles.active` (`dev`/`docker`) e os demais valores do respectivo ambiente
    (porta do servidor, `issuer-uri`) declarados estaticamente, permitindo que o artefato
    empacotado suba com a configuração correta do ambiente para o qual foi compilado, sem exigir
    `SPRING_PROFILES_ACTIVE` externo no ambiente de execução.
