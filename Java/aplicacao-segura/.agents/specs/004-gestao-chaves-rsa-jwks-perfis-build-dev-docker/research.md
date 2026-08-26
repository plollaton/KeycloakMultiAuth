# Research: Perfis de build (dev/docker) para seleção da configuração ativa

## Mecanismo de seleção da configuração por perfil de build

**Contexto.** O `pom.xml` precisa produzir, a partir do mesmo código-fonte e dos mesmos arquivos
`application.yml`/`application-docker.yml`, um artefato que suba com a configuração correta do
ambiente para o qual foi compilado (`dev` ou `docker`), sem exigir `SPRING_PROFILES_ACTIVE`
externo no momento da execução.

**Alternativas:**

- **Filtragem de recurso Maven + ativação nativa de perfil Spring.** Habilitar
  `<filtering>true</filtering>` em `src/main/resources` com um delimitador dedicado (`@...@`, para
  não colidir com placeholders `${...}` do próprio Spring), e declarar em `application.yml` a
  chave `spring.profiles.active: @spring.profiles.active@`. Cada perfil Maven (`dev`/`docker`)
  define a propriedade `spring.profiles.active` com o valor correspondente. No build, o
  `maven-resources-plugin` (já parte do ciclo de vida padrão herdado de `spring-boot-starter-parent`,
  sem necessidade de nova dependência) substitui o token pelo valor do perfil ativo. Em tempo de
  execução, o Spring Boot usa seu próprio mecanismo nativo de arquivos por perfil
  (`application-{profile}.yml`) para sobrepor `application-docker.yml` a `application.yml` quando
  `spring.profiles.active=docker` — mecanismo documentado do próprio framework, sem código
  adicional.
- **Cópia/sobrescrita física do arquivo via `maven-antrun-plugin`.** Adicionar
  `maven-antrun-plugin` (nova dependência de build) e, no perfil `docker`, executar uma tarefa Ant
  `<copy>` que sobrescreve `target/classes/application.yml` com o conteúdo de
  `application-docker.yml` após a cópia padrão de recursos. Funciona, mas introduz uma dependência
  de build só para essa finalidade, deixa `application-docker.yml` duplicado e não utilizado dentro
  do artefato final, e não ativa nenhum perfil Spring — a ativação por perfil, pedida explicitamente
  na descrição da unidade ("ative o perfil pelo yml"), ficaria sem função real nessa alternativa.

**Decisão:** filtragem de recurso Maven + ativação nativa de perfil Spring (primeira alternativa).

**Base de confirmação:** mecanismo documentado e canônico do próprio Spring Boot para build
multi-ambiente com Maven (arquivos `application-{profile}.yml` combinados com
`spring.profiles.active` definido via filtragem de recurso); usa um plugin já presente no ciclo de
vida padrão do projeto (herdado de `spring-boot-starter-parent`), sem adicionar nenhuma dependência
nova de build. Atende às duas instruções da unidade — "instalar os plugins necessários para a cópia
do arquivo correto conforme o perfil" (a filtragem seleciona o conteúdo correto a ser empacotado) e
"ativar o perfil pelo yml" (o `spring.profiles.active` gravado em `application.yml` no momento do
build) — sem exigir alteração de `application-docker.yml` nem reorganização dos arquivos já
criados.

**Consequências:**

- `application.yml` passa a ter uma linha adicional (`spring.profiles.active`), preenchida pelo
  Maven no empacotamento; ao rodar `mvn spring-boot:run` diretamente (sem passar pelo `package`), o
  token pode não ser substituído fora do fluxo de build padrão — mitigado adotando `dev` como
  perfil `activeByDefault`, de modo que o valor-fonte do arquivo (antes da filtragem) já é
  consistente com o ambiente local.
- `application-docker.yml` continua no artefato para os dois perfis (a filtragem de recursos não
  remove arquivos do pacote), mas só é efetivamente lido quando `spring.profiles.active=docker` —
  comportamento nativo do Spring Boot, sem necessidade de excluí-lo do build do perfil `dev`.
