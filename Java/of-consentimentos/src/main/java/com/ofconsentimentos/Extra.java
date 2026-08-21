package com.ofconsentimentos;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import org.springframework.security.oauth2.jwt.Jwt;

import com.ofconsentimentos.security.JweTokenDecoder;

public class Extra {

    private static final String ENV_VAR_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIybFVTSEVzbWRaeTZESTlCVDJBaUFzY2VNb2VIeVg4azVYY0FPS3FySUJFIn0.eyJleHAiOjE3ODcyMzUwMTUsImlhdCI6MTc4NzIzNDk1NSwianRpIjoidHJydGNjOjdiMDNmNTI0LTIyYjUtOThjMy04ZDlkLTNhZTdmZjYyNzI2ZCIsImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9yZWFsbXMvbWFzdGVyIiwiYXVkIjpbIm9mLWNvbnNlbnRpbWVudG9zIiwiYWNjb3VudCJdLCJzdWIiOiI4YjFiMmYzNS0wZTRmLTQzNzEtOWU1Yi04ZTUzYmQ3YmIzMTciLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJvZi1wYWdhbWVudG9zIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIvKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1tYXN0ZXIiLCJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJjbGllbnRIb3N0IjoiMTcyLjIxLjAuMSIsInByZWZlcnJlZF91c2VybmFtZSI6InNlcnZpY2UtYWNjb3VudC1vZi1wYWdhbWVudG9zIiwiY2xpZW50QWRkcmVzcyI6IjE3Mi4yMS4wLjEiLCJjbGllbnRfaWQiOiJvZi1wYWdhbWVudG9zIn0.AD7ZBvTvEgJJxNInNRt_IPpuEbW9JYeIjNzF4K8vsZa-20wD-RuMBsjnVPumvQtq2SfAitBChxSotpQ7bRR35EfdAuy4lmRbNhzEX61S1dZiWnTcNmBiMMhnTtKD6wyLkwpRtNSpGDmJvIKyKecz2LMqxyBsMX-MfgLvOsTV4bAtsW8hN0QDycO_eE-cStNfhTgV8rSLO2Zapgig2wZuqPZk-hY482EGceyOL-jdDCODfcrjxL1rFz-Nve7f_pz3QVdgCaLvgBswUS16vnZ59bAgKzxSAoTufI05FeUoD5ZM0HNNZzGgiU4HNoGh44rsqa6pk9qCu4gNkHQb7Ywthw";
    private static final String PRIVATE_KEY_RESOURCE = "/private-key.pem";

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        String token = ENV_VAR_TOKEN;
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Variável de ambiente " + ENV_VAR_TOKEN + " não definida");
        }

        PrivateKey privateKey = loadPrivateKey();
        JweTokenDecoder decoder = new JweTokenDecoder(privateKey);

        Jwt jwt = decoder.decode(token);
        System.out.println(jwt.getClaims());
    }

    private static PrivateKey loadPrivateKey() throws IOException, GeneralSecurityException {
        String pem;
        try (InputStream in = Extra.class.getResourceAsStream(PRIVATE_KEY_RESOURCE)) {
            if (in == null) {
                throw new IOException("Recurso " + PRIVATE_KEY_RESOURCE + " não encontrado no classpath");
            }
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
