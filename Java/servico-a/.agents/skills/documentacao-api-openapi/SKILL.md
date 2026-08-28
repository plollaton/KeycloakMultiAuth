---
name: documentacao-api-openapi
description: >
  Convenção para documentar via OpenAPI/Swagger os endpoints REST expostos pela
  aplicação (GET /oauth2/jwks, GET /api/public, GET /api/protected). Carregar ao
  criar, alterar ou remover qualquer endpoint REST, ao anotar um controller com
  contrato OpenAPI, ao adicionar ou atualizar a dependência springdoc-openapi no
  `pom.xml`, ou ao revisar request/response, códigos de status HTTP e a liberação
  de acesso público ao Swagger UI e ao `/v3/api-docs` na `SecurityFilterChain`.
metadata:
  author: clovis-cli
  type: technical-skill
---

# Documentação de API com OpenAPI/Swagger

> **Mantendo esta skill**
>
> Atualizar sempre que a convenção de documentação mudar (troca de biblioteca, novo
> padrão de anotação, novo grupo de endpoints que passa a exigir contrato formal). Um
> refactor que preserva a convenção — mover um controller de pacote, renomear um DTO
> sem mudar seu contrato — não exige atualização.

## Visão geral do padrão e problema que resolve

Todo endpoint REST exposto pela aplicação (`GET /oauth2/jwks`, `GET /api/public`,
`GET /api/protected`) é documentado via OpenAPI/Swagger, conforme decisão registrada
em `.agents/context/discovery-answers.md`. A geração do contrato OpenAPI é feita por
`springdoc-openapi` a partir de anotações nos controllers, e não por um arquivo
`.yaml`/`.json` mantido manualmente: o contrato deve refletir o código a cada build,
sem exigir sincronização manual entre implementação e documentação.

`GET /actuator/health` é o único endpoint público do projeto que não segue esta
convenção — ele é documentado pelo próprio contrato padrão do Spring Boot Actuator.

## Como aplicar

1. **Dependência**: adicionar `springdoc-openapi-starter-webmvc-ui` ao `pom.xml`,
   na versão da linha 2.x compatível com o Spring Boot 4.1.x / Spring Framework 7.x /
   Jakarta EE 11 já fixados como stack do projeto (ver `AGENTS.md`). Com essa
   dependência, a aplicação expõe automaticamente `GET /v3/api-docs` (contrato OpenAPI
   em JSON) e `GET /swagger-ui.html` (UI interativa), sem controller adicional.
2. **Anotar cada controller REST** com `@Tag(name = ...)` no nível da classe, agrupando
   por domínio (ex.: `jwks`, `api`), e cada método com `@Operation(summary = ...,
   description = ...)`.
3. **Anotar as respostas possíveis** de cada endpoint com `@ApiResponse` (um por
   código HTTP relevante), incluindo o caso de sucesso e os casos de erro que a
   `SecurityFilterChain` pode produzir (`401`, `403`) quando o endpoint exige token.
4. **Descrever o corpo de resposta** com `@Schema` nos DTOs/records retornados, para
   que o contrato gerado inclua nome, tipo e descrição de cada campo — por exemplo,
   nos campos `kty`, `kid`, `use`, `alg`, `n`, `e` do `JWKSet` retornado por
   `GET /oauth2/jwks`.
5. **Liberar acesso público** aos caminhos `GET /v3/api-docs/**` e
   `GET /swagger-ui/**` (e `GET /swagger-ui.html`) na mesma `SecurityFilterChain` que
   já libera `GET /api/public`, `GET /actuator/health` e `GET /oauth2/jwks` — sem essa
   liberação, o Swagger UI e o contrato ficam inacessíveis mesmo para os endpoints que
   não exigem autenticação.
6. Ao criar um novo endpoint REST, ou ao alterar o contrato de um existente (novo
   parâmetro, novo campo de resposta, nova faixa de status HTTP), atualizar as
   anotações OpenAPI no mesmo commit da mudança de código — o contrato nunca fica
   defasado em relação ao controller.

## Ferramentas e artefatos envolvidos

- **`springdoc-openapi-starter-webmvc-ui`**: dependência Maven que lê as anotações
  `@Tag`/`@Operation`/`@ApiResponse`/`@Schema` dos controllers e gera o contrato
  OpenAPI em tempo de execução — não há arquivo `.yaml`/`.json` versionado no
  repositório para editar manualmente.
- **`GET /v3/api-docs`**: contrato OpenAPI gerado, em JSON, servido pela própria
  aplicação.
- **`GET /swagger-ui.html`**: UI interativa para explorar e testar os endpoints
  documentados, servida pela própria aplicação.
- **`SecurityFilterChain`** (domínio "Servidor de Recursos OAuth2" em
  `.agents/maps/functional-map.md`): dono da lista de caminhos públicos; os caminhos
  do Swagger UI e do `/v3/api-docs` entram nessa lista como pré-requisito para a
  documentação funcionar, mas a regra de quais endpoints de negócio exigem token
  continua sendo definida por aquele domínio, não por esta skill.

## Restrições e armadilhas conhecidas

- Anotar um controller sem liberar `/v3/api-docs/**` e `/swagger-ui/**` na
  `SecurityFilterChain` resulta em Swagger UI retornando `401`/`403` mesmo para
  endpoints públicos — a causa é a cadeia de segurança, não a anotação.
- `GET /actuator/health` não recebe anotações OpenAPI: ele é documentado pelo
  contrato padrão do Spring Boot Actuator, fora do escopo desta convenção.
- Esta POC não implementa testes automatizados; não há teste de contrato (contract
  testing) validando o OpenAPI gerado contra o comportamento real dos endpoints — a
  consistência depende de atualizar as anotações no mesmo commit que altera o
  controller.
