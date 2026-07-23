# App A + Keycloak + App B — client_credentials com JWT (JWKS local)

## O que foi implementado

Foi implementada a **Abordagem 1** (segredo compartilhado): a App A é um client confidencial
no Keycloak, autentica via `client_credentials` usando `client_id` + `client_secret`, recebe
um access token (JWT) e chama a App B com `Authorization: Bearer <token>`. A App B valida a
assinatura do token **offline**, a partir de um JWKS (chaves públicas do Keycloak) mantido em
uma pasta local, em vez de consultar o Keycloak a cada requisição.

Fluxo:

```
App A --client_credentials (client_id+secret)--> Keycloak
App A <--------------- access token (JWT) -------------- Keycloak
App A ------ GET /api/protegido, Authorization: Bearer <jwt> -----> App B
App B: valida assinatura/iss/aud/exp usando JWKS lido de arquivo local
```

## Validação da explicação que você recebeu

A explicação do consultor sobre a Abordagem 2 (`private_key_jwt`/mTLS) está correta: ali a
App A assina uma prova com sua própria chave privada, o Keycloak só guarda a chave pública/
certificado, e não há segredo compartilhado — a "rotação" é de par de chaves/certificado, não
de senha. É a opção mais forte e a mais comum em exigências de segurança de instituições
financeiras, ao custo de uma gestão de chaves/PKI.

Você optou por implementar a **Abordagem 1** (`client_secret`) primeiro, o que é uma escolha
razoável para validar o fluxo ponta a ponta rapidamente. Dois pontos para levar de volta ao
consultor de segurança:

1. Do ponto de vista da App B, os dois casos são idênticos: ela só verifica a assinatura do
   JWT com a chave pública do Keycloak. Toda a diferença de segurança está em como a App A
   se autentica no Keycloak (segredo vs. chave privada), não em como a App B valida o token.
2. Se o requisito de "sem segredo compartilhado" veio como exigência formal de segurança
   (e não só como sugestão), a Abordagem 1 não a atende — o `client_secret` continua sendo
   um segredo estático que, se vazar, permite qualquer chamador se passar pela App A. Nesse
   caso, o próximo passo natural é migrar a App A para `private_key_jwt` (ou mTLS), mantendo
   a App B exatamente como está.

## Estrutura entregue

Por causa de uma instabilidade temporária no ambiente de execução (sem espaço para o sandbox
Linux subir), os projetos foram entregues como **scripts geradores** em vez de árvores de
pasta prontas — rodar cada script recria a estrutura Maven completa, arquivo por arquivo:

- `build-app-a-client.sh` → gera `app-a-client/` (cliente OAuth2, client_credentials)
- `build-app-b-resource-server.sh` → gera `app-b-resource-server/` (resource server, JWKS local)
- `sync-jwks.sh` → script standalone que sincroniza o JWKS do Keycloak para a pasta local da App B

Para materializar os projetos (Linux/macOS/WSL/Git Bash, ou dentro de um container com bash):

```bash
bash build-app-a-client.sh
bash build-app-b-resource-server.sh
```

Isso cria dois projetos Maven completos, prontos para abrir na IDE:

```
app-a-client/
  pom.xml
  src/main/resources/application.yml
  src/main/java/com/dbfinanceira/appa/
    AppAClientApplication.java
    config/OAuth2RestClientConfig.java
    client/AppBClient.java
    web/DemoController.java

app-b-resource-server/
  pom.xml
  src/main/resources/application.yml
  src/main/java/com/dbfinanceira/appb/
    AppBResourceServerApplication.java
    config/AppBSecurityProperties.java
    config/JwtDecoderConfig.java
    config/SecurityConfig.java
    security/LocalFileJWKSource.java
    web/SecureController.java
```

Stack: Java 21, Spring Boot 4.0.6 (Spring Framework 7 / Spring Security 7), sem Undertow
(removido no Boot 4 — os projetos usam Tomcat, o padrão), CSRF desabilitado explicitamente
na App B porque o Security 7 passou a habilitá-lo por padrão também para APIs stateless.

**Importante**: não foi possível rodar `mvn verify`/`spring-boot:run` neste ambiente (mesma
instabilidade de sandbox). Recomendo compilar e subir localmente antes de considerar pronto
para o consultor revisar.

## Configuração necessária no Keycloak

### Client da App A (`app-a`)

- **Client authentication**: ligado (client confidencial)
- **Authentication flow**: apenas "Service accounts roles" ligado (habilita `client_credentials`);
  desligue "Standard flow" e "Direct access grants"
