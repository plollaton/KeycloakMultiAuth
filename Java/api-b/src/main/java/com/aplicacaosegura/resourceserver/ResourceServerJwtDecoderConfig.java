package com.aplicacaosegura.resourceserver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Monta o {@link JwtDecoder} que valida os {@code access_tokens} apresentados a esta aplicação
 * contra o JWKS do Keycloak (descoberto a partir de {@code issuer-uri}), combinando a validação
 * padrão de assinatura/{@code iss} da autoconfiguração do
 * {@code spring-boot-starter-oauth2-resource-server} com a validação de {@code exp} exigindo a
 * presença dessa claim (a autoconfiguração padrão, a partir do Spring Security 7.0, aceita um
 * {@code access_token} sem {@code exp}), a validação de {@code aud} ({@link AudienceValidator}) e
 * a de {@code iat} ({@link IssuedAtValidator}), não cobertas por essa autoconfiguração. A
 * expiração do {@code access_token} é controlada inteiramente pelo valor de {@code exp} definido
 * pelo Keycloak: esta aplicação apenas confirma que a claim está presente e que o instante atual
 * não é posterior a ela, sem impor uma janela de vida própria.
 */
@Configuration
class ResourceServerJwtDecoderConfig {

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${app.security.resource-server.expected-audience}") String expectedAudience) {

        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);

        JwtTimestampValidator timestampValidator = new JwtTimestampValidator();
        timestampValidator.setAllowEmptyExpiryClaim(false);
        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithValidators(
                new JwtIssuerValidator(issuerUri), timestampValidator);
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(expectedAudience);
        OAuth2TokenValidator<Jwt> issuedAtValidator = new IssuedAtValidator();
        jwtDecoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(defaultValidator, audienceValidator, issuedAtValidator));

        return jwtDecoder;
    }
}
