---
name: documentacao-api-openapi
description: >
  Como manter a documentação do contrato REST via OpenAPI/Swagger com
  springdoc-openapi no Spring Boot. Carregar ao criar, alterar ou remover
  endpoints REST (`@RestController`, `@GetMapping`/`@PostMapping`), ao anotar
  operações, parâmetros e schemas (`@Operation`, `@ApiResponse`, `@Parameter`,
  `@Schema`), ao configurar o bean `OpenAPI`/Swagger UI ou ao ajustar
  propriedades `springdoc.*` no `application.yml`. Cobre expor o `/v3/api-docs`
  (JSON/YAML) e o Swagger UI e manter o contrato sincronizado com o código.
metadata:
  author: clovis-cli
  type: technical-skill
---

# Documentação de API (OpenAPI)

> **Manutenção desta skill**
>
> Atualize este documento sempre que o **how-to** de documentar a API mudar
> (nova dependência/versão, nova convenção de anotação, novo caminho exposto,
> nova propriedade `springdoc.*`). Um refactor que preserva o padrão — renomear
> um controller, mover um método — não exige alteração. Se o projeto passar a
> adotar uma abordagem diferente da descrita aqui sem decisão registrada,
> escale para o humano em vez de "consertar" a skill contra o código.

## Visão geral do padrão e o problema que resolve

O contrato REST do `app-a-client` é documentado via **OpenAPI/Swagger**, gerado
a partir do próprio código com a biblioteca **springdoc-openapi**. Esta é a
única forma de documentação mantida no projeto (decisão validada na descoberta;
Postman e ADRs foram descartados). A documentação **não** é um arquivo
`.openapi.yaml` escrito à mão: ela é derivada dos `@RestController` e das
anotações OpenAPI presentes no código, garantindo que o contrato acompanhe a
implementação e seja versionado junto com ela.

O objetivo é que qualquer endpoint REST exposto esteja descrito no OpenAPI gerado,
com operação, respostas e schemas coerentes com o que o código realmente faz. Os
caminhos expostos a documentar são: `GET /demo/chamar-app-b` (`DemoController`),
`POST /credencial/rotacionar-chave` (`CredencialController`) e
`GET /.well-known/jwks.json` (`JwksController`).

## Como aplicar

### 1. Garantir a dependência do springdoc-openapi

