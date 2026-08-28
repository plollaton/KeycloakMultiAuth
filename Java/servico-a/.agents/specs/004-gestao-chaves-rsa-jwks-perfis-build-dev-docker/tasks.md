# Tasks: Perfis de build (dev/docker) para seleção da configuração ativa

- [x] **T1. Perfis Maven dev/docker com ativação automática da configuração de ambiente**
  - Depends on: none
  - Perfis `dev` (ativo por padrão) e `docker` no `pom.xml`, cada um definindo a propriedade
    `spring.profiles.active` com o valor do respectivo ambiente.
  - Filtragem de recurso habilitada para `src/main/resources`, com delimitador dedicado (evitando
    colisão com os placeholders `${...}` do Spring), reaproveitando o `maven-resources-plugin` já
    herdado de `spring-boot-starter-parent` — nenhuma dependência nova de build.
  - `application.yml` passa a declarar `spring.profiles.active` a partir da propriedade do perfil
    Maven ativo, resolvida no momento do empacotamento, permitindo que o Spring Boot combine
    automaticamente `application-docker.yml` sobre `application.yml` quando o artefato é
    compilado com o perfil `docker`, sem exigir `SPRING_PROFILES_ACTIVE` externo no ambiente de
    execução.
