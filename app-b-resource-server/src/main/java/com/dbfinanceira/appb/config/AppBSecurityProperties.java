package com.dbfinanceira.appb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public class AppBSecurityProperties {

    /** Issuer esperado (claim "iss"), deve bater com o realm do Keycloak. */
    private String issuer;

    /** Audience esperada (claim "aud"); exige Audience Mapper no Keycloak. */
    private String audience;

    /** Caminho local (pasta especifica) onde o JWKS publico deve estar disponivel. */
    private String jwksPath;

    /** Tolerancia de relogio (clock skew) na validacao de exp/nbf/iat. */
    private long clockSkewSeconds = 60;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getJwksPath() {
        return jwksPath;
    }

    public void setJwksPath(String jwksPath) {
        this.jwksPath = jwksPath;
    }

    public long getClockSkewSeconds() {
        return clockSkewSeconds;
    }

    public void setClockSkewSeconds(long clockSkewSeconds) {
        this.clockSkewSeconds = clockSkewSeconds;
    }
}
