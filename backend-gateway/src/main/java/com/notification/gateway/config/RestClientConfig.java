package com.notification.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient backendCoreRestClient(GatewayProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getBackendCore().getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getBackendCore().getReadTimeoutMs()));

        return RestClient.builder()
                .baseUrl(properties.getBackendCore().getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
