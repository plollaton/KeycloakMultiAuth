package com.aplicacaosegura.jwks;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Conjunto de chaves públicas no formato JWK Set (RFC 7517).")
public record JwkSetDto(List<JwkDto> keys) {
}
