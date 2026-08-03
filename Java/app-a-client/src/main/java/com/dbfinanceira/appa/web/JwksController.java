package com.dbfinanceira.appa.web;
//
////import com.dbfinanceira.appa.credential.KeyManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
//
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//
/**
 * Publica o JWKS da propria aplicacao (chaves publicas correntes) para o Keycloak validar a
 * {@code client_assertion} ({@code private_key_jwt}) via "Use JWKS URL". Nao expoe material privado.
 */
@Tag(name = "Credencial", description = "Chaves publicas e credencial da aplicacao (App A)")
@RestController
public class JwksController {

    private final KeyPair keyPair;
    private final String alias;

    public JwksController(KeyPair keyPair,
                          @Value("${jwt.key.alias}") String alias) {
        this.keyPair = keyPair;
        this.alias = alias;
    }

    @Operation(
            summary = "Publica o JWKS com as chaves publicas correntes da aplicacao",
            description = "Documento JWKS (RFC 7517) com a(s) chave(s) publica(s) corrente(s) que o "
                    + "Keycloak busca a cada renovacao (\"Use JWKS URL\") para validar a "
                    + "client_assertion. Nunca inclui material privado.")
    @ApiResponse(responseCode = "200", description = "Documento JWKS com as chaves publicas correntes")
    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
        public Map<String, Object> jwks() {
            System.out.println("Chamou...");
            RSAPublicKey rsaKey = (RSAPublicKey) keyPair.getPublic();

            Map<String, Object> key = new HashMap<>();
            key.put("kty", rsaKey.getAlgorithm());
            key.put("use", "sig");
            key.put("kid", alias);
            key.put("n", Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(rsaKey.getModulus().toByteArray()));
            key.put("e", Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(rsaKey.getPublicExponent().toByteArray()));
            key.put("alg", "RS256");

            return Map.of("keys", List.of(key));
        }
}
