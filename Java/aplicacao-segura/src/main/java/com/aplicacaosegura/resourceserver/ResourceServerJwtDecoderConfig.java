package com.aplicacaosegura.resourceserver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Monta o {@link JwtDecoder} que valida os {@code access_tokens} apresentados a esta aplicação
 * contra o JWKS do Keycloak (descoberto a partir de {@code issuer-uri}), combinando a validação
 * padrão de assinatura/{@code exp}/{@code iss} da autoconfiguração do
 * {@code spring-boot-starter-oauth2-resource-server} com a validação de {@code aud}
 * ({@link AudienceValidator}), não coberta por essa autoconfiguração.
 */
@Configuration
class ResourceServerJwtDecoderConfig {

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${app.security.resource-server.expected-audience}") String expectedAudience) {

        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(expectedAudience);
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidator, audienceValidator));

        return jwtDecoder;
    }
}
