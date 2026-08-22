package com.ofconsentimentos.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Extrai, do claims set do token já validado pelo {@link org.springframework.security.oauth2.jwt.JwtDecoder}
 * do resource server, os papéis de realm ({@code realm_access.roles}), os papéis de
 * client/resource ({@code resource_access.*.roles}) e os escopos ({@code scope}) do portador,
 * combinando-os em uma única lista de autoridades. A presença de qualquer item nessa lista já
 * satisfaz a regra de autorização do domínio.
 */
public class PermissoesAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.addAll(realmRoles(jwt));
        authorities.addAll(clientRoles(jwt));
        authorities.addAll(scopes(jwt));
        return new JwtAuthenticationToken(jwt, authorities);
    }

    @SuppressWarnings("unchecked")
    private List<GrantedAuthority> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return List.of();
        }
        Object roles = realmAccess.get("roles");
        if (!(roles instanceof Collection<?> rolesCollection)) {
            return List.of();
        }
        return toAuthorities(rolesCollection, "ROLE_");
    }

    @SuppressWarnings("unchecked")
    private List<GrantedAuthority> clientRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null) {
            return List.of();
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Object clientAccess : resourceAccess.values()) {
            if (!(clientAccess instanceof Map<?, ?> clientAccessMap)) {
                continue;
            }
            Object roles = clientAccessMap.get("roles");
            if (roles instanceof Collection<?> rolesCollection) {
                authorities.addAll(toAuthorities(rolesCollection, "ROLE_"));
            }
        }
        return authorities;
    }

    private List<GrantedAuthority> scopes(Jwt jwt) {
        String scope = jwt.getClaimAsString("scope");
        if (scope == null || scope.isBlank()) {
            return List.of();
        }
        return toAuthorities(List.of(scope.split("\\s+")), "SCOPE_");
    }

    private List<GrantedAuthority> toAuthorities(Collection<?> values, String prefix) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Object value : values) {
            authorities.add(new SimpleGrantedAuthority(prefix + value));
        }
        return authorities;
    }
}
