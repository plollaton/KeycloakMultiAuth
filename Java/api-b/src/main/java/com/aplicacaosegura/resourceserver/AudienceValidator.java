package com.aplicacaosegura.resourceserver;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Valida que a claim {@code aud} de um {@code access_token} contém o identificador de audiência
 * esperado por esta aplicação — verificação não coberta pela autoconfiguração padrão do
 * {@code spring-boot-starter-oauth2-resource-server}, que valida apenas assinatura, {@code exp} e
 * {@code iss}.
 */
class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_AUDIENCE =
            new OAuth2Error("invalid_token", "O access_token não contém a audiência esperada.", null);

    private final String expectedAudience;

    AudienceValidator(String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (token.getAudience().contains(expectedAudience)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
    }
}
