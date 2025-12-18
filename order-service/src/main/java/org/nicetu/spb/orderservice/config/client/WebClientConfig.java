package org.nicetu.spb.orderservice.config.client;

import jakarta.ws.rs.core.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean("userWebClient")
    public WebClient userWebClient(
            WebClient.Builder builder,
            @Value("${services.user.url}") String userServiceUrl
    ) {
        return builder
                .baseUrl(userServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    @Bean("productWebClient")
    public WebClient productWebClient(
            WebClient.Builder builder,
            @Value("${services.product.url}") String productServiceUrl
    ) {
        return builder
                .baseUrl(productServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }
}
