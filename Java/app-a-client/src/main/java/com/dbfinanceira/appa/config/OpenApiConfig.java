package com.dbfinanceira.appa.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados globais do contrato OpenAPI gerado pelo springdoc. O contrato em si e derivado dos
 * {@code @RestController} e das anotacoes OpenAPI no codigo (fonte unica), servido em
 * {@code /v3/api-docs} e {@code /swagger-ui.html}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI appAOpenApi() {
        return new OpenAPI().info(new Info()
                .title("app-a-client")
                .version("1.0.0")
                .description("Cliente OAuth2 client_credentials (autenticacao por Signed JWT) que "
                        + "autentica no Keycloak e chama a App B"));
    }
}
