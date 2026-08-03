---
name: documentacao-api-openapi
description: >
  Como manter e gerar a documentação OpenAPI/Swagger do App B com springdoc-openapi:
  adicionar a dependência, expor a Swagger UI e o JSON /v3/api-docs, liberar essas
  rotas na SecurityConfig, declarar o esquema Bearer JWT e anotar os endpoints
  (/api/protegido e /actuator/health). Carregar sempre que for criar, alterar,
  documentar ou revisar endpoints REST, controllers, anotações OpenAPI/Swagger,
  springdoc, contrato de API ou a UI de documentação. Máx. 1024 caracteres.
metadata:
  author: clovis-cli
  type: technical-skill
---

# Documentação de API (OpenAPI/Swagger)

> **Manutenção desta skill**
>
> Atualize-a sempre que o **como** manter/gerar a documentação OpenAPI mudar (nova
> dependência, mudança de rota da UI, novo padrão de anotação, nova exposição). Um
> refactor que preserva o padrão não exige mudança. Se a skill disser X e o projeto
> adotar Y sem decisão registrada, não decida sozinho: escale ao humano.

## Visão geral do padrão e do problema que resolve

O App B mantém a documentação de sua API via **OpenAPI/Swagger gerada a partir do
código** com **springdoc-openapi** — não há arquivo `.openapi.yaml` escrito à mão nem
collection Postman. A especificação é derivada dos controllers e de anotações
`io.swagger.v3.oas.annotations.*`, versionada junto ao código (o contrato acompanha
automaticamente cada mudança de endpoint). Todo endpoint REST exposto — hoje
`GET /api/protegido` e o `GET /actuator/health` do Actuator — deve estar descrito por
essa documentação.

## Como aplicar

1. **Dependência.** Adicione ao `pom.xml` o starter
   `org.springdoc:springdoc-openapi-starter-webmvc-ui` (aplicação é Spring MVC / Tomcat
   com `spring-boot-starter-web`). Selecione a **versão compatível com a versão de
   Spring Boot do projeto** (ver "Restrições e armadilhas"): confira as release notes do
   springdoc antes de fixar a versão — não copie versões de exemplos de Boot antigo.

2. **Rotas geradas.** Com o starter, ficam disponíveis:
   - JSON da spec: `GET /v3/api-docs`
   - Swagger UI: `GET /swagger-ui.html` (redireciona para `/swagger-ui/index.html`)

3. **Liberar as rotas na segurança (passo obrigatório).** A `SecurityConfig` usa
   `anyRequest().authenticated()`, então sem ajuste a Swagger UI e o `/v3/api-docs`
   respondem `401`. Adicione essas rotas ao `permitAll`, junto de
   `/actuator/health/**`:

   ```java
   .authorizeHttpRequests(auth -> auth
       .requestMatchers("/actuator/health/**").permitAll()
       .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
       .anyRequest().authenticated()
   )
   ```

   Isso é aceitável nesta POC (finalidade demonstrativa). Não amplie o `permitAll` além
   dessas rotas de documentação.

4. **Esquema de segurança Bearer JWT.** Como `/api/protegido` exige
   `Authorization: Bearer <JWT>`, declare o security scheme para habilitar o botão
   **Authorize** na Swagger UI. Defina um bean `OpenAPI` (por exemplo em uma classe
   `config/OpenApiConfig.java`) com um `SecurityScheme` do tipo `HTTP`, esquema
   `bearer`, `bearerFormat` `JWT`, e um `SecurityRequirement` correspondente. Assim o
   token pode ser colado na UI e enviado ao endpoint protegido.

5. **Anotar os endpoints.** No `SecureController` (e em qualquer controller novo), use
   `@Tag` na classe e `@Operation` + `@ApiResponse` no método para descrever propósito,
   respostas (200 autorizado, 401 sem token/ inválido) e o formato do corpo retornado
   (apenas metadados do token). Mantenha as descrições em português.

6. **Actuator no contrato.** O springdoc **não** inclui endpoints do Actuator por
   padrão. Para descrever o `GET /actuator/health`, habilite em `application.yml`:

   ```yaml
   springdoc:
     show-actuator: true
   ```

   Mantenha coerência com `management.endpoints.web.exposure.include: health` — só o
   `health` está exposto; não documente endpoints do Actuator que não estão expostos.

7. **Verificar.** Suba com `mvn spring-boot:run` (porta `8082`) e confira
   `http://localhost:8082/swagger-ui.html` e `http://localhost:8082/v3/api-docs`;
   confirme que `/api/protegido` aparece com o esquema Bearer e o Actuator `health`
   quando `show-actuator` estiver ligado.

## Ferramentas e artefatos envolvidos

- **springdoc-openapi** (`springdoc-openapi-starter-webmvc-ui`) — declarado no `pom.xml`.
- **Anotações OpenAPI** `io.swagger.v3.oas.annotations.*` — nos controllers em
  `src/main/java/com/dbfinanceira/appb/web/`.
- **Bean `OpenAPI`** — configuração global (título, versão da API, security scheme),
  em `src/main/java/com/dbfinanceira/appb/config/`.
- **`application.yml`** — chaves `springdoc.*` (ex.: `show-actuator`).
- **Artefato gerado (não versionado como arquivo):** a spec vive em `/v3/api-docs` em
  tempo de execução, derivada do código. A fonte da verdade do contrato é o código
  anotado, não um YAML separado.

## Restrições e armadilhas conhecidas

- **Compatibilidade de versão.** O springdoc 2.x tem como alvo Spring Boot 3.x
  (Spring Framework 6). O projeto está em **Spring Boot 4 / Spring Framework 7 /
  Spring Security 7**, que exige a linha do springdoc compatível com essa geração.
  Fixar uma versão pensada para Boot 3 pode quebrar a subida. Sempre confirme a
  matriz de compatibilidade nas release notes antes de definir a versão; se não houver
  versão compatível estável, trate como decisão a escalar, não invente um pin.
- **Segurança das rotas de doc.** Só libere `/v3/api-docs/**`, `/swagger-ui/**` e
  `/swagger-ui.html`. Não desabilite autenticação de forma ampla para fazer a UI
  funcionar.
- **Não vazar segredo no contrato.** A documentação descreve `/api/protegido`, que
  retorna **apenas metadados** do token (`sub`, `azp`, `iss`, `aud`, `scope`, `exp`).
  Não adicione exemplos com JWT bruto, `client_secret` ou qualquer dado sensível nas
  anotações/`examples`.
- **Coerência com o comportamento real.** As anotações são a documentação: se um
  endpoint muda (nova resposta, novo campo, nova rota), atualize as anotações na mesma
  mudança para o contrato não divergir do código.
