package com.aplicacaosegura.resourceserver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de exemplo que evidenciam a classificação de acesso aplicada pela
 * {@code SecurityFilterChain}: {@code /api/protected} exige um {@code access_token} válido,
 * emitido pelo Keycloak; {@code /api/public} é processado sem exigir autenticação.
 */
@RestController
@Tag(name = "api", description = "Endpoints de exemplo que evidenciam a classificação de acesso público/protegido.")
public class ResourceServerExampleController {

    @GetMapping("/api/protected")
    @Operation(summary = "Endpoint protegido de exemplo",
            description = "Exige um access_token válido, emitido pelo Keycloak, com assinatura, "
                    + "iss, exp e aud válidos para o ambiente.")
    @ApiResponse(responseCode = "200", description = "Acesso concedido com access_token válido.",
            content = @Content(schema = @Schema(implementation = AccessClassificationResponse.class)))
    @ApiResponse(responseCode = "401", description = "access_token ausente ou inválido "
            + "(assinatura, iss, exp ou aud).")
    public AccessClassificationResponse protectedEndpoint() {
        return new AccessClassificationResponse("protegido");
    }

    @GetMapping("/api/public")
    @Operation(summary = "Endpoint público de exemplo",
            description = "Processado sem exigir access_token.")
    @ApiResponse(responseCode = "200", description = "Acesso concedido sem exigência de autenticação.",
            content = @Content(schema = @Schema(implementation = AccessClassificationResponse.class)))
    public AccessClassificationResponse publicEndpoint() {
        return new AccessClassificationResponse("publico");
    }
}
