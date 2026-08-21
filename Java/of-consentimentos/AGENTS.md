# AGENTS.md

Fonte única de verdade para agentes que trabalham neste repositório. Leia este arquivo antes de
qualquer tarefa.

## Visão geral

Prova de conceito de **autenticação e autorização** de requisições destinadas à "aplicação de moto
offline". Cada requisição chega com um header `Bearer` emitido por um servidor **Keycloak**
associado a um certificado. A aplicação guarda a **chave privada** localmente, usa-a para
**descriptografar** o token (JWE) e, após decifrar, valida as **permissões** do portador antes de
liberar o acesso.

O objetivo é **provar a ideia da autenticação**, não entregar funcionalidade de negócio. A
superfície funcional é mínima: um único domínio exercitado por um endpoint no estilo "hello world"
que serve de recurso protegido para demonstrar o fluxo.

## Domínio

Domínio único: **Validação de Autenticação e Permissões** (`validacao-autenticacao-permissoes`).
Toda requisição ao recurso protegido exige um token `Bearer`; ausência, invalidez ou falha na
descriptografia (JWE) do token resultam em acesso negado. Após decifrar o token com a chave privada
local, a autorização considera as permissões (papéis/escopos) do portador.

## Stack

- **Java 21**
- **Spring Boot**
- **Spring Security** como resource server — intercepta as requisições, extrai o header `Bearer` e
  aplica a descriptografia/autorização em cada endpoint.
- **Keycloak** — emissor dos tokens `Bearer` cifrados (JWE); origem da identidade e das permissões.
- **Chave privada + certificado** — material criptográfico que a aplicação guarda para descriptografar
  o token JWE.
- **Maven** com **Maven Wrapper** (`./mvnw`) como ferramenta de build.

## Estrutura do repositório

Layout Maven padrão para Spring Boot:

```
pom.xml                       # dependências e build (versionado)
mvnw, mvnw.cmd, .mvn/         # Maven Wrapper
src/main/java/                # código-fonte da aplicação
src/main/resources/           # configuração da aplicação (application.properties/yml) e recursos
.agents/                      # artefatos de contexto para agentes (ver abaixo)
```

## Comandos essenciais

Use sempre o Maven Wrapper para garantir a versão correta do Maven. No Windows, use `mvnw.cmd` no
lugar de `./mvnw`.

- Build: `./mvnw clean package`
- Executar a aplicação: `./mvnw spring-boot:run`

## Convenções e restrições globais

- **Testes automatizados:** esta prova de conceito não mantém testes automatizados. A validação do
  fluxo é feita manualmente exercitando o endpoint protegido.
- **Documentação de API:** não há documentação adicional de API (sem Swagger/OpenAPI, sem coleção
  Postman, sem ADRs). A documentação do domínio vive nas skills de `.agents/skills/`.
- **Persistência, cache e mensageria:** fora de escopo. O fluxo ("hello world", validar a ideia de
  autenticação, um domínio) não envolve entidades de negócio nem estado persistente.
- **Observabilidade:** basta o logging padrão do Spring Boot; nenhuma stack de observabilidade
  dedicada.
- **Configuração:** a configuração da aplicação — incluindo a origem/carregamento da chave privada e
  do certificado — é centralizada em `src/main/resources` conforme a convenção do Spring Boot. Nunca
  versione a chave privada nem segredos no repositório.

## Estrutura `.agents/`

A pasta `.agents/` guarda o contexto durável para agentes:

- `context/` — memória da discovery funcional (`business-input.md`, `discovery-answers.md`): objetivo,
  escopo, restrições declaradas e decisões transversais. As restrições registradas aqui têm
  precedência sobre inferências posteriores.
- `maps/` — mapa funcional (`functional-map.md`): identificação dos domínios (bounded contexts), suas
  fronteiras e dependências.
- `skills/` — skills carregadas sob demanda pelo agente. Abriga tanto skills de **negócio** (por
  domínio) quanto skills **técnicas** (transversais). O carregamento é dirigido pelo `description` do
  frontmatter de cada skill; por isso não há índice de skills aqui.

## Precedência das skills e onde vive a documentação do domínio

Antes de criar ou editar arquivos, ou implementar qualquer funcionalidade, verifique se uma skill de
`.agents/skills/` governa o caso — seja pela tecnologia, padrão de código ou arquitetura envolvidos,
seja pelo domínio de negócio, feature ou módulo em questão — e siga-a antes de agir.

As skills em `.agents/skills/` são autoritativas e têm precedência sobre padrões inferidos do código
existente. O corpo de cada skill indica onde vive a documentação completa do seu assunto e qual
fonte detém a verdade — dentro ou fora do repositório —; siga essa indicação a partir da skill.

## Escrita de comentários e documentação

Todo texto que vive neste repositório — comentário de código, docstring, skill, `AGENTS.md`, README,
spec — descreve o estado atual, escrito para quem abre o arquivo hoje sem conhecer nem o histórico do
arquivo nem a conversa que o produziu.

- **Descreva o que é, nunca a transição.** Ao editar um texto existente, reescreva a passagem a
  partir do resultado final, como se sempre tivesse sido assim. Uma frase que só faz sentido para
  quem viu a versão anterior — ou o seu próprio diff — não pertence ao arquivo: o que mudou pertence
  à mensagem de commit e à descrição do pull request.
- **Negue apenas para evitar um erro plausível.** Uma negação merece seu lugar quando um leitor
  competente de fato tentaria a alternativa e a frase diz por que ela falha, guardando assim uma
  mudança futura. Contraste com a versão anterior, com uma alternativa que ninguém tentaria, ou com o
  que o código já mostra, é ruído que custa a atenção do leitor.
- **Poda antes de terminar a tarefa.** Releia os textos que criou ou alterou e corte o que falhar nas
  duas regras acima. Construções como "não mais", "costumava", "agora é", "em vez de", "ao invés de",
  "diferente de" e "sem precisar de" são os sintomas usuais — mantenha só as que sobrevivem à segunda
  regra.

Uma exceção: quando o assunto do texto **é** uma mudança — mensagem de commit, descrição de pull
request, spec de uma unidade de manutenção, changelog — a transição é o conteúdo. A regra proíbe
narrar a *edição do texto*, nunca a mudança de que o texto trata.
