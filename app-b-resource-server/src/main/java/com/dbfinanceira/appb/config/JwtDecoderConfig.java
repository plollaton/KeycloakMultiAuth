package com.dbfinanceira.appb.config;

import com.dbfinanceira.appb.security.LocalFileJWKSource;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Em vez de "spring.security.oauth2.resourceserver.jwt.jwk-set-uri" (que busca as chaves
 * via HTTP no Keycloak), aqui construimos o JwtDecoder manualmente a partir de um JWKS
 * lido de arquivo local (pasta especifica), conforme pedido pelo consultor de seguranca.
 *
 * Definir um bean JwtDecoder desativa a auto-configuracao padrao do Spring Boot para
 * resource server, entao NAO e necessario (nem deve ser usada) a propriedade
 * spring.security.oauth2.resourceserver.jwt.jwk-set-uri neste projeto.
 */
@Configuration
@EnableConfigurationProperties(AppBSecurityProperties.class)
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(AppBSecurityProperties properties) {
        JWKSource<SecurityContext> jwkSource = new LocalFileJWKSource(Path.of(properties.getJwksPath()));

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSource(jwkSource).build();

        OAuth2TokenValidator<Jwt> timestampValidator =
                new JwtTimestampValidator(Duration.ofSeconds(properties.getClockSkewSeconds()));

        OAuth2TokenValidator<Jwt> issuerValidator =
                new JwtIssuerValidator(properties.getIssuer());

        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains(properties.getAudience()));

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestampValidator, issuerValidator, audienceValidator));

        return decoder;
    }
}
