# Research: Servidor de Recursos OAuth2

## Mecanismo de validação do access_token

**Contexto.** A skill de domínio exige, para todo `access_token` apresentado a um endpoint
protegido, validar cumulativamente assinatura (contra o JWKS do Keycloak), `iss`, `exp` e `aud`
(regras 6 e 7). O `AGENTS.md` já fixa `spring-boot-starter-oauth2-resource-server` sem reserva
como dependência da stack.

**Alternativas:**

- **`spring.security.oauth2.resourceserver.jwt.issuer-uri`** — autoconfiguração padrão do starter:
  descobre o JWKS do Keycloak via o endpoint de metadados do `issuer-uri` configurado, monta o
  `JwtDecoder` e já aplica, sem código adicional, a validação de assinatura, `exp` e `iss`. Só a
  validação de `aud` fica fora da autoconfiguração e precisa de um `OAuth2TokenValidator`
  customizado, combinado ao validador padrão via `DelegatingOAuth2TokenValidator` em um bean
  `JwtDecoder` explícito — padrão documentado pelo próprio Spring Security para validar audiências
  confiáveis.
- **`spring.security.oauth2.resourceserver.jwt.jwk-set-uri`** — aponta diretamente para o endpoint
  JWKS do Keycloak, sem descoberta de metadados; a autoconfiguração resultante valida apenas
  assinatura e `exp`, exigindo um `OAuth2TokenValidator` customizado tanto para `iss` quanto para
  `aud`.

**Decisão:** `issuer-uri`.

**Confirmação:** `spring-boot-starter-oauth2-resource-server` está fixado sem ressalva em
`AGENTS.md`; `issuer-uri` é o mecanismo de configuração padrão desse starter para o cenário
descrito pela skill de domínio (validar um `access_token` de um emissor OAuth2/OIDC único), e é o
que deixa mais das quatro verificações da regra 6 a cargo da autoconfiguração do framework,
restando código próprio apenas para `aud` em ambas as alternativas — não há alternativa
concorrente que reduza mais esse código sem se afastar do suporte nativo do starter já fixado.

**Consequências:** a aplicação depende do endpoint de metadados do `issuer-uri` do Keycloak estar
acessível na inicialização, para a descoberta do JWKS; o valor concreto de `issuer-uri`
(`spring.security.oauth2.resourceserver.jwt.issuer-uri`) e o de `aud` esperado
(`app.security.resource-server.expected-audience`) ficam definidos por ambiente, sem valor fixado
pelo material de negócio (skill de domínio, regra 6).

## Endpoints de exemplo e corpo de resposta

**Contexto.** A skill de domínio fixa apenas a classificação de acesso de `GET /api/protected`
(protegido) e `GET /api/public` (público), sem definir um contrato de corpo de resposta para
nenhum dos dois (`spec.md`, seção "Risks and observations").

**Alternativas:**

- **Corpo mínimo evidenciando a classificação** — cada endpoint responde `200` com um JSON curto
  que apenas identifica se o acesso foi tratado como protegido ou público (ex.: `{"acesso":
  "protegido"}`), suficiente para confirmar visualmente qual caminho da `SecurityFilterChain` foi
  percorrido.
- **Corpo vazio (`204`)** — confirma o status de acesso sem corpo.

**Decisão:** corpo mínimo evidenciando a classificação, com status `200`.

**Confirmação:** nenhuma das duas opções tem base documental — decisão de implementação para um
endpoint de exemplo sem contrato de negócio fixado, mantendo o mesmo padrão de status HTTP (`200`)
já usado nos demais endpoints de exemplo/diagnóstico do projeto (`GET /oauth2/jwks`), em vez de
introduzir um `204` que nenhum outro endpoint do projeto usa.

**Consequências:** o corpo de resposta desses dois endpoints pode ser ajustado livremente no
futuro sem impacto de negócio, já que a skill de domínio não fixa um contrato para eles — apenas a
classificação de acesso (protegido/público) é comportamento observável exigido.
