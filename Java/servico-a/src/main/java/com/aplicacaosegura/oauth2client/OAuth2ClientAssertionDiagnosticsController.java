package com.aplicacaosegura.oauth2client;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint exclusivamente diagnóstico que aciona manualmente a troca de {@code client_assertion}
 * por {@code access_token} junto ao Keycloak, para validar o fluxo sem depender de um consumidor
 * real do {@code access_token}. Não é uma funcionalidade de negócio e nunca devolve o
 * {@code access_token} nem qualquer material da chave privada no corpo da resposta.
 */
@RestController
@Tag(name = "diagnostics", description = "Endpoints exclusivamente diagnósticos, sem função de negócio.")
public class OAuth2ClientAssertionDiagnosticsController {

    private static final String REGISTRATION_ID = "keycloak";
    private static final String PRINCIPAL_NAME = "diagnostics";

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public OAuth2ClientAssertionDiagnosticsController(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    @GetMapping("/diagnostics/oauth2-client-assertion")
    @Operation(summary = "Aciona manualmente a troca client_assertion -> access_token",
            description = "Monta e assina o client_assertion com a chave RSA ativa, executa a troca "
                    + "client_credentials com o Keycloak e confirma sucesso ou falha, sem devolver o "
                    + "access_token nem qualquer material da chave privada.")
    @ApiResponse(responseCode = "200", description = "Troca concluída com sucesso.",
            content = @Content(schema = @Schema(implementation = OAuth2ClientAssertionDiagnosticsResponse.class)))
    @ApiResponse(responseCode = "502", description = "O Keycloak rejeitou a troca.")
    public ResponseEntity<OAuth2ClientAssertionDiagnosticsResponse> exchange() {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(REGISTRATION_ID)
                .principal(PRINCIPAL_NAME)
                .build();
        try {
            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
            if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }
            return ResponseEntity.ok(new OAuth2ClientAssertionDiagnosticsResponse("ok"));
        } catch (OAuth2AuthorizationException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
