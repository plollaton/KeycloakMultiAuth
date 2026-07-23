# Dependências técnicas — Integração com a App B

> **Manutenção deste arquivo**
>
> Atualize sempre que uma pré-condição técnica do domínio for adicionada, removida ou
> mudar de natureza. Cada item descreve o que a dependência é e o que deixa de
> funcionar no domínio se ela estiver ausente ou mal configurada. Divergência entre
> o descrito aqui e o comportamento implementado, sem decisão registrada que a
> resolva, é lacuna a escalar para decisão humana.

Cada item lista uma pré-condição técnica necessária para o domínio entregar seu
comportamento completo e utilizável (consumir o endpoint protegido da App B com o
bearer token anexado automaticamente e expor o endpoint de demonstração do fluxo).

- **App B** — serviço REST a jusante que expõe `GET /api/protegido` e valida o JWT
  via JWKS; sem ele não há destino para a integração e a chamada de demonstração não
  tem o que consumir. O contrato de `/api/protegido` (formato do corpo, códigos de
  resposta) pertence à App B; esta aplicação o trata de forma opaca.

- **Domínio de Autenticação Máquina-a-Máquina** — fornece o access token (JWT)
  anexado automaticamente pelo interceptor de credencial a cada chamada de saída;
  sem ele as requisições à App B seriam rejeitadas (401). A produção e a renovação
  do token são responsabilidade desse domínio; este apenas consome a credencial já
  disponível.

- **Spring Web `RestClient`** (`spring-boot-starter-web`) — cliente HTTP usado para
  chamar a App B e para expor o endpoint de demonstração de entrada; sem ele não há
  como emitir a chamada a jusante nem publicar a rota de gatilho.

- **Configuração externalizada** (`APP_B_BASE_URL`) — define o endereço-base da App B
  alvo da integração; se ausente, usa o default `http://localhost:8082`, adequado
  apenas a desenvolvimento. Sem apontar para a App B correta, a chamada de saída
  falha ou atinge um destino indevido.
