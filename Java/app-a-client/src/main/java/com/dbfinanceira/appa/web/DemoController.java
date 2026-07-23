package com.dbfinanceira.appa.web;

import com.dbfinanceira.appa.client.AppBClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint apenas para demonstracao/teste manual do fluxo:
 * App A obtem token client_credentials no Keycloak e chama a App B com ele.
 */
@RestController
public class DemoController {

    private final AppBClient appBClient;

    public DemoController(AppBClient appBClient) {
        this.appBClient = appBClient;
    }

    @GetMapping("/demo/chamar-app-b")
    public String chamarAppB() {
        return appBClient.chamarEndpointProtegido();
    }
}
