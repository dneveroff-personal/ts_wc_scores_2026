package com.tswcscores.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    @Value("${football-data.api.url}")
    private String footballApiUrl;

    @Value("${football-data.api.token}")
    private String footballApiToken;

    @Bean
    public WebClient footballDataClient() {
        return WebClient.builder()
                .baseUrl(footballApiUrl)
                .defaultHeader("X-Auth-Token", footballApiToken)
                .build();
    }
}
