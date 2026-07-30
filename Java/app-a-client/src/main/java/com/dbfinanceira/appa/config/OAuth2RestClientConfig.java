//package com.dbfinanceira.appa.config;
//
//import com.dbfinanceira.appa.credential.KeyManager;
//import com.nimbusds.jose.jwk.JWK;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
//import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
//import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
//import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
//import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
//import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
//import org.springframework.security.oauth2.client.endpoint.NimbusJwtClientAuthenticationParametersConverter;
//import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
//import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
//import org.springframework.security.oauth2.client.registration.ClientRegistration;
//import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
//import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
//import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
//import org.springframework.web.client.RestClient;
//
//import java.util.function.Function;
//
///**
// * Fluxo client_credentials "puro" (machine-to-machine): nao existe usuario/sessao HTTP
// * de entrada, entao usamos AuthorizedClientServiceOAuth2AuthorizedClientManager (nao o
// * gerenciador baseado em HttpServletRequest/Response usado em login OAuth2 de usuario).
// *
// * O cliente autentica-se no Keycloak por Signed JWT (private_key_jwt): o token response client
// * do provider client_credentials recebe um NimbusJwtClientAuthenticationParametersConverter que,
// * a cada obtencao de token, monta a client_assertion assinada com a chave privada corrente do
// * KeyManager (dominio Autenticacao Maquina-a-Maquina). Nao ha client-secret. O grant
// * client_credentials e o escopo app-b.invoke sao preservados (definidos no application.yml).
// *
// * O OAuth2ClientHttpRequestInterceptor busca/renova o access token automaticamente antes
// * de cada chamada e anexa "Authorization: Bearer <jwt>" na requisicao para a App B.
// */
//@Configuration
//public class OAuth2RestClientConfig {
//
//    @Bean
//    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
//        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
//    }
//
//    @Bean
//    public OAuth2AuthorizedClientManager authorizedClientManager(
//            ClientRegistrationRepository clientRegistrationRepository,
//            OAuth2AuthorizedClientService authorizedClientService,
//            KeyManager keyManager) {
//
//        // Resolve, por registration, o JWK de assinatura corrente para a client_assertion. So
//        // responde a registrations que autenticam por private_key_jwt; caso contrario, null (o
//        // converter nao adiciona os parametros de asserção). A chave e lida a cada obtencao de
//        // token, entao uma rotacao passa a valer na proxima renovacao.
//        Function<ClientRegistration, JWK> jwkResolver = clientRegistration -> {
//            if (ClientAuthenticationMethod.PRIVATE_KEY_JWT.equals(clientRegistration.getClientAuthenticationMethod())) {
//                return keyManager.getSigningKey();
//            }
//            return null;
//        };
//
//        RestClientClientCredentialsTokenResponseClient tokenResponseClient =
//                new RestClientClientCredentialsTokenResponseClient();
//        tokenResponseClient.addParametersConverter(
//                new NimbusJwtClientAuthenticationParametersConverter<OAuth2ClientCredentialsGrantRequest>(jwkResolver));
//
//        OAuth2AuthorizedClientProvider authorizedClientProvider =
//                OAuth2AuthorizedClientProviderBuilder.builder()
//                        .clientCredentials(clientCredentials ->
//                                clientCredentials.accessTokenResponseClient(tokenResponseClient))
//                        .build();
//
//        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
//                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
//        manager.setAuthorizedClientProvider(authorizedClientProvider);
//        return manager;
//    }
//
//    @Bean
//    public RestClient appBRestClient(OAuth2AuthorizedClientManager authorizedClientManager,
//                                      @Value("${app.app-b.base-url}") String appBBaseUrl,
//                                      @Value("${app.app-b.registration-id}") String registrationId) {
//
//        OAuth2ClientHttpRequestInterceptor interceptor =
//                new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
//        // sempre usa o registration "keycloak-client-credentials", nao depende de request attribute
//        interceptor.setClientRegistrationIdResolver(request -> registrationId);
//
//        return RestClient.builder()
//                .baseUrl(appBBaseUrl)
//                .requestInterceptor(interceptor)
//                .build();
//    }
//}
