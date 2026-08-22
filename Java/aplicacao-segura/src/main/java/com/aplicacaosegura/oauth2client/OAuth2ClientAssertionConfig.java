package com.aplicacaosegura.oauth2client;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.NimbusJwtClientAuthenticationParametersConverter;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Configura a obtenção de um {@code access_token} junto ao Keycloak via {@code client_credentials},
 * autenticando esta aplicação com um {@code client_assertion} JWT (RFC 7523) assinado com a chave
 * RSA ativa publicada pelo domínio "Gestão de Chaves RSA e Publicação JWKS" ({@code RsaKeyPairConfig}).
 */
@Configuration
public class OAuth2ClientAssertionConfig {

    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService,
            RSAKey activeRsaKey) {

        Function<ClientRegistration, JWK> jwkResolver = clientRegistration -> activeRsaKey;
        RestClientClientCredentialsTokenResponseClient tokenResponseClient =
                new RestClientClientCredentialsTokenResponseClient();
        tokenResponseClient.addParametersConverter(
                new NimbusJwtClientAuthenticationParametersConverter<>(jwkResolver));

        OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials(builder -> builder.accessTokenResponseClient(tokenResponseClient))
                .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
    }
}
