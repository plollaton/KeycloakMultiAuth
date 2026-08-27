# Plan: Validação da claim iat do access_token em GET /api/protected

## Stack e estrutura

Java 21, Spring Boot 4.1.x, Spring Security 7.x, `spring-boot-starter-oauth2-resource-server`,
conforme fixado em `AGENTS.md`. A unidade acrescenta uma única classe ao pacote já existente
`com.aplicacaosegura.resourceserver`, sem novo módulo, pacote ou dependência: o mesmo pacote que
hoje contém `AudienceValidator` e `ResourceServerJwtDecoderConfig`.

## Decisões técnicas

- **Novo validador de claim, no mesmo padrão do `AudienceValidator`**: uma classe
  `IssuedAtValidator`, package-private, implementando `OAuth2TokenValidator<Jwt>` no pacote
  `resourceserver`, falhando com `OAuth2Error("invalid_token", ...)` quando `Jwt#getIssuedAt()` é
  `null` ou posterior ao instante atual. `ResourceServerJwtDecoderConfig.jwtDecoder` passa a compor
  três validadores no `DelegatingOAuth2TokenValidator`: o validador padrão da autoconfiguração
  (assinatura/`iss`/`exp`), o `AudienceValidator` existente (`aud`) e este novo `IssuedAtValidator`
  (`iat`).
  Base de confirmação: reaproveita um padrão já em uso no código (`AudienceValidator`) — decisão
  assentada, sem gap.
- **Tolerância de relógio (clock skew) para comparar `iat` com o instante atual**: 60 segundos,
  igual à tolerância padrão que o `JwtTimestampValidator` da própria autoconfiguração do Spring
  Security já aplica a `exp`/`nbf` no mesmo `jwtDecoder`. Detalhes e alternativas consideradas em
  [`research.md`](./research.md).

## Modelo de dados

Não aplicável — dispensado. A unidade não introduz entidade, tabela ou estado persistente; o novo
validador é uma verificação sem estado sobre uma claim já presente no `Jwt` decodificado pela
autoconfiguração existente.

## Contratos externos

Não aplicável — dispensado. O contrato de resposta de `GET /api/protected` (`200`/`401`) não muda
de formato; a única diferença observável é mais uma causa possível de `401` (claim `iat` ausente ou
no futuro), já coberta pela descrição OpenAPI existente do endpoint ("access_token ausente ou
inválido"), sem exigir um novo contrato ou anotação adicional.

## Interface

Não aplicável — domínio exclusivamente backend, sem interface própria.

## Estratégia de testes

Esta POC não implementa testes automatizados (unitários, integração ou e2e), conforme restrição
já registrada em `AGENTS.md`. A verificação desta unidade depende de um `access_token` real emitido
por um Keycloak configurado para o ambiente, incluindo a claim `iat`, do mesmo modo que a verificação
das demais claims já validadas por este domínio. Nenhuma infraestrutura de teste é introduzida por
esta unidade.

## Artefatos opcionais

- `data-model.md`: dispensado — sem entidade ou estado novo (ver "Modelo de dados").
- `research.md`: gerado — registra a tolerância de relógio adotada para `iat`, única decisão
  técnica não trivial desta unidade.
- `contracts/`: dispensado — sem novo contrato formal (ver "Contratos externos").
- `ui/`: dispensado — sem interface (ver "Interface").

## Impacto na documentação autoritativa

A regra 7 da skill `.agents/skills/servidor-recursos-oauth2/SKILL.md` fixa hoje que a validação de
um `access_token` cobre apenas assinatura, `iss`, `exp` e `aud` — sem mencionar `iat`. A tabela de
"Entidades e dados" da mesma skill ("Access token validado por este domínio") também lista apenas
`iss`, `exp` e `aud`. Esta unidade estende deliberadamente essa validação para incluir `iat`, decisão
registrada na resposta humana ao gap `g1` da fase de spec desta unidade (redirecionamento do pedido
para este domínio, com a extensão de escopo confirmada). Trata-se de drift deliberado: a spec desta
unidade já descreve o comportamento-alvo (regra 7 estendida), e a atualização da skill — regra 7 e a
tabela de "Entidades e dados" — para refletir esse comportamento vira tarefa na fase `tasks`, a
executar junto da implementação.

A mesma extensão também torna desatualizada a frase de `AGENTS.md` ("checa as claims `iss`, `exp` e
`aud`", na seção "Convenções arquiteturais importantes"), que resume a mesma regra 7. A atualização
dessa frase para incluir `iat` acompanha, como a mesma tarefa ou uma tarefa correlata, a atualização
da skill na fase `tasks`.
