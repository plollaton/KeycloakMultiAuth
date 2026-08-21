# Test cases: Validação de Autenticação e Permissões

## Preconditions

- Aplicação em execução localmente, com o arquivo PEM da chave privada configurado no caminho
  esperado pela propriedade de aplicação (`plan.md` — "Armazenamento da chave privada local").
- Client desta aplicação configurado no Keycloak para emitir tokens de acesso cifrados (JWE) com
  o certificado correspondente à chave privada carregada pela aplicação (precondição externa
  registrada em `spec.md` — "Cross-domain dependencies").
- Ao menos um usuário de teste no Keycloak com um papel de realm, um papel de client/resource ou
  um escopo atribuído — para obter um token com permissão — e, para os casos de negação por
  autorização, um usuário de teste sem nenhum papel/escopo atribuído.
- Um token JWE emitido para um certificado diferente do carregado pela aplicação (chave
  incompatível) e um token corrompido/malformado (por exemplo, truncando a string compact de um
  token válido), disponíveis para os casos de falha de descriptografia.
- Um token JWE de curta duração (ou já expirado) disponível para o caso de falha estrutural do
  JWT.

## Story 1 — Header obrigatório

### TC-1 (mandatory) — Acesso sem o header Authorization

1. Envie uma requisição `GET /hello` sem o header `Authorization`.

**Expected:** a aplicação responde `401` (falha de autenticação), conforme `spec.md`.

## Story 2 e 3 — Descriptografia e validade do token

### TC-2 (mandatory) — Token corrompido ou malformado

1. Envie uma requisição `GET /hello` com o header `Authorization: Bearer <token>`, usando o
   token corrompido/malformado das precondições.

**Expected:** a aplicação responde `401` (falha de autenticação), conforme `spec.md`.

### TC-3 (recommended) — Token cifrado com chave incompatível

1. Envie uma requisição `GET /hello` com o header `Authorization: Bearer <token>`, usando o
   token emitido para um certificado diferente do carregado pela aplicação.

**Expected:** a aplicação responde `401` (falha de autenticação) — a descriptografia com a chave
privada local falha, conforme `spec.md`.

### TC-4 (mandatory) — Token decifrável, porém estruturalmente inválido (expirado)

1. Envie uma requisição `GET /hello` com o header `Authorization: Bearer <token>`, usando o
   token expirado das precondições.

**Expected:** a aplicação responde `401` (falha de autenticação), mesmo com a descriptografia do
token bem-sucedida, conforme `spec.md`.

## Story 4 e 5 — Checagem de permissões

### TC-5 (mandatory) — Acesso liberado com permissão presente no token

1. Obtenha, junto ao Keycloak, um token para o usuário de teste com permissão atribuída.
2. Envie uma requisição `GET /hello` com o header `Authorization: Bearer <token>`, usando esse
   token.

**Expected:** a aplicação responde `200`, com corpo mínimo sem dado de negócio próprio, conforme
`spec.md` e `plan.md` ("External contracts").

### TC-6 (recommended) — Acesso liberado com permissão via papel de client/resource

1. Obtenha, junto ao Keycloak, um token para um usuário de teste cuja única permissão atribuída
   seja um papel de client/resource (`resource_access.*.roles`), sem papel de realm nem escopo.
2. Envie uma requisição `GET /hello` com o header `Authorization: Bearer <token>`, usando esse
   token.

**Expected:** a aplicação responde `200`, conforme a regra de que qualquer um dos três tipos de
permissão (papel de realm, papel de client/resource ou escopo) já libera o acesso (`spec.md`,
`plan.md` — "Extração e checagem de permissões").

### TC-7 (recommended) — Acesso liberado com permissão via escopo

1. Obtenha, junto ao Keycloak, um token para um usuário de teste cuja única permissão atribuída
   seja um escopo (`scope`), sem papel de realm nem papel de client/resource.
2. Envie uma requisição `GET /hello` com o header `Authorization: Bearer <token>`, usando esse
   token.

**Expected:** a aplicação responde `200`, conforme a regra de que qualquer um dos três tipos de
permissão já libera o acesso (`spec.md`, `plan.md` — "Extração e checagem de permissões").

### TC-8 (mandatory) — Acesso negado por ausência de permissão

1. Obtenha, junto ao Keycloak, um token para o usuário de teste sem nenhum papel/escopo
   atribuído.
2. Envie uma requisição `GET /hello` com o header `Authorization: Bearer <token>`, usando esse
   token.

**Expected:** a aplicação responde `403` (falha de autorização) — sinal distinto do `401` das
demais falhas, já que o token foi decifrado com sucesso e apenas a checagem de permissões
falhou, conforme `spec.md`.
