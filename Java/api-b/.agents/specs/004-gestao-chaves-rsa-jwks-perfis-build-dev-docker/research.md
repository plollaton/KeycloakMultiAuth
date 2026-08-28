# Research: Perfis de build (dev/docker) para seleção da configuração ativa

## Mecanismo de seleção da configuração por perfil de build

**Contexto.** O `pom.xml` precisa produzir, a partir do mesmo código-fonte e dos dois arquivos
`application.yml` de ambiente (um para `dev`, outro para `docker`), um artefato que suba com a
configuração correta do ambiente para o qual foi compilado, sem exigir `SPRING_PROFILES_ACTIVE`
externo no momento da execução.

**Alternativas:**

- **Diretório de recursos próprio por perfil, somado ao diretório base.** Cada perfil Maven
  (`dev`/`docker`) declara seu próprio `<build><resources>`, apontando para
  `src/main/resources-dev` ou `src/main/resources-docker`; esse `<resource>` de perfil é somado ao
  `<resource>` base `src/main/resources` (onde vivem os arquivos `api-b.pem`/`api-b-cert.pem`,
  incluídos em qualquer perfil). Cada diretório de ambiente contém seu próprio `application.yml`
  autocontido, já com a porta do servidor, o `issuer-uri` e `spring.profiles.active` (`dev`/
  `docker`) declarados estaticamente. O `maven-resources-plugin` (já parte do ciclo de vida padrão
  herdado de `spring-boot-starter-parent`, sem necessidade de nova dependência) apenas copia, para
  cada perfil, o diretório de recursos correspondente para o artefato empacotado — sem filtragem
  nem substituição de token.
- **Cópia/sobrescrita física do arquivo via `maven-antrun-plugin`.** Adicionar
  `maven-antrun-plugin` (nova dependência de build) e, no perfil `docker`, executar uma tarefa Ant
  `<copy>` que sobrescreve `target/classes/application.yml` com o conteúdo do `application.yml` do
  ambiente Docker após a cópia padrão de recursos. Funciona, mas introduz uma dependência de build
  só para essa finalidade e deixa o arquivo de origem duplicado e não utilizado dentro do artefato
  final.

**Decisão:** diretório de recursos próprio por perfil, somado ao diretório base (primeira
alternativa).

**Base de confirmação:** mecanismo baseado no suporte nativo do Maven a múltiplos `<resource>` por
perfil (`<build><resources>` dentro de `<profile>`), somado aos `<resource>` declarados fora de
`<profiles>`; usa um plugin já presente no ciclo de vida padrão do projeto (herdado de
`spring-boot-starter-parent`), sem adicionar nenhuma dependência nova de build. Atende à instrução
da unidade de instalar o(s) plugin(s) necessários para a cópia do arquivo correto conforme o
perfil, mantendo cada `application.yml` de ambiente autocontido e já com o `spring.profiles.active`
correspondente, sem exigir reorganização dos arquivos de chave RSA em `src/main/resources`.

**Consequências:**

- Cada diretório de ambiente (`resources-dev`/`resources-docker`) mantém seu próprio
  `application.yml`, sem depender de um passo de build para preencher `spring.profiles.active` —
  ao rodar `mvn spring-boot:run` diretamente, o perfil Maven ativo (`dev` por padrão) já resolve
  para o diretório de recursos correto.
- Apenas o `application.yml` do perfil de build escolhido entra no artefato empacotado — o
  `application.yml` do outro ambiente não é copiado, já que `dev` e `docker` são mutuamente
  exclusivos (ativar `docker` explicitamente desativa `dev`, que só é ativo por padrão na ausência
  de qualquer outro perfil explícito) e cada um soma apenas o seu próprio diretório de recursos ao
  `<resource>` base `src/main/resources`, que permanece incluído em qualquer perfil.
