package com.aplicacaosegura.resourceserver;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evidencia a classificação de acesso (público ou protegido) do endpoint chamado.")
public record AccessClassificationResponse(
        @Schema(description = "Classificação de acesso do endpoint.", example = "protegido") String acesso) {
}
