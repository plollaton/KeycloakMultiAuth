package com.aplicacaosegura.resourceserver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Endpoint público que aciona o acesso cruzado desta aplicação a {@code GET /api/protected} da
 * aplicação externa configurada em {@code app.cross.external-base-url}, apresentando um
 * {@code access_token} obtido junto ao Keycloak pelo {@link OAuth2AuthorizedClientManager} já
 * usado pelo domínio "Cliente OAuth2 com Client Assertion JWT". Nunca devolve o {@code access_token}
 * nem qualquer material da chave privada no corpo da resposta.
 */
@RestController
@Tag(name = "api", description = "Endpoints de exemplo que evidenciam a classificação de acesso público/protegido.")
public class CrossAccessController {

    private static final String REGISTRATION_ID = "keycloak";
    private static final String PRINCIPAL_NAME = "cross";

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final RestClient crossAccessRestClient;

    public CrossAccessController(OAuth2AuthorizedClientManager authorizedClientManager,
            RestClient crossAccessRestClient) {
        this.authorizedClientManager = authorizedClientManager;
        this.crossAccessRestClient = crossAccessRestClient;
    }

    @GetMapping("/api/cross")
    @Operation(summary = "Acesso cruzado a GET /api/protected de uma aplicação externa",
            description = "Processado sem exigir access_token do chamador. Obtém um access_token "
                    + "junto ao Keycloak e chama GET /api/protected da aplicação externa configurada "
                    + "em app.cross.external-base-url, apresentando esse token.")
    @ApiResponse(responseCode = "200", description = "Acesso cruzado aceito pela aplicação externa.",
            content = @Content(schema = @Schema(implementation = CrossAccessResponse.class)))
    @ApiResponse(responseCode = "502", description = "Falha na obtenção do access_token junto ao "
            + "Keycloak, ou falha na chamada a GET /api/protected da aplicação externa (token "
            + "rejeitado, aplicação externa inacessível, ou qualquer outro erro na chamada).")
    public ResponseEntity<CrossAccessResponse> cross() {
        try {
            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(
                    OAuth2AuthorizeRequest.withClientRegistrationId(REGISTRATION_ID)
                            .principal(PRINCIPAL_NAME)
                            .build());
            if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }

            OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
            ResponseEntity response = crossAccessRestClient.get()
                    .uri("/api/protected")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.getTokenValue())
                    .retrieve()
                    .toBodilessEntity();
            if (response.getStatusCode().isError()){
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(new CrossAccessResponse("ok"));
        } catch (OAuth2AuthorizationException | RestClientException ex) {
            ex.printStackTrace();
            return  ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
