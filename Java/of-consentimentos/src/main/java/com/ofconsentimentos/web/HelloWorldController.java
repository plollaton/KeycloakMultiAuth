package com.ofconsentimentos.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Único recurso de negócio deste domínio: existe apenas para demonstrar o fluxo de
 * autenticação/autorização, sem corpo de negócio próprio.
 */
@RestController
public class HelloWorldController {

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("message", "hello world");
    }
}
