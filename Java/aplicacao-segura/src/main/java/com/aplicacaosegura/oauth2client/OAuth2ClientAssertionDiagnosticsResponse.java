package com.aplicacaosegura.oauth2client;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Confirmação do resultado da troca client_assertion -> access_token.")
public record OAuth2ClientAssertionDiagnosticsResponse(
        @Schema(description = "Resultado da troca.", example = "ok") String status) {
}
