# Test cases: Gestão de Chaves RSA e Publicação JWKS

Esta POC não implementa testes automatizados (`discovery-answers.md`); este catálogo serve como
roteiro de validação manual do domínio e como referência de cobertura na fase de implementação.

## Preconditions

- Aplicação em execução (`mvn spring-boot:run`), com os arquivos `api-b.pem` e
  `api-b-cert.pem` presentes em `src/main/resources`, exceto quando o caso indicar o contrário.
- Cliente HTTP capaz de fazer requisições sem enviar nenhuma credencial (ex.: `curl`).
- Para os casos que envolvem rotação: um segundo par de arquivos `api-b.pem`/
  `api-b-cert.pem` válido, com um par de chaves RSA distinto do par original, para substituir os
  arquivos originais antes de reiniciar a aplicação.

## Story 1 — Publicação pública da chave pública

### TC-1 (mandatory) — Endpoint responde publicamente no formato JWK Set

1. Com a aplicação no ar e sem enviar nenhuma credencial, chame `GET /oauth2/jwks`.

**Expected:** resposta `200` com corpo JSON no formato `{"keys": [...]}`.

### TC-2 (mandatory) — Conteúdo da chave pública publicada corresponde ao par ativo

1. Sem enviar nenhuma credencial, chame `GET /oauth2/jwks`.
2. Inspecione o único item do array `keys`.

**Expected:** o item traz `kty: "RSA"`, `use: "sig"`, `alg: "RS256"`, `kid` igual ao identificador
do par de chaves ativo, e `n`/`e` correspondentes ao módulo e ao expoente público da chave RSA
ativa, codificados em Base64URL.

### TC-3 (mandatory) — Chave privada nunca é exposta

1. Sem enviar nenhuma credencial, chame `GET /oauth2/jwks`.
2. Inspecione todos os campos do corpo da resposta.

**Expected:** o corpo contém, em cada item de `keys`, apenas os campos `kty`, `kid`, `use`, `alg`,
`n` e `e` — nenhum campo relativo à chave privada aparece na resposta.

### TC-4 (mandatory) — Publicação traz exatamente uma chave

1. Sem enviar nenhuma credencial, chame `GET /oauth2/jwks`.
2. Conte os itens do array `keys`.

**Expected:** o array `keys` contém exatamente um item — nunca chaves de outras aplicações do
cenário nem chaves de rotações anteriores.

## Story 2 — Rotação de chaves refletida automaticamente

### TC-5 (mandatory) — Rotação via substituição do par de chaves fixo

1. Com a aplicação no ar, chame `GET /oauth2/jwks` e registre o `kid` e o `n` retornados.
2. Pare a aplicação, substitua os arquivos `api-b.pem` e `api-b-cert.pem` em
   `src/main/resources` por um novo par de chaves válido, e suba a aplicação novamente.
3. Chame novamente `GET /oauth2/jwks`.

**Expected:** o `kid` e o `n` retornados no passo 3 são diferentes dos registrados no passo 1, sem
exigir nenhuma ação adicional de republicação.

## Story 3 — Carregamento do par de chaves fixo

### TC-6 (mandatory) — Carregamento do par de chaves fixo na inicialização

1. Com os arquivos `api-b.pem` e `api-b-cert.pem` presentes em `src/main/resources`, suba
   a aplicação.
2. Chame `GET /oauth2/jwks`.

**Expected:** a resposta traz o par de chaves desses arquivos, com `alg: "RS256"` e `kid` igual ao
número de série do certificado `api-b-cert.pem`.

### TC-7 (mandatory) — Par de chaves estável entre reinicializações sem alteração dos arquivos

1. Com a aplicação no ar, chame `GET /oauth2/jwks` e registre o `kid` e o `n` retornados.
2. Reinicie a aplicação sem alterar os arquivos `api-b.pem`/`api-b-cert.pem`.
3. Chame novamente `GET /oauth2/jwks`.

**Expected:** o `kid` e o `n` retornados no passo 3 são idênticos aos registrados no passo 1.

## Story 4 — Documentação OpenAPI/Swagger

### TC-9 (mandatory) — Contrato OpenAPI descreve o endpoint JWKS e é acessível publicamente

1. Sem enviar nenhuma credencial, chame `GET /v3/api-docs`.
2. Localize, no contrato retornado, a operação correspondente a `GET /oauth2/jwks`.

**Expected:** resposta `200`; a operação aparece descrita, incluindo o schema de resposta com os
campos `kty`, `kid`, `use`, `alg`, `n` e `e`.

### TC-10 (mandatory) — Swagger UI acessível sem autenticação

1. Sem enviar nenhuma credencial, acesse `GET /swagger-ui.html`.

**Expected:** a página carrega com resposta `200`, sem exigir autenticação.
