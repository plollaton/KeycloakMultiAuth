//package com.dbfinanceira.appa.web;
//
//import com.dbfinanceira.appa.credential.KeyManager;
//import com.dbfinanceira.appa.credential.KeyManager.RotationResult;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.media.Schema;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.springframework.http.MediaType;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.time.Instant;
//
///**
// * Gera/rotaciona o par de chaves da aplicacao (dominio Autenticacao Maquina-a-Maquina). A rotacao
// * promove um novo par a corrente de assinatura e mantem o anterior publicado no JWKS (sobreposicao
// * corrente + anterior) para tolerar o cache do JWKS no Keycloak. Endpoint <b>aberto</b> (sem
// * autenticacao, coerente com a POC, como o {@code GET /demo/chamar-app-b}); nunca expoe material
// * privado.
// */
//@Tag(name = "Credencial", description = "Chaves publicas e credencial da aplicacao (App A)")
//@RestController
//public class CredencialController {
//
//    private final KeyManager keyManager;
//
//    public CredencialController(KeyManager keyManager) {
//        this.keyManager = keyManager;
//    }
//
//    @Operation(
//            summary = "Gera/rotaciona o par de chaves da aplicacao",
//            description = "Sem corpo de requisicao. Gera um novo par, promove-o a chave corrente de "
//                    + "assinatura da client_assertion e mantem a chave anterior publicada no JWKS "
//                    + "(sobreposicao corrente + anterior) para tolerar o cache do JWKS no Keycloak. "
//                    + "Aberto (POC), sem autenticacao. A resposta nunca inclui material privado.")
//    @ApiResponse(responseCode = "200",
//            description = "Identificador (kid) e instante de criacao da nova chave corrente")
//    @PostMapping(value = "/credencial/rotacionar-chave", produces = MediaType.APPLICATION_JSON_VALUE)
//    public RotacaoChaveResponse rotacionarChave() {
//        RotationResult resultado = keyManager.rotate();
//        return new RotacaoChaveResponse(resultado.kid(), resultado.criadaEm());
//    }
//
//    /** Resposta da rotacao: identificador e instante de criacao da nova chave corrente. */
//    public record RotacaoChaveResponse(
//            @Schema(description = "Identificador (kid) da nova chave corrente de assinatura",
//                    example = "0k7m3Q4wR2t...")
//            String kid,
//            @Schema(description = "Instante de criacao da nova chave corrente (ISO-8601)",
//                    example = "2026-07-23T12:34:56Z")
//            Instant criadaEm) {
//    }
//}
