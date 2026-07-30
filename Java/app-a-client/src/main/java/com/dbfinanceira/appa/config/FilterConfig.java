package com.dbfinanceira.appa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class FilterConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> {
                    authorizationManagerRequestMatcherRegistry.requestMatchers("/.well-known/jwks.json").permitAll()  // ← Público
                            .requestMatchers("/login", "/signup").permitAll()        // ← Seus públicos
                            .anyRequest().authenticated();
                }) ;
        return http.build();
    }
}
