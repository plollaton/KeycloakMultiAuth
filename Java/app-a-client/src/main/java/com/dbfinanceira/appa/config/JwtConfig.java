package com.dbfinanceira.appa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;

@Configuration
public class JwtConfig {

    @Bean
    public KeyStore keyStore(
            @Value("${jwt.keystore.location}") Resource location,
            @Value("${jwt.keystore.password}") String password) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(location.getInputStream(), password.toCharArray());
        return ks;
    }

    @Bean
    public PrivateKey privateKey(KeyStore keyStore,
                                 @Value("${jwt.key.alias}") String alias,
                                 @Value("${jwt.key.password}") String password) throws Exception {
        return (PrivateKey) keyStore.getKey(alias, password.toCharArray());
    }

    @Bean
    public KeyPair keyPair(KeyStore keyStore,
                           @Value("${jwt.key.alias}") String alias,
                           @Value("${jwt.key.password}") String password) throws Exception {

        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
        PublicKey publicKey = keyStore.getCertificate(alias).getPublicKey();

        return new KeyPair(publicKey, privateKey);
    }
}
