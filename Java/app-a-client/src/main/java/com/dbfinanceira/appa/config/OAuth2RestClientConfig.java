package com.dbfinanceira.appa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * Fluxo client_credentials "puro" (machine-to-machine): nao existe usuario/sessao HTTP
 * de entrada, entao usamos AuthorizedClientServiceOAuth2AuthorizedClientManager (nao o
 * gerenciador baseado em HttpServletRequest/Response usado em login OAuth2 de usuario).
 *
 * O OAuth2ClientHttpRequestInterceptor busca/renova o access token automaticamente antes
 * de cada chamada e anexa "Authorization: Bearer <jwt>" na requisicao para a App B.
 */
@Configuration
public class OAuth2RestClientConfig {

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(authorizedClientProvider);
        return manager;
    }

    @Bean
    public RestClient appBRestClient(OAuth2AuthorizedClientManager authorizedClientManager,
                                      @Value("${app.app-b.base-url}") String appBBaseUrl,
                                      @Value("${app.app-b.registration-id}") String registrationId) {

        OAuth2ClientHttpRequestInterceptor interceptor =
                new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        // sempre usa o registration "keycloak-client-credentials", nao depende de request attribute
        interceptor.setClientRegistrationIdResolver(request -> registrationId);

        return RestClient.builder()
                .baseUrl(appBBaseUrl)
                .requestInterceptor(interceptor)
                .build();
    }
}
