package com.dbfinanceira.appb.web;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class SecureController {

    @GetMapping("/api/protegido")
    public Map<String, Object> protegido(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mensagem", "Acesso autorizado - JWT validado offline via JWKS local");
        body.put("subject", jwt.getSubject());
        body.put("clientId", jwt.getClaimAsString("azp"));
        body.put("issuer", jwt.getIssuer() != null ? jwt.getIssuer().toString() : null);
        body.put("audience", jwt.getAudience());
        body.put("scope", jwt.getClaimAsString("scope"));
        body.put("expiresAt", jwt.getExpiresAt());
        return body;
    }
}
