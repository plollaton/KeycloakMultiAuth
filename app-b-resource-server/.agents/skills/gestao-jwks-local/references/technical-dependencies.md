# Dependências técnicas — Gestão do JWKS local

Cada item descreve o que a dependência é e o que ela afeta no domínio se estiver ausente
ou indisponível. Este arquivo é lido para checar as precondições técnicas do domínio antes
de gerar spec/plan/tasks.

- **Processo externo de sincronização do JWKS (script standalone)** — popula/atualiza o
  arquivo JWKS lido por este domínio, buscando as chaves no endpoint de certs do Keycloak e
  gravando o arquivo local de forma atômica (arquivo temporário + `mv`) somente após
  validar que o conteúdo é um JSON com a lista `keys` não vazia. Se parar e o Keycloak
  rotacionar a chave de assinatura, tokens novos (com `kid` desconhecido) passam a ser
  rejeitados mesmo sendo válidos. Sua escrita atômica + validação é o que impede que a
  aplicação leia um arquivo parcial ou corrompido durante o hot-reload.

- **Keycloak (endpoint `/protocol/openid-connect/certs`)** — fonte original das chaves
  públicas; consumido pelo processo de sincronização, não pela aplicação diretamente. Sem
  essa fonte, o processo de sync não tem de onde obter o material de chaves e o arquivo
  local não é populado/atualizado.

- **Biblioteca de parsing JOSE/JWK (Nimbus JOSE — `com.nimbusds.jose.jwk.JWKSet`)** — faz
  o parse/carga do arquivo JWKS e a seleção de chaves; sem ela não há como interpretar o
  material de chaves nem servi-lo para a verificação de assinatura.

- **Sistema de arquivos / volume montado no caminho do JWKS (`APP_B_JWKS_PATH`)** — a
  pasta/arquivo específica precisa existir e ser legível pela aplicação; é o contrato de
  integração físico com o processo de sincronização. Se ausente ou ilegível na subida, a
  aplicação falha ao iniciar (fail-fast); se a leitura falhar transitoriamente em operação,
  o domínio mantém a última versão válida em memória.

---

> **Manutenção deste arquivo**
>
> Atualize-o sempre que uma dependência técnica do domínio for adicionada, removida ou
> mudar de efeito. Uma divergência entre o descrito aqui e o comportamento real do domínio
> sem decisão registrada que a resolva é escalada para decisão humana.
