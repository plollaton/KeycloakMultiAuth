package com.aplicacaosegura.resourceserver;

import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Valida que a claim {@code iat} de um {@code access_token} está presente e não é posterior ao
 * instante atual, além da tolerância de relógio de 60 segundos — a mesma já aplicada por padrão
 * pelo {@code JwtTimestampValidator} da autoconfiguração do
 * {@code spring-boot-starter-oauth2-resource-server} a {@code exp}/{@code nbf} no mesmo
 * {@code jwtDecoder}. Verificação não coberta por essa autoconfiguração.
 */
class IssuedAtValidator implements OAuth2TokenValidator<Jwt> {

    private static final Duration CLOCK_SKEW = Duration.ofSeconds(60);

    private static final OAuth2Error INVALID_ISSUED_AT =
            new OAuth2Error("invalid_token", "O access_token não contém uma claim iat válida.", null);

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Instant issuedAt = token.getIssuedAt();
        if (issuedAt == null || issuedAt.isAfter(Instant.now().plus(CLOCK_SKEW))) {
            return OAuth2TokenValidatorResult.failure(INVALID_ISSUED_AT);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
