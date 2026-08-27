# Research: Validação da claim iat do access_token em GET /api/protected

## Tolerância de relógio para comparar iat com o instante atual

**Contexto.** O novo `IssuedAtValidator` rejeita um `access_token` cuja claim `iat` esteja no
futuro em relação ao instante da validação. Comparar `iat` contra `Instant.now()` sem nenhuma
tolerância torna a verificação sensível a uma pequena diferença de relógio entre o Keycloak (que
emite o token) e esta aplicação (que o valida) — um cenário plausível entre processos distintos,
ainda que na mesma rede.

**Alternativas:**

- **Sem tolerância (comparação estrita contra `Instant.now()`)** — mais simples, mas rejeita
  tokens válidos sempre que o relógio do Keycloak estiver levemente à frente do desta aplicação.
- **Tolerância própria, configurável por `@Value`** — flexível por ambiente, mas introduz uma nova
  propriedade de configuração sem valor fixado pelo material de negócio, exigindo decisão de
  negócio sobre o valor padrão.
- **Tolerância fixa de 60 segundos, igual à já aplicada pelo `JwtTimestampValidator` da
  autoconfiguração a `exp`/`nbf`** — mesmo `jwtDecoder` já convive com essa tolerância para as
  claims de tempo que valida hoje; usar o mesmo valor para `iat` mantém a verificação de tempo
  consistente dentro do mesmo componente, sem introduzir uma nova constante de negócio.

**Decisão:** tolerância fixa de 60 segundos.

**Base de confirmação:** o `JwtTimestampValidator` da autoconfiguração do
`spring-boot-starter-oauth2-resource-server`, já composto pelo `jwtDecoder` existente
(`ResourceServerJwtDecoderConfig`) para validar `exp`/`nbf`, usa por padrão essa mesma tolerância de
60 segundos — decisão assentada por já estar em uso, por composição, no próprio `jwtDecoder` que o
`IssuedAtValidator` passa a integrar; não é uma constante de negócio nova, apenas a mesma tolerância
de relógio já implicitamente aplicada às demais claims de tempo deste `access_token`.

**Consequências:** a verificação de `iat` fica consistente com a de `exp`/`nbf` no mesmo `jwtDecoder`
— nenhuma tolerância nova ou dessincronizada é introduzida, e nenhuma propriedade de configuração
adicional é necessária.
