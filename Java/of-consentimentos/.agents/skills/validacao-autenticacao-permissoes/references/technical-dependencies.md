# Dependências técnicas — Validação de Autenticação e Permissões

Cada item abaixo é uma precondição técnica que o domínio precisa ter satisfeita para
entregar o comportamento descrito no `SKILL.md`. A Stage 3 (spec/plan/tasks) lê este
arquivo para checar essas precondições antes de detalhar a implementação.

- **Keycloak** — emissor dos tokens `Bearer` assinados (JWT, RS256). Sem ele não há
  credencial a validar; é a origem da identidade e das permissões consumidas pela
  aplicação.
- **Chave privada + certificado** — material criptográfico que a aplicação guarda; a
  chave privada é usada para derivar a chave pública com a qual a aplicação verifica a
  assinatura do token e libera o acesso. Sem ele, nenhuma requisição pode ser validada.
  A forma de guardar/carregar essa chave (keystore, arquivo, variável de ambiente) ainda
  não foi definida nas fontes investigadas; é uma decisão de implementação a ser tomada
  na especificação técnica desta prova de conceito.
- **Spring Security (resource server)** — camada que intercepta as requisições, extrai o
  header `Bearer` e aplica a verificação de assinatura/autorização em cada endpoint;
  sem ela o endpoint "hello world" ficaria desprotegido.
- **Configuração, no Keycloak, do client desta aplicação para emitir tokens assinados**
  — o Keycloak precisa ter o certificado desta aplicação registrado e o client
  configurado para assinar o token de acesso com ele. Sem essa configuração do lado do
  Keycloak, nenhum token chega com assinatura verificável por esta aplicação, mesmo que
  a chave privada correspondente esteja corretamente carregada.
- **Configuração, no Keycloak, de ao menos um papel/escopo (permissão) atribuído ao
  portador** — o realm/client do Keycloak precisa atribuir a quem for autenticar ao
  menos uma permissão (papel de realm, papel de client/resource ou escopo), já que
  qualquer uma delas é suficiente para a checagem deste domínio. Sem nenhuma permissão
  atribuída, o portador tem o acesso negado mesmo com a assinatura do token
  corretamente verificada.
