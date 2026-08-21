# Tasks: Validação de Autenticação e Permissões

- [x] **T1. Endpoint protegido com descriptografia JWE e negação por falha de autenticação**
  - Depends on: none
  - Precondition: o client desta aplicação no Keycloak já configurado para emitir tokens de
    acesso cifrados (JWE) com o certificado correspondente à chave privada local (fora do
    escopo desta unidade — ver `spec.md`, "Cross-domain dependencies").
  - Bootstrap do projeto Spring Boot (`pom.xml` com Java 21, Maven Wrapper, dependências de web
    e de resource server, classe de aplicação) como primeiro passo desta tarefa, por ser o
    primeiro uso.
  - Bean que carrega a chave privada local a partir do arquivo PEM configurado e a expõe como
    `PrivateKey`, per `plan.md` ("Armazenamento da chave privada local").
  - `JweTokenDecoder`: descriptografa o token `Bearer` recebido com essa chave privada e aplica
    as checagens estruturais do JWT decifrado (por exemplo, expiração), per `plan.md`
    ("Interceptação e descriptografia do token", "Checagem estrutural do JWT decifrado").
  - `SecurityConfig` protegendo `GET /hello`: exige o header `Authorization: Bearer <token>` e
    sinaliza `401` quando ausente, quando o token não é decifrável ou quando falha na checagem
    estrutural.
  - `HelloWorldController` expondo `GET /hello`, respondendo com o corpo mínimo descrito em
    `plan.md` ("External contracts") quando o token é aceito.

- [x] **T2. Checagem de permissões com negação por falha de autorização**
  - Depends on: T1
  - `PermissoesAuthenticationConverter`: extrai, do claims set já decifrado por `JweTokenDecoder`,
    os papéis de realm, os papéis de client/resource e os escopos do portador, combinando-os em
    uma única lista de autoridades, per `plan.md` ("Extração e checagem de permissões").
  - Regra de autorização sobre `GET /hello` exigindo essa lista não vazia, com um
    `AccessDeniedHandler` que sinaliza `403` quando o token é aceito por `JweTokenDecoder` mas
    não carrega nenhuma permissão — sinal distinto do `401` produzido em T1.
