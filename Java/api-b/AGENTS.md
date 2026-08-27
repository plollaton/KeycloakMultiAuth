# AGENTS.md

Fonte única de verdade para agentes de código que atuam neste repositório. Instruções mais
específicas (skills em `.agents/skills/`) complementam este arquivo, nunca o contradizem.

## Visão geral do projeto e do domínio

`aplicacao-segura` é uma POC (prova de conceito) de backend Java que valida um fluxo de
autenticação OAuth2 entre múltiplas aplicações mediadas por um Keycloak. A aplicação assume o
papel de:

- **OAuth2 Resource Server**: valida `access_tokens` emitidos pelo Keycloak em endpoints próprios
  protegidos, usando o JWKS publicado pelo Keycloak (não o JWKS da própria aplicação).

A aplicação também carrega seu próprio par de chaves RSA fixo e publica a chave pública
correspondente em um endpoint JWKS próprio (`GET /oauth2/jwks`, público).

Os dois domínios de negócio da POC, com regras e dependências detalhadas, estão descritos em
`.agents/maps/functional-map.md`:

1. **Gestão de Chaves RSA e Publicação JWKS** — domínio de fundação; carrega o par de chaves RSA
   fixo (2048 bits, RS256, `kid` único) no início da aplicação e expõe `GET /oauth2/jwks` (público).
2. **Servidor de Recursos OAuth2 (validação de access token)** — depende do domínio 1 para manter
   `/oauth2/jwks` público na mesma `SecurityFilterChain` que protege `GET /api/protected`.

## Stack e tecnologias principais

Stack alvo fixada pelo material de negócio (`.agents/context/business-input.md`,
`.agents/context/discovery-answers.md`):

- Java 21 ou superior
- Spring Boot 4.1.x
- Spring Framework 7.x
- Spring Security 7.x
- Jakarta EE 11
- `spring-boot-starter-oauth2-resource-server`
- Nimbus JOSE+JWT (via starters, para construir/serializar `RSAKey`/`JWKSet`)
- Lombok (opcional)
- Maven, como ferramenta de build (`pom.xml`)

## Estrutura do repositório

- `pom.xml` — scaffold Maven do projeto, com os perfis `dev` (porta 8081, ativo por padrão) e
  `docker` (porta 8080), cada um definindo seu próprio `issuer-uri` do Keycloak.
- `src/main/java/com/aplicacaosegura/` — código-fonte da aplicação, em três pacotes:
  - `jwks` — carregamento do par de chaves RSA fixo e publicação de `GET /oauth2/jwks`.
  - `resourceserver` — validação de `access_tokens` do Keycloak e endpoints de exemplo.
  - `web` — `SecurityConfig`, com a `SecurityFilterChain` única da aplicação.
- `src/main/resources/` — par de chaves RSA fixo (`api-b.pem`, `api-b-cert.pem`) carregado na
  inicialização.
- `src/main/resources-dev/` e `src/main/resources-docker/` — `application.yml` de cada perfil
  Maven.
- `descritivo.md` — material de negócio bruto que originou a descoberta funcional.
- `.agents/` — artefatos e skills da descoberta funcional e do fluxo assistido por agentes (ver
  seção dedicada abaixo).
- `.clovis/`, `.claude/` — configuração das ferramentas de CLI/agente usadas neste fluxo.

## Convenções arquiteturais importantes

- O par de chaves RSA da aplicação é carregado como bean na inicialização (2048 bits, algoritmo
  RS256) a partir do par fixo de arquivos `api-b.pem` (chave privada, PKCS8) e
  `api-b-cert.pem` (certificado X.509 com a chave pública) em `src/main/resources`; o `kid` é o
  número de série do certificado.
- `GET /oauth2/jwks` expõe apenas a chave pública da aplicação, no formato `kty`/`kid`/`use`/`alg`/
  `n`/`e`, e é público (sem autenticação).