- **Client secret**: gere um, guarde em cofre de segredos (Vault/KMS), nunca em `application.yml`
- **Audience Mapper** (passo que costuma faltar e quebra a validação na App B): crie um client
  scope (ex.: `app-b-audience`) com um mapper do tipo *Audience*, "Included Client Audience" =
  `app-b`, e associe esse client scope como *Default* no client `app-a`. Sem isso, o token
  emitido normalmente só contém `"aud": "account"`, e o validador de audience da App B vai
  rejeitar toda chamada.
- Prazo de expiração do access token: mantenha curto (ex.: 5 minutos) já que é uso M2M.

### App B

Não precisa de client próprio no Keycloak para este fluxo (ela só valida tokens). Se quiser
introspecção adicional ou revogação, considere um client separado só de leitura — fora do
escopo desta implementação.

## Variáveis de ambiente

App A (`app-a-client`):

| Variável | Descrição |
|---|---|
| `KEYCLOAK_ISSUER_URI` | ex.: `https://keycloak.exemplo.com/realms/financeiro` |
| `APP_A_CLIENT_ID` | `app-a` |
| `APP_A_CLIENT_SECRET` | segredo gerado no Keycloak (via cofre, não em texto puro) |
| `APP_B_BASE_URL` | ex.: `http://app-b:8082` |

App B (`app-b-resource-server`):

| Variável | Descrição |
|---|---|
| `KEYCLOAK_ISSUER_URI` | mesmo valor usado pela App A |
| `APP_B_EXPECTED_AUDIENCE` | `app-b` (deve bater com o Audience Mapper) |
| `APP_B_JWKS_PATH` | pasta/arquivo específico onde o JWKS deve estar, ex.: `/etc/app-b/jwks/jwks.json` |
| `APP_B_CLOCK_SKEW_SECONDS` | tolerância de relógio, padrão `60` |

## Sincronizando o JWKS local (a "pasta específica")

`sync-jwks.sh` busca `GET {issuer}/protocol/openid-connect/certs` no Keycloak e grava o
resultado atomicamente (arquivo temporário + `mv`) em `APP_B_JWKS_PATH`. A `LocalFileJWKSource`
usada pela App B recarrega o arquivo automaticamente quando o `mtime` muda — não é preciso
reiniciar a aplicação após uma rotação de chave no Keycloak, desde que o arquivo seja
atualizado a tempo.

```bash
KEYCLOAK_ISSUER_URI=https://keycloak.exemplo.com/realms/financeiro \
JWKS_OUTPUT_DIR=/etc/app-b/jwks \
bash sync-jwks.sh
```

Agende via cron ou systemd timer (ex.: a cada 5–15 minutos):

```
*/10 * * * * KEYCLOAK_ISSUER_URI=... JWKS_OUTPUT_DIR=/etc/app-b/jwks /opt/app-b/sync-jwks.sh >> /var/log/sync-jwks.log 2>&1
```

**Risco a monitorar**: se esse processo parar e o Keycloak rotacionar a chave de assinatura,
tokens novos (com `kid` desconhecido) passam a ser rejeitados pela App B mesmo sendo válidos.
`LocalFileJWKSource.getLastLoadedAt()` está exposto no código para virar um health indicator
customizado (`/actuator/health`) que alarme se o JWKS estiver "velho" demais.

## Testando localmente

```bash
mvn -f app-a-client spring-boot:run &
mvn -f app-b-resource-server spring-boot:run &
curl http://localhost:8081/demo/chamar-app-b
```

Se tudo estiver configurado (Keycloak + Audience Mapper + JWKS sincronizado), a resposta vem
da App B confirmando o `subject`, `clientId`, `issuer`, `audience` e `scope` do token validado.

## Checklist de segurança antes de ir para produção (instituição financeira)

- `client_secret` da App A em cofre de segredos com rotação periódica automatizada — nunca em
  repositório de código ou variável de ambiente em texto puro no manifesto de deploy.
- Avaliar com o consultor a migração da App A para `private_key_jwt` ou mTLS (Abordagem 2),
  especialmente se "sem segredo compartilhado" for requisito formal, não só preferência.
- TLS obrigatório em todos os saltos: App A → Keycloak, App A → App B, `sync-jwks.sh` → Keycloak.
- Nunca logar o JWT bruto (nem nos logs da App A ao chamar a App B, nem nos da App B ao validar)
  — logar apenas metadados não sensíveis (`sub`, `azp`, `jti`, timestamps).
- Validar `iss`, `aud` e timestamps (implementado); se o endpoint precisar de granularidade por
  operação, validar também `scope`/roles do token, não só autenticidade.
- Alarmar sobre "JWKS local desatualizado" (idade do arquivo) e sobre falhas do `sync-jwks.sh`.
- Definir expiração curta para o access token de client_credentials no Keycloak.
