package com.dbfinanceira.appa.web;

//import com.dbfinanceira.appa.client.AppBClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint apenas para demonstracao/teste manual do fluxo:
 * App A obtem token client_credentials no Keycloak e chama a App B com ele.
 */
@Tag(name = "Demonstracao", description = "Gatilho manual do fluxo ponta a ponta App A -> App B")
@RestController
public class DemoController {

//    private final AppBClient appBClient;

//    public DemoController(AppBClient appBClient) {
//        this.appBClient = appBClient;
//    }

    @Operation(
            summary = "Dispara o fluxo ponta a ponta: obtem o token no Keycloak e chama a App B",
            description = "A App A obtem/renova o access token via client_credentials no Keycloak e "
                    + "chama GET /api/protegido da App B com o bearer anexado, repassando a resposta.")
    @ApiResponse(responseCode = "200", description = "Corpo repassado verbatim pela App B")
    @GetMapping("/demo/chamar-app-b")
    public String chamarAppB(@AuthenticationPrincipal Jwt jwt) {
        System.out.println("✅ Token válido!");
        System.out.println("Client ID: " + jwt.getClaimAsString("client_id"));
        System.out.println("Scopes: " + jwt.getClaimAsStringList("scope"));
        System.out.println("Roles: " + jwt.getClaimAsStringList("roles"));
        System.out.println("Chamada funcionou...");
        return "OK"; //appBClient.chamarEndpointProtegido();
    }
}
