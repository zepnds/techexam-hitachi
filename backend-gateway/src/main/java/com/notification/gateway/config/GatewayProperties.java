package com.notification.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private BackendCore backendCore = new BackendCore();
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class BackendCore {
        private String baseUrl = "http://localhost:8081";
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 5000;
    }

    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private int requestsPerMinute = 5000;
    }
}
