package com.aplicacaosegura;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.aplicacaosegura")
public class AplicacaoSeguraApplication {

    public static void main(String[] args) {
        SpringApplication.run(AplicacaoSeguraApplication.class, args);
    }
}
