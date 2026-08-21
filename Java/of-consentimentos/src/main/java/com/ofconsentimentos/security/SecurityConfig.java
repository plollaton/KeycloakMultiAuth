package com.ofconsentimentos.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Cadeia de filtros do resource server: exige token {@code Bearer} descriptografável em toda
 * requisição ao endpoint protegido e, sobre o claims set decifrado, exige ao menos uma permissão
 * do portador antes de liberar o acesso.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JweTokenDecoder jweTokenDecoder) throws Exception {
        AuthorizationManager<RequestAuthorizationContext> comAoMenosUmaPermissao = (authentication, context) ->
                new AuthorizationDecision(!authentication.get().getAuthorities().isEmpty());
        AuthorizationManager<RequestAuthorizationContext> autorizacao =
                AuthorizationManagers.allOf(AuthenticatedAuthorizationManager.authenticated(), comAoMenosUmaPermissao);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/hello").access(autorizacao))
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt
                        .decoder(jweTokenDecoder)
                        .jwtAuthenticationConverter(new PermissoesAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public JweTokenDecoder jweTokenDecoder(PrivateKey jwtDecryptionPrivateKey) {
        return new JweTokenDecoder(jwtDecryptionPrivateKey);
    }

    @Bean
    public PrivateKey jwtDecryptionPrivateKey(
            @Value("${app.security.jwe.private-key-location}") Resource privateKeyResource) throws IOException, GeneralSecurityException {
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
