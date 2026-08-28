package com.aplicacaosegura.crossaccess;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configura o {@link RestClient} usado na chamada de saída de {@code GET /api/cross} a
 * {@code GET /api/protected} da aplicação externa, com a base URL lida de
 * {@code app.cross.external-base-url} — sem valor padrão em nenhum {@code application.yml},
 * resolvida apenas pela variável informada na linha de comando de inicialização desta aplicação.
 */
@Configuration
public class CrossAccessRestClientConfig {

    @Bean
    RestClient crossAccessRestClient(@Value("${app.cross.external-base-url}") String externalBaseUrl) {
        return RestClient.builder().baseUrl(externalBaseUrl).build();
    }
}
