# Research: Extração do domínio de Acesso Cruzado a Aplicação Externa

## Nome do pacote do novo domínio

**Contexto.** As classes `CrossAccessController`, `CrossAccessResponse` e
`CrossAccessRestClientConfig` precisam de um pacote Java próprio, fora de
`com.aplicacaosegura.resourceserver`.

**Alternativas:**

- **`com.aplicacaosegura.crossaccess`** — segue a convenção já em uso no projeto: um pacote por
  domínio, nome técnico em inglês, sem separadores (`jwks`, `oauth2client`, `resourceserver`).
- **`com.aplicacaosegura.acessocruzado`** — usaria o termo em português do nome do domínio, quebrando
  a convenção já estabelecida nos demais pacotes de domínio, todos em inglês.

**Decisão:** `com.aplicacaosegura.crossaccess`.

**Confirmação basis:** em uso no código — os três pacotes de domínio já existentes seguem esse
mesmo padrão (inglês, sem separadores, nome curto do papel do domínio).

**Consequências:** nenhuma; é a aplicação mecânica de uma convenção já observável, sem introduzir um
padrão novo.

## Renomeação da pasta da spec 005

**Contexto.** O conteúdo de `.agents/specs/005-servidor-recursos-oauth2-cross-chamada-api-externa/`
passa a documentar o novo domínio `acesso-cruzado-api-externa`, e não mais uma feature sobre
`servidor-recursos-oauth2`.

**Alternativas:**

- **Renomear para `005-acesso-cruzado-api-externa`** — mesma convenção das specs fundadoras dos
  outros três domínios (`001-gestao-chaves-rsa-jwks`, `002-cliente-oauth2-client-assertion`,
  `003-servidor-recursos-oauth2`: número + slug do domínio, sem sufixo de feature), preservando o
  número original (ordem cronológica de criação).
- **Manter o número 005 e apenas remover o prefixo `servidor-recursos-oauth2`, preservando o restante
  do slug** (`005-cross-chamada-api-externa`) — preservaria o nome original da feature, mas deixaria
  a spec fundadora de um domínio com um slug de "feature" em vez do slug do próprio domínio,
  divergindo do padrão das specs 001-003.
- **Renumerar como uma nova spec fundadora (`007-acesso-cruzado-api-externa`), mantendo 005 intacta
  como está** — duplicaria a documentação de `GET /api/cross` em duas pastas (a original, ainda
  presa a `servidor-recursos-oauth2`, e a nova), contrariando o pedido de mover a documentação para
  fora do domínio de origem.

**Decisão:** `005-acesso-cruzado-api-externa`.

**Confirmação basis:** convenção já em uso nas specs fundadoras dos três domínios existentes.

**Consequências:** a numeração das specs seguintes (006 em diante) não muda; apenas a pasta 005 é
renomeada (sem duplicação de conteúdo).

## `technical-dependencies.md` para a nova skill

**Contexto.** A skill `acesso-cruzado-api-externa`, criada por esta unidade, precisa decidir se
documenta suas dependências técnicas em `references/technical-dependencies.md`, como as três skills
de domínio já existentes fazem, ou apenas embutidas no corpo da skill.

**Alternativas:**

- **Criar `references/technical-dependencies.md`** — mesmo padrão dos três domínios já existentes
  (`gestao-chaves-rsa-jwks`, `cliente-oauth2-client-assertion`, `servidor-recursos-oauth2`), todos com
  esse arquivo.
- **Descrever as dependências apenas no corpo da `SKILL.md`, sem arquivo dedicado** — quebraria a
  consistência com os três domínios já existentes, sem ganho de clareza (a lista de dependências
  deste domínio tem tamanho comparável à dos demais).

**Decisão:** criar `references/technical-dependencies.md`, listando `cliente-oauth2-client-assertion`,
`servidor-recursos-oauth2`, `spring-boot-starter-web` (`RestClient`) e OpenAPI/Swagger.

**Confirmação basis:** convenção já em uso nos três domínios existentes — todos mantêm esse arquivo.

**Consequências:** nenhuma; mantém a mesma estrutura de skill já adotada no projeto.
