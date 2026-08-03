package com.dbfinanceira.appa.config;

import org.hibernate.validator.cfg.defs.RangeDef;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class FilterConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> {
                    authorizationManagerRequestMatcherRegistry
                            .requestMatchers("/.well-known/jwks.json").permitAll()  // ← Público
                            .requestMatchers("/login", "/signup").permitAll()        // ← Seus públicos
                            .anyRequest()
                            .authenticated();
                })
                .oauth2ResourceServer(oauth ->
                        oauth.jwt(jwt ->
                                jwt.jwkSetUri("http://localhost:8080/realms/master/protocol/openid-connect/certs")
                        )
                );
        return http.build();
    }
}
