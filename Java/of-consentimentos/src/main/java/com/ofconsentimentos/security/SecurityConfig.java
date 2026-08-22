package com.ofconsentimentos.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Cadeia de filtros do resource server: exige, em toda requisição ao endpoint protegido, um token
 * {@code Bearer} com assinatura verificável e, sobre o claims set do token verificado, exige ao
 * menos uma permissão do portador antes de liberar o acesso.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        AuthorizationManager<RequestAuthorizationContext> comAoMenosUmaPermissao = (authentication, context) ->
                new AuthorizationDecision(!authentication.get().getAuthorities().isEmpty());
        AuthorizationManager<RequestAuthorizationContext> autorizacao =
                AuthorizationManagers.allOf(AuthenticatedAuthorizationManager.authenticated(), comAoMenosUmaPermissao);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/hello").access(autorizacao))
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt
                        .decoder(jwtDecoder)
                        .jwtAuthenticationConverter(new PermissoesAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAPublicKey jwtVerificationPublicKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(jwtVerificationPublicKey).build();
        decoder.setJwtValidator(JwtValidators.createDefault());
        return decoder;
    }

    @Bean
    public RSAPublicKey jwtVerificationPublicKey(PrivateKey jwtPrivateKey) throws GeneralSecurityException {
        RSAPrivateCrtKey rsaPrivateKey = (RSAPrivateCrtKey) jwtPrivateKey;
        RSAPublicKeySpec publicKeySpec =
                new RSAPublicKeySpec(rsaPrivateKey.getModulus(), rsaPrivateKey.getPublicExponent());
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);
    }

    @Bean
    public PrivateKey jwtPrivateKey(
            @Value("${app.security.jwt.private-key-location}") Resource privateKeyResource) throws IOException, GeneralSecurityException {
        String pem;
        try (InputStream in = privateKeyResource.getInputStream()) {
            pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }
}
