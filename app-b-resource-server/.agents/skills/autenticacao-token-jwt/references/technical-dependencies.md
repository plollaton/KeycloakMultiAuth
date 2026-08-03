# Dependências técnicas — Autenticação e validação offline do token JWT

Cada item descreve o que a dependência é e o que ela afeta no domínio se estiver ausente
ou indisponível. Este arquivo é lido para checar as precondições técnicas do domínio antes
de gerar spec/plan/tasks.

- **Keycloak (emissor dos tokens)** — emite os access tokens (JWT) que este domínio valida e
  determina os valores esperados de `iss` e `aud`. Sem alinhamento com o realm e com o
  **Audience Mapper** do Keycloak, os tokens não trazem os claims esperados (por padrão o
  `aud` vem apenas como `account`) e **nenhuma requisição autentica**, ainda que os tokens
  sejam legítimos.

- **Fonte local de chaves públicas (JWKS)** — disponibiliza, em memória a partir de arquivo
  local, o conjunto de chaves públicas usado para verificar a assinatura dos tokens. Sem essa
  fonte de chaves, o decoder não consegue conferir assinatura alguma e toda validação falha;
  se as chaves estiverem desatualizadas em relação à rotação no Keycloak, tokens novos (com
  `kid` desconhecido) são rejeitados. A obtenção e recarga desse material pertencem ao
  domínio de gestão local do JWKS.

- **Pipeline de resource server OAuth2** — provê o encaixe de validação de JWT nas rotas
  protegidas (extração do Bearer token, orquestração dos validadores e resposta 401 em
  falha). Sem ele não há ponto de aplicação da autenticação sobre as requisições.

- **Biblioteca de verificação JOSE/JWT (Nimbus JOSE)** — executa a verificação criptográfica
  da assinatura do token contra o material de chaves e a seleção de chave por `kid`. Sem ela
  não há como validar assinaturas.

- **Configuração externalizada de segurança (variáveis de ambiente)** — fornece o emissor
  esperado (`iss`), a audience esperada (`aud`) e a tolerância de relógio (clock skew) usados
  pelos validadores. Erro ou ausência de configuração aqui bloqueia toda a autenticação: um
  `iss`/`aud` incorreto rejeita todos os tokens.

---

> **Manutenção deste arquivo**
>
> Atualize-o sempre que uma dependência técnica do domínio for adicionada, removida ou
> mudar de efeito. Uma divergência entre o descrito aqui e o comportamento real do domínio
> sem decisão registrada que a resolva é escalada para decisão humana.