O contrato usa o starter WebMVC + Swagger UI do springdoc:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version><!-- versão compatível com o Spring Boot deste projeto --></version>
</dependency>
```

Use a versão do springdoc **compatível com a linha do Spring Boot** em uso
(hoje Spring Boot 4.0.6). O `spring-boot-starter-parent` **não** gerencia a
versão do springdoc, então declare-a explicitamente. Confirme a versão correta
na matriz de compatibilidade do springdoc-openapi no momento da adoção, em vez
de fixar um número desatualizado — a linha do springdoc para Spring Boot 3.x
(`2.x`) pode não servir ao Spring Boot 4.

### 2. Anotar os endpoints

Os endpoints já são detectados automaticamente por serem `@RestController` com
`@GetMapping`/`@PostMapping`. Enriqueça o contrato com as anotações OpenAPI
(pacote `io.swagger.v3.oas.annotations`) sempre que criar ou alterar um endpoint:

- `@Tag` no controller — agrupa as operações por área.
- `@Operation(summary = "...", description = "...")` no método — descreve o que
  o endpoint faz.
- `@ApiResponse` / `@ApiResponses` — documenta os códigos de status e o corpo de
  cada resposta.
- `@Parameter` — documenta parâmetros de path/query/header.
- `@Schema` — descreve campos de DTOs de request/response.

O tipo de retorno e os parâmetros do método já viram schema automaticamente;
as anotações servem para o que não é inferível (descrições, exemplos, respostas
de erro). Documente todos os caminhos expostos: o `GET /demo/chamar-app-b`
deve ter ao menos `@Operation` e as respostas esperadas; o
`POST /credencial/rotacionar-chave` (resposta `200` com `{ kid, criadaEm }`, sem material
privado) e o `GET /.well-known/jwks.json` (resposta `200` com o documento JWKS das chaves
públicas correntes) devem ser anotados com `@Operation` e `@ApiResponse` coerentes com o
que o código retorna.

### 3. Expor e conferir o contrato gerado

Com a aplicação no ar (`mvn spring-boot:run`, porta `8081`), o springdoc expõe,
nos caminhos padrão:

- **OpenAPI JSON:** `/v3/api-docs`
- **OpenAPI YAML:** `/v3/api-docs.yaml`
- **Swagger UI:** `/swagger-ui.html`

Após criar ou alterar um endpoint, suba a aplicação e confira nesses caminhos
que a operação aparece com operação, respostas e schemas corretos.

### 4. Configurar metadados globais (opcional, quando útil)

Para título, versão e descrição da API, exponha um bean `OpenAPI`:

```java
@Bean
OpenAPI appAOpenApi() {
    return new OpenAPI().info(new Info()
        .title("app-a-client")
        .version("1.0.0")
        .description("Cliente OAuth2 client_credentials que chama a App B"));
}
```

Ajustes de comportamento (caminhos, habilitar/desabilitar o Swagger UI por
ambiente) vão por propriedades `springdoc.*` no `application.yml`, seguindo o
padrão de configuração externalizada já adotado (defaults no `application.yml`,
sobrescritos por variável de ambiente quando fizer sentido). Exemplo:

```yaml
springdoc:
  swagger-ui:
    enabled: true
```

## Ferramentas e artefatos envolvidos

- **springdoc-openapi** (`springdoc-openapi-starter-webmvc-ui`) — gera o OpenAPI
  e serve o Swagger UI; dependência declarada no `pom.xml`.
- **Anotações OpenAPI** (`io.swagger.v3.oas.annotations.*`) — aplicadas nos
  controllers em `src/main/java/com/dbfinanceira/appa/web/`.
- **Contrato gerado** — servido em runtime nos caminhos `/v3/api-docs`,
  `/v3/api-docs.yaml` e `/swagger-ui.html`; não há arquivo de contrato versionado
  à parte, o código é a fonte.
- **`application.yml`** — configuração `springdoc.*` quando necessária.
- **Endpoints a documentar:** `GET /demo/chamar-app-b` (`DemoController`),
  `POST /credencial/rotacionar-chave` (`CredencialController`) e
  `GET /.well-known/jwks.json` (`JwksController`).

## Restrições e armadilhas conhecidas

- **Compatibilidade de versão:** a versão do springdoc precisa casar com a linha
  do Spring Boot (4.x). Uma versão pensada para Spring Boot 3.x pode não subir; é
  o principal ponto de atenção na adoção.
- **O código é a fonte, não um YAML manual:** não crie nem edite um
  `.openapi.yaml` à mão em paralelo. Divergência entre um arquivo manual e as
  anotações gera contrato inconsistente. Ajuste sempre as anotações no código.
- **Sincronização é responsabilidade da alteração:** ao mudar assinatura,
  resposta ou schema de um endpoint, atualize as anotações na mesma mudança —
  contrato desatualizado é pior que ausência de contrato.
- **Exposição do Swagger UI:** os caminhos do springdoc não são cobertos pelo
  fluxo `client_credentials`/`OAuth2ClientHttpRequestInterceptor` (esse
  interceptor age nas chamadas de saída para a App B, não nas rotas de entrada).
  Avalie restringir ou desabilitar o Swagger UI fora de desenvolvimento por
  `springdoc.swagger-ui.enabled`.
- **Não confundir com o Actuator:** o Actuator expõe endpoints operacionais
  (`/actuator/**`), não o contrato de negócio. A documentação OpenAPI cobre os
  endpoints REST da aplicação, não os do Actuator.
