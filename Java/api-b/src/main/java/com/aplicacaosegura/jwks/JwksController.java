package com.aplicacaosegura.jwks;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publica a chave pública ativa da aplicação em formato JWK Set, sem autenticação, para que o
 * Keycloak valide assinaturas produzidas com a chave privada correspondente.
 */
@RestController
@Tag(name = "jwks", description = "Publicação da chave pública ativa da aplicação.")
public class JwksController {

    private final RSAKey activeRsaKey;

    public JwksController(RSAKey activeRsaKey) {
        this.activeRsaKey = activeRsaKey;
    }

    @GetMapping("/oauth2/jwks")
    @Operation(summary = "Publica a chave pública ativa em formato JWK Set",
            description = "Endpoint público, sem autenticação, consultado pelo Keycloak para "
                    + "validar assinaturas produzidas com a chave privada desta aplicação.")
    @ApiResponse(responseCode = "200", description = "Chave pública ativa publicada com sucesso.",
            content = @Content(schema = @Schema(implementation = JwkSetDto.class)))
    public JwkSetDto jwks() {
        JWKSet jwkSet = new JWKSet(activeRsaKey.toPublicJWK());
        List<JwkDto> keys = jwkSet.getKeys().stream()
                .map(RSAKey.class::cast)
                .map(JwksController::toDto)
                .toList();
        return new JwkSetDto(keys);
    }

    private static JwkDto toDto(RSAKey key) {
        return new JwkDto(
                key.getKeyType().getValue(),
                key.getKeyID(),
                key.getKeyUse().getValue(),
                key.getAlgorithm().getName(),
                key.getModulus().toString(),
                key.getPublicExponent().toString());
    }
}
