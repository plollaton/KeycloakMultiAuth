package com.aplicacaosegura.jwks;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/**
 * Mantém o par de chaves RSA ativo da aplicação, carregado na inicialização do par fixo de
 * arquivos em {@code src/main/resources}: {@code api-a-private.pem} (chave privada, PKCS8) e
 * {@code api-a-cert.pem} (certificado X.509 com a chave pública correspondente). O {@code kid}
 * publicado é o número de série do certificado.
 */
@Configuration
public class RsaKeyPairConfig {

    private static final String PRIVATE_KEY_RESOURCE = "api-a-private.pem";
    private static final String CERTIFICATE_RESOURCE = "api-a-cert.pem";

    @Bean
    RSAKey activeRsaKey() throws Exception {
        RSAPrivateKey privateKey = readPrivateKey();
        X509Certificate certificate = readCertificate();
        return new RSAKey.Builder((RSAPublicKey) certificate.getPublicKey())
                .privateKey(privateKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID(certificate.getSerialNumber().toString(16))
                .build();
    }

    private RSAPrivateKey readPrivateKey() throws Exception {
        String base64 = readResourceAsString(PRIVATE_KEY_RESOURCE)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(base64);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private X509Certificate readCertificate() throws Exception {
        try (InputStream inputStream = new ClassPathResource(CERTIFICATE_RESOURCE).getInputStream()) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(inputStream);
        }
    }

    private String readResourceAsString(String name) throws Exception {
        try (InputStream inputStream = new ClassPathResource(name).getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }
}
