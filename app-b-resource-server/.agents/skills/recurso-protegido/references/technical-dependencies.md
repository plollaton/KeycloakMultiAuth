# Dependências técnicas — Recurso protegido (API demonstrativa)

Cada item descreve o que a dependência é e o que ela afeta no domínio se estiver ausente
ou indisponível. Este arquivo é lido para checar as precondições técnicas do domínio antes
de gerar spec/plan/tasks.

- **Mecanismo de autenticação/validação offline do token JWT** — o recurso só recebe uma
  requisição a atender porque o token Bearer já foi validado e um principal portador dos
  claims foi produzido antes de o endpoint ser executado. Sem esse mecanismo, o endpoint fica
  inacessível ou, pior, aberto sem verificação; e é dele (não deste domínio) a fonte dos
  metadados devolvidos (subject, clientId, issuer, audience, scope, expiresAt).

- **Camada web REST (servidor de aplicação HTTP)** — expõe a rota `GET /api/protegido` e
  encaminha a requisição ao recurso. Sem ela não há endpoint HTTP publicado.

- **Serialização JSON (via a camada web)** — converte o conjunto de metadados no corpo JSON da
  resposta, incluindo a lista de `audience` e o instante de `expiresAt`. Sem ela o contrato de
  resposta não é produzido no formato esperado.

- **Documentação OpenAPI/Swagger gerada a partir do código (a adicionar)** — a dependência que
  gera/serve a documentação OpenAPI/Swagger ainda **não** está presente no projeto e deve ser
  incluída (decisão registrada na descoberta). Sem ela, o endpoint deste domínio não fica
  descrito pelo contrato de API que o projeto decidiu manter; o comportamento do recurso em si
  não é afetado, apenas a sua documentação.

---

> **Manutenção deste arquivo**
>
> Atualize-o sempre que uma dependência técnica do domínio for adicionada, removida ou
> mudar de efeito. Uma divergência entre o descrito aqui e o comportamento real do domínio
> sem decisão registrada que a resolva é escalada para decisão humana.