- A `SecurityFilterChain` que protege os endpoints da aplicação valida `access_tokens` contra o
  JWKS do **Keycloak** (não o JWKS próprio da aplicação) e checa as claims `iss`, `exp`, `aud` e
  `iat`. Nessa mesma cadeia, `GET /api/public`, `GET /actuator/health`, `GET /oauth2/jwks`,
  `GET /v3/api-docs/**`, `GET /swagger-ui/**` e `GET /swagger-ui.html` permanecem públicos;
  `GET /api/protected` exige token válido.

## Restrições globais

- Esta POC não implementa testes automatizados (unitários, integração ou e2e).
- Os endpoints REST do projeto são documentados via OpenAPI/Swagger.

## Comandos de build, lint e teste

O projeto usa Maven (`pom.xml`) como ferramenta de build. Os comandos padrão são:

- Build: `mvn clean package`
- Executar a aplicação: `mvn spring-boot:run`

Não há comando de teste: esta POC não implementa testes automatizados. Nenhuma ferramenta de lint
foi definida pelo material de negócio ou pela descoberta funcional.

## Estrutura de `.agents/`

- `.agents/context/` e `.agents/maps/` guardam os artefatos canônicos da descoberta funcional
  (material de negócio, decisões transversais e mapa de domínios) — não são reescritos por este
  arquivo, apenas referenciados.
- `.agents/skills/` reúne as skills carregadas sob demanda pelo agente, tanto de negócio (regras e
  fluxos de um domínio específico) quanto técnicas (convenções transversais que exigem passos ou
  exemplos, como a documentação de API via OpenAPI/Swagger). O carregamento de cada skill é
  disparado pelo seu próprio frontmatter `description`; este arquivo não lista nem indexa as
  skills individualmente.

## Precedência das skills

Antes de criar ou editar arquivos, ou de implementar qualquer funcionalidade, o agente deve
verificar se existe uma skill em `.agents/skills/` que governe o caso — pela tecnologia, padrão de
código ou arquitetura envolvidos, ou pelo domínio de negócio, funcionalidade ou módulo em questão —
e segui-la antes de agir. As skills em `.agents/skills/` são autoritativas e têm precedência sobre
padrões inferidos do código existente. O corpo de cada skill indica onde vive a documentação
completa do seu assunto e qual fonte é a verdade — dentro ou fora do repositório —, e o agente
segue essa indicação a partir da skill.

## Escrita de comentários e documentação

Todo texto que vive neste repositório — comentário de código, docstring, skill, `AGENTS.md`,
README, spec — descreve o estado atual, escrito para quem abre o arquivo hoje sem conhecer o
histórico do arquivo nem a conversa que o produziu.

- **Descreva o que é, nunca a transição.** Ao editar um texto existente, reescreva a passagem a
  partir do resultado final, como se sempre tivesse sido assim. Uma frase que só faz sentido para
  quem viu a versão anterior — ou o seu próprio diff — não pertence ao arquivo: o que mudou
  pertence à mensagem de commit e à descrição do pull request.
- **Negue apenas para prevenir um erro plausível.** Uma negação só se justifica quando um leitor
  competente realmente tentaria a alternativa e a frase explica por que ela falha, funcionando
  como guarda para uma mudança futura. Contrastar com a versão anterior, com uma alternativa que
  ninguém tentaria, ou com o que o próprio código já mostra, é ruído que custa atenção ao leitor.
- **Faça a limpeza antes de terminar a tarefa.** Releia os textos que você criou ou alterou e
  corte o que falhar nas duas regras acima. Expressões como "não mais", "antes", "agora",
  "em vez de", "ao contrário de" e "sem precisar de" são os sintomas mais comuns — mantenha apenas
  as que sobrevivem à segunda regra.

Uma exceção: quando o assunto do texto **é** uma mudança — uma mensagem de commit, a descrição de
um pull request, a spec de uma unidade de manutenção, um changelog —, a transição é o conteúdo. A
regra proíbe narrar a *edição do texto*, nunca a mudança de que o texto trata.
