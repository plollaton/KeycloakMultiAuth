package com.aplicacaosegura.jwks;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Chave pública ativa no formato JWK (JSON Web Key).")
public record JwkDto(
        @Schema(description = "Tipo da chave.", example = "RSA") String kty,
        @Schema(description = "Identificador único do par de chaves ativo.", example = "3f2a1e4b-6c3d-4e8a-9c2f-1a2b3c4d5e6f") String kid,
        @Schema(description = "Uso da chave.", example = "sig") String use,
        @Schema(description = "Algoritmo de assinatura.", example = "RS256") String alg,
        @Schema(description = "Módulo RSA, codificado em Base64URL.") String n,
        @Schema(description = "Expoente público RSA, codificado em Base64URL.", example = "AQAB") String e) {
}
