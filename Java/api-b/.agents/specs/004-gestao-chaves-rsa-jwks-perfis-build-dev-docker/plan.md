# Plan: Perfis de build (dev/docker) para seleção da configuração ativa

## Stack e estrutura

Build Maven (`pom.xml`) do projeto `aplicacao-segura`, herdando de `spring-boot-starter-parent`
(versão `4.1.1`), conforme `AGENTS.md`. Os recursos de configuração seguem o layout
`src/main/resources` (base, com os arquivos `api-b.pem`/`api-b-cert.pem` do domínio de chaves
RSA) somado a um diretório específico por ambiente — `src/main/resources-dev` ou
`src/main/resources-docker` —, cada um com seu próprio `application.yml` autocontido. Nenhuma
mudança de camada, pacote ou dependência de runtime é necessária — a unidade é inteiramente de
build/empacotamento.

## Decisões técnicas

- **Dois perfis Maven em `pom.xml`**: `dev` (com `<activation><activeByDefault>true</activeByDefault></activation>`)
  e `docker`. Cada perfil declara seu próprio `<build><resources>`, apontando para
  `src/main/resources-dev` (perfil `dev`) ou `src/main/resources-docker` (perfil `docker`); esse
  `<resource>` de perfil é somado ao `<resource>` base `src/main/resources` já declarado fora de
  `<profiles>`, que permanece incluído em qualquer perfil.
- **`application.yml` próprio por diretório de ambiente, sem filtragem de recurso**: cada
  diretório de perfil (`resources-dev`/`resources-docker`) contém seu próprio `application.yml`,
  já com os valores finais do ambiente (porta do servidor, `issuer-uri`) e com
  `spring.profiles.active` declarado estaticamente (`dev`/`docker`). Não há token de substituição
  nem `<filtering>` habilitado — o `maven-resources-plugin` apenas copia, para cada perfil, o
  diretório de recursos correspondente para o artefato empacotado. Alternativas avaliadas e a base
  de confirmação desta escolha estão em [`research.md`](./research.md).
- **`src/main/resources-dev/application.yml` e `src/main/resources-docker/application.yml`
  permanecem sem alteração de conteúdo por esta unidade** — a unidade não modifica os valores de
  ambiente já definidos (`issuer-uri`, `expected-audience`, porta), apenas o mecanismo de build que
  os seleciona.
- **Nenhum novo plugin de dependência é adicionado** ao `pom.xml` — a solução reaproveita o
  `maven-resources-plugin` já ativo no ciclo de vida padrão, apenas com um `<resource>` adicional
  por perfil.

## Modelo de dados

Não aplicável — nenhuma entidade, tabela ou schema é criado ou alterado por esta unidade.
`data-model.md` dispensado.

## Contratos externos

Não aplicável — nenhum contrato REST/evento é criado ou alterado; o endpoint `GET /oauth2/jwks` e
os demais contratos já documentados permanecem inalterados. `contracts/` dispensado.

## Interface

Não aplicável — unidade de build/empacotamento backend, sem interface de usuário. `ui/` dispensado.

## Estratégia de testes

Esta POC não implementa testes automatizados (`discovery-answers.md`); a verificação desta
unidade é manual, por build:

- **Perfil `dev` (padrão)**: `mvn clean package` sem `-P`; inspecionar
  `target/classes/application.yml` e confirmar `spring.profiles.active: dev`; subir a aplicação
  localmente e confirmar que o `issuer-uri` resolvido é `http://localhost:8080/realms/master` —
  via log de inicialização (o Spring Security Resource Server registra a descoberta do JWKS a
  partir do `issuer-uri` configurado) ou, indiretamente, observando que a aplicação sobe
  corretamente na porta `8081` (perfil `dev`) e expõe `GET /oauth2/jwks` e `GET /api/public`; o
  `issuer-uri` em si não é exposto por nenhum endpoint HTTP desta aplicação, sendo usado apenas
  internamente pelo Spring Security para descobrir o JWKS do Keycloak.
- **Perfil `docker`**: `mvn clean package -P docker`; inspecionar
  `target/classes/application.yml` e confirmar `spring.profiles.active: docker`; subir o artefato
  dentro do container definido em `docker/dockercompose.yml` e confirmar, pelo mesmo mecanismo
  (log de inicialização, ou observar que a aplicação sobe corretamente na porta `8080` do
  container e expõe `GET /oauth2/jwks` e `GET /api/public`), que o `issuer-uri` resolvido é
  `http://keycloak:8080/realms/master` e que a validação de um access token emitido pelo Keycloak
  do cenário Docker é bem-sucedida.

## Impacto na documentação autoritativa

Sem impacto. A skill `gestao-chaves-rsa-jwks` não descreve nenhum comportamento sobre seleção de
configuração por ambiente/perfil de build — não há afirmação prévia com a qual este plano
divirja, deliberada ou não. Nenhuma tarefa de atualização de documentação autoritativa nasce desta
unidade.

## Artefatos opcionais

- `data-model.md` — dispensado (sem entidades).
- `research.md` — gerado: documenta a decisão do mecanismo de seleção de configuração e as
  alternativas avaliadas.
- `contracts/` — dispensado (sem contrato novo ou alterado).
- `ui/` — dispensado (sem interface).
