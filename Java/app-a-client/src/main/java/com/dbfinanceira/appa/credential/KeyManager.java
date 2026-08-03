//package com.dbfinanceira.appa.credential;
//
//import com.nimbusds.jose.JWSAlgorithm;
//import com.nimbusds.jose.jwk.JWK;
//import com.nimbusds.jose.jwk.JWKSet;
//import com.nimbusds.jose.jwk.KeyUse;
//import com.nimbusds.jose.jwk.RSAKey;
//import jakarta.annotation.PostConstruct;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import sun.security.tools.keytool.CertAndKeyGen;
//import sun.security.x509.X500Name;
//
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.security.KeyStore;
//import java.security.PrivateKey;
//import java.security.cert.Certificate;
//import java.security.cert.X509Certificate;
//import java.security.interfaces.RSAPublicKey;
//import java.time.Instant;
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.Enumeration;
//import java.util.List;
//
///**
// * Gestao do par de chaves da aplicacao (dominio Autenticacao Maquina-a-Maquina).
// *
// * <p>No boot, carrega o par de chaves do keystore PKCS12 persistido ou, na ausencia dele, gera o
// * par inicial (RSA-2048/RS256, certificado X.509 autoassinado) e o persiste. O par sobrevive a
// * reinicios (o {@code kid} publicado no JWKS permanece estavel), enquanto o access token continua
// * mantido apenas em memoria pelo fluxo OAuth2 (fora deste componente).
// *
// * <p>Expoe a chave privada corrente para assinar a {@code client_assertion} ({@code private_key_jwt})
// * e o {@code JWKSet} publico (sem material privado) para publicacao no endpoint JWKS.
// *
// * <p>A rotacao ({@link #rotate()}) gera um novo par, promove-o a corrente e mantem a chave anterior
// * publicada no JWKS (sobreposicao corrente + anterior), tolerando a janela de cache do JWKS no
// * Keycloak. Sempre se assina com a corrente; mantem-se no maximo duas chaves.
// *
// * <p>O certificado X.509 autoassinado e apenas o envelope exigido pelo formato PKCS12 para persistir
// * uma chave privada; a validacao pelo Keycloak usa os parametros {@code n}/{@code e} do JWK, e nao a
// * validade do certificado. A geracao do certificado usa APIs internas da JDK
// * ({@code sun.security.*}), habilitadas via {@code --add-exports} no build/execucao, para nao
// * introduzir dependencia nova (decisao registrada em {@code research.md}).
// */
//@Component
//public class KeyManager {
//
//    private static final Logger log = LoggerFactory.getLogger(KeyManager.class);
//
//    private static final String KEYSTORE_TYPE = "PKCS12";
//    private static final String KEY_ALGORITHM = "RSA";
//    private static final int KEY_SIZE = 2048;
//    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
//    private static final String CERT_DN = "CN=app-a-client";
//    // Validade longa: o certificado e so o envelope de persistencia da chave no PKCS12.
//    private static final long CERT_VALIDITY_SECONDS = 100L * 365 * 24 * 60 * 60;
//
//    private final Path keystoreLocation;
//    private final char[] keystorePassword;
//
//    private volatile List<StoredKey> keys;
//
//    public KeyManager(@Value("${app.credential.keystore.location}") String keystoreLocation,
//                      @Value("${app.credential.keystore.password}") String keystorePassword) {
//        this.keystoreLocation = Path.of(keystoreLocation);
//        this.keystorePassword = keystorePassword.toCharArray();
//    }
//
//    @PostConstruct
//    void init() {
//        try {
//            if (Files.exists(keystoreLocation)) {
//                this.keys = load();
//                log.info("Par de chaves carregado do keystore '{}' (kid corrente: {})",
//                        keystoreLocation, currentKey().jwk().getKeyID());
//            } else {
//                StoredKey initial = generate();
//                this.keys = List.of(initial);
//                persist(this.keys);
//                log.info("Keystore ausente em '{}'; par inicial gerado e persistido (kid corrente: {})",
//                        keystoreLocation, initial.jwk().getKeyID());
//            }
//        } catch (Exception e) {
//            throw new IllegalStateException("Falha ao inicializar o par de chaves da aplicacao", e);
//        }
//    }
//
//    /**
//     * Chave de assinatura corrente, com material privado. Uso interno do dominio para assinar a
//     * {@code client_assertion} ({@code private_key_jwt}).
//     */
//    public RSAKey getSigningKey() {
//        return currentKey().jwk();
//    }
//
//    /** Identificador ({@code kid}) da chave corrente de assinatura. */
//    public String getCurrentKid() {
//        return currentKey().jwk().getKeyID();
//    }
//
//    /**
//     * {@code JWKSet} publico (sem qualquer material privado) com a(s) chave(s) publica(s)
//     * corrente(s), para publicacao em {@code GET /.well-known/jwks.json}.
//     */
//    public JWKSet getPublicJwkSet() {
//        List<JWK> publicKeys = new ArrayList<>();
//        for (StoredKey key : keys) {
//            publicKeys.add(key.jwk().toPublicJWK());
//        }
//        return new JWKSet(publicKeys);
//    }
//
//    /**
//     * Rotaciona o par de chaves: gera um novo par (nova corrente de assinatura), mantem a corrente
//     * anterior publicada no JWKS e descarta a "anterior" mais antiga — no maximo duas chaves. A
//     * sobreposicao corrente + anterior tolera a janela em que o Keycloak ainda tem o JWKS anterior
//     * em cache. Persiste o novo estado no keystore e retorna o {@code kid} e o {@code criadaEm} da
//     * nova corrente, sem expor material privado.
//     */
//    public synchronized RotationResult rotate() {
//        try {
//            StoredKey newKey = generate();
//            StoredKey previous = currentKey();
//            List<StoredKey> rotated = List.of(previous, newKey);
//            persist(rotated);
//            this.keys = rotated;
//            log.info("Chave rotacionada: nova corrente kid={}, anterior mantida no JWKS kid={}",
//                    newKey.jwk().getKeyID(), previous.jwk().getKeyID());
//            return new RotationResult(newKey.jwk().getKeyID(), newKey.createdAt());
//        } catch (Exception e) {
//            throw new IllegalStateException("Falha ao rotacionar o par de chaves da aplicacao", e);
//        }
//    }
//
//    private StoredKey currentKey() {
//        // Corrente = entrada com o certificado mais recente (maior notBefore).
//        return keys.stream().max(Comparator.comparing(StoredKey::createdAt)).orElseThrow();
//    }
//
//    private StoredKey generate() throws Exception {
//        CertAndKeyGen generator = new CertAndKeyGen(KEY_ALGORITHM, SIGNATURE_ALGORITHM);
//        generator.generate(KEY_SIZE);
//        PrivateKey privateKey = generator.getPrivateKey();
//        X509Certificate certificate = generator.getSelfCertificate(new X500Name(CERT_DN), CERT_VALIDITY_SECONDS);
//        RSAPublicKey publicKey = (RSAPublicKey) certificate.getPublicKey();
//        RSAKey jwk = toRsaKey(publicKey, privateKey);
//        return new StoredKey(jwk, certificate, certificate.getNotBefore().toInstant());
//    }
//
//    private RSAKey toRsaKey(RSAPublicKey publicKey, PrivateKey privateKey) throws Exception {
//        // kid = thumbprint RFC 7638, derivado da chave publica -> estavel entre reinicios.
//        String kid = new RSAKey.Builder(publicKey).build().computeThumbprint().toString();
//        return new RSAKey.Builder(publicKey)
//                .privateKey(privateKey)
//                .keyUse(KeyUse.SIGNATURE)
//                .algorithm(JWSAlgorithm.RS256)
//                .keyID(kid)
//                .build();
//    }
//
//    private List<StoredKey> load() throws Exception {
//        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
//        try (InputStream in = Files.newInputStream(keystoreLocation)) {
//            keyStore.load(in, keystorePassword);
//        }
//        List<StoredKey> loaded = new ArrayList<>();
//        Enumeration<String> aliases = keyStore.aliases();
//        while (aliases.hasMoreElements()) {
//            String alias = aliases.nextElement();
//            if (!keyStore.isKeyEntry(alias)) {
//                continue;
//            }
//            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, keystorePassword);
//            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
//            RSAPublicKey publicKey = (RSAPublicKey) certificate.getPublicKey();
//            RSAKey jwk = toRsaKey(publicKey, privateKey);
//            loaded.add(new StoredKey(jwk, certificate, certificate.getNotBefore().toInstant()));
//        }
//        if (loaded.isEmpty()) {
//            throw new IllegalStateException("Keystore '" + keystoreLocation + "' nao contem nenhuma chave");
//        }
//        return loaded;
//    }
//
//    private void persist(List<StoredKey> toPersist) throws Exception {
//        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
//        keyStore.load(null, null);
//        for (StoredKey key : toPersist) {
//            keyStore.setKeyEntry(key.jwk().getKeyID(),
//                    key.jwk().toPrivateKey(),
//                    keystorePassword,
//                    new Certificate[]{key.certificate()});
//        }
//        Path parent = keystoreLocation.toAbsolutePath().getParent();
//        if (parent != null) {
//            Files.createDirectories(parent);
//        }
//        try (OutputStream out = Files.newOutputStream(keystoreLocation)) {
//            keyStore.store(out, keystorePassword);
//        }
//    }
//
//    /** Entrada de chave persistida: JWK (com privada), certificado X.509 e instante de criacao. */
//    private record StoredKey(RSAKey jwk, X509Certificate certificate, Instant createdAt) {
//    }
//
//    /** Resultado de uma rotacao: identificador ({@code kid}) e instante de criacao da nova corrente. */
//    public record RotationResult(String kid, Instant criadaEm) {
//    }
//}
