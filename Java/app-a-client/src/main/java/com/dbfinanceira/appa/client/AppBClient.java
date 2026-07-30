//package com.dbfinanceira.appa.client;
//
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestClient;
//
//@Component
//public class AppBClient {
//
//    private final RestClient appBRestClient;
//
//    public AppBClient(RestClient appBRestClient) {
//        this.appBRestClient = appBRestClient;
//    }
//
//    public String chamarEndpointProtegido() {
//        return appBRestClient.get()
//                .uri("/api/protegido")
//                .retrieve()
//                .body(String.class);
//    }
//}
