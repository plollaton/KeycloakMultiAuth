package com.ofconsentimentos.security;

import java.security.PrivateKey;
import java.text.ParseException;
import java.util.Date;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;

import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidationException;

/**
 * {@link JwtDecoder} que interpreta o token Bearer como um envelope JWE: descriptografa-o com a
 * chave privada local e usa o payload decifrado diretamente como claims set do token, sem aplicar
 * nenhuma verificação de assinatura adicional.
 */
public class JweTokenDecoder implements JwtDecoder {

    private final PrivateKey privateKey;
    private final JwtTimestampValidator timestampValidator = new JwtTimestampValidator();

    public JweTokenDecoder(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public Jwt decode(String token) {
        EncryptedJWT encryptedJwt = parse(token);
        JWTClaimsSet claimsSet = decrypt(encryptedJwt);
        Jwt jwt = toJwt(token, encryptedJwt.getHeader(), claimsSet);
        validateStructure(jwt);
        return jwt;
    }

    private EncryptedJWT parse(String token) {
        try {
            return EncryptedJWT.parse(token);
        } catch (ParseException ex) {
            throw new BadJwtException("Token corrompido ou malformado", ex);
        }
    }

    private JWTClaimsSet decrypt(EncryptedJWT encryptedJwt) {
        try {
            encryptedJwt.decrypt(new RSADecrypter(this.privateKey));
            return encryptedJwt.getJWTClaimsSet();
        } catch (JOSEException | ParseException ex) {
            throw new BadJwtException("Token não pôde ser decifrado com a chave privada local", ex);
        }
    }

    private Jwt toJwt(String token, JWEHeader header, JWTClaimsSet claimsSet) {
        Jwt.Builder builder = Jwt.withTokenValue(token)
                .headers(headers -> headers.putAll(header.toJSONObject()))
                .claims(claims -> claims.putAll(claimsSet.getClaims()));
        Date issuedAt = claimsSet.getIssueTime();
        if (issuedAt != null) {
            builder.issuedAt(issuedAt.toInstant());
        }
        Date expiresAt = claimsSet.getExpirationTime();
        if (expiresAt != null) {
            builder.expiresAt(expiresAt.toInstant());
        }
        return builder.build();
    }

    private void validateStructure(Jwt jwt) {
        OAuth2TokenValidatorResult result = this.timestampValidator.validate(jwt);
        if (result.hasErrors()) {
            throw new JwtValidationException("Token decifrado, porém estruturalmente inválido", result.getErrors());
        }
    }
}
