---
name: cliente-oauth2-client-assertion-technical-dependencies
description: Dependências técnicas do domínio Cliente OAuth2 com Client Assertion JWT — o que cada uma é e o que fica comprometido na sua ausência.
metadata:
  author: clovis-cli
  type: domain-skill-reference
---

# Dependências técnicas — Cliente OAuth2 com Client Assertion JWT

> **Mantendo este documento**
>
> Atualizar sempre que uma dependência técnica deste domínio for adicionada,
> removida ou substituída. Um refactor que preserva a mesma dependência (troca
> de versão sem mudança de contrato, por exemplo) não exige atualização.

- **Keycloak** — expõe o endpoint `/oauth2/token` que recebe o
  `client_assertion` e emite o `access_token`; sem ele não há como testar ou
  completar o fluxo.
- **Gestão de Chaves RSA e Publicação JWKS** — fornece a chave privada
  necessária para assinar o `client_assertion`; sem ela este domínio não tem o
  que assinar.
- **Nimbus JOSE+JWT** — biblioteca que constrói e assina o `client_assertion`
  no formato JWT exigido pelo Keycloak; sem ela não há como montar o JWT no
  contrato esperado.
- **`spring-boot-starter-oauth2-client`** — starter que fornece o suporte de
  client OAuth2 (registro do client e execução do grant `client_credentials`
  com autenticação via JWT assertion); sem ele a chamada ao endpoint de token
  do Keycloak precisaria ser implementada manualmente, fora do padrão do
  framework fixado para o projeto.
- **Configuração do client no Keycloak para autenticação via JWT assinado**
  (client registrado com o método de autenticação de client baseado em JWT
  assinado, com a fonte de validação apontando para o endpoint de publicação
  da chave pública desta aplicação) — sem essa configuração do lado do
  Keycloak, o Keycloak não sabe onde buscar a chave pública para validar o
  `client_assertion` recebido, e a troca por `access_token` falha
  independentemente do que esta aplicação enviar.
