# Plan: Validação de Autenticação e Permissões

## Stack and structure

Backend único em **Spring Boot / Java 21**, layout Maven padrão e Maven Wrapper (`./mvnw`),
conforme `AGENTS.md`. Não há código nem estrutura de pacotes prévia no repositório (projeto
greenfield) — esta unidade estabelece o primeiro pacote da aplicação:

```
src/main/java/com/ofconsentimentos/
├── OfConsentimentosApplication.java   # classe de bootstrap do Spring Boot
├── security/
│   ├── SecurityConfig.java            # cadeia de filtros do resource server
│   ├── JweTokenDecoder.java            # JwtDecoder customizado: descriptografia JWE
│   └── PermissoesAuthenticationConverter.java   # extrai papéis/escopos do token decifrado
└── web/
    └── HelloWorldController.java      # recurso protegido "hello world"
```

## Technical decisions

- **Interceptação e descriptografia do token** — Spring Security como resource server
  (`spring-boot-starter-oauth2-resource-server`), com um `JwtDecoder` customizado
  (`JweTokenDecoder`) no lugar do `NimbusJwtDecoder` padrão: o decoder recebe o token
  compact-serialized, descriptografa o envelope JWE com a chave privada local usando
  `com.nimbusds.jose.crypto.RSADecrypter` (Nimbus JOSE+JWT) e interpreta o payload decifrado
  diretamente como o claims set do token — sem aplicar, além da descriptografia, nenhuma
  verificação de assinatura, conforme a regra 3 do domínio. Falha na descriptografia (chave
  incompatível, token corrompido ou malformado) lança uma exceção de autenticação, capturada
  pela cadeia do resource server e traduzida em `401`. Provenance e alternativas descartadas em
  [`research.md`](./research.md).
- **Checagem estrutural do JWT decifrado** — validação padrão de claims temporais (`exp`, e
  demais checagens estruturais aplicáveis) sobre o claims set já decifrado; falha nessa
  checagem é tratada como falha de autenticação (`401`), no mesmo padrão de um token não
  decifrável.
- **Extração e checagem de permissões** — `PermissoesAuthenticationConverter` (implementação de
  `Converter<Jwt, AbstractAuthenticationToken>`) lê, do claims set decifrado, os papéis de
  realm (`realm_access.roles`), os papéis de client/resource (`resource_access.*.roles`) e os
  escopos (`scope`), combinando-os em uma única lista de autoridades. A presença de qualquer
  item nessa lista já satisfaz a regra de autorização deste domínio (nenhuma permissão
  específica é exigida).
- **Distinção entre falha de autenticação (401) e de autorização (403)** — a cadeia de
  segurança usa um `AuthenticationEntryPoint` para o caso de token ausente/não decifrável/
  estruturalmente inválido (`401`) e uma regra de autorização dedicada — o acesso ao endpoint
  protegido exige a lista de autoridades não vazia — associada a um `AccessDeniedHandler` para
  o caso de token válido sem nenhuma permissão (`403`). As duas falhas produzem sinais HTTP
  distintos ao chamador, conforme a regra 6 do domínio.
- **Armazenamento da chave privada local** — arquivo PEM (chave privada em formato PKCS8),
  carregado por um bean customizado a partir de um `org.springframework.core.io.Resource` do
  Spring, cujo caminho é configurável por propriedade de aplicação (aceitando os prefixos
  `classpath:` ou `file:`). O bean decodifica o Base64 do conteúdo entre os delimitadores PEM e
  constrói a `PrivateKey` via `KeyFactory` (`RSA`, `PKCS8EncodedKeySpec`). O arquivo PEM não é
  versionado no repositório, conforme a convenção de segredos de `AGENTS.md`. Provenance em
  [`research.md`](./research.md).
- **Rota e verbo do endpoint protegido** — `GET /hello`. Decisão de implementação substituível
  (a spec não fixa rota/verbo); não há convenção prévia no repositório nem restrição declarada
  pelo usuário que a contradiga.

## Data model

Não aplicável. O domínio não persiste entidades nem estado (regra 8 do domínio) — `token
Bearer (JWE)` é transitório, existente apenas durante o processamento de cada requisição.
`data-model.md` dispensado.

## External contracts

A prova de conceito não mantém documentação formal de API (Swagger/OpenAPI, Postman ou ADRs —
convenção registrada em `discovery-answers.md`/`AGENTS.md`); `contracts/` dispensado. Contrato
informal do único endpoint:

- `GET /hello`, header `Authorization: Bearer <token JWE>` obrigatório.
  - `401` — header ausente, token não decifrável com a chave privada local, ou token
    estruturalmente inválido (por exemplo, expirado).
  - `403` — token decifrado com sucesso, mas sem nenhuma permissão (papel de realm, papel de
    client/resource ou escopo) no claims set.
  - `200` — token decifrado com sucesso e ao menos uma permissão presente; corpo de resposta
    mínimo, sem dado de negócio próprio (regra 7 do domínio).

## Interface

Não aplicável — domínio exclusivamente backend, sem superfície de UI.

## Testing strategy

Não aplicável. Esta prova de conceito não mantém testes automatizados (convenção registrada em
`discovery-answers.md`/`AGENTS.md`); a validação dos critérios de aceitação do `spec.md` é
manual, exercitando o endpoint protegido com tokens emitidos pelo Keycloak.

## Optional artifacts

- `data-model.md` — dispensado: domínio sem entidades nem esquema de dados (ver "Data model").
- `research.md` — gerado: registra a provenência e as alternativas descartadas para a
  descriptografia JWE via Nimbus JOSE+JWT.
- `contracts/` — dispensado: prova de conceito sem documentação formal de API (ver "External
  contracts").
- `ui/` — dispensado: domínio exclusivamente backend (ver "Interface").

## Impact on the authoritative documentation

Sem impacto. As decisões técnicas desta unidade implementam o comportamento já descrito na
skill `validacao-autenticacao-permissoes`, sem alterá-lo por decisão deliberada; nenhuma tarefa
de atualização da documentação autoritativa nasce desta rodada.
