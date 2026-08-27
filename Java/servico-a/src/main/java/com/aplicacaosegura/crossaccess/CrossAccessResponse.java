package com.aplicacaosegura.crossaccess;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Confirmação do resultado do acesso cruzado a GET /api/protected da aplicação externa.")
public record CrossAccessResponse(
        @Schema(description = "Resultado do acesso cruzado.", example = "ok") String status) {
}
