package com.notification.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Scalable Notification Delivery Platform with CEP")
                        .version("1.0.0")
                        .description("High-throughput, resilient asynchronous notification delivery platform with Complex Event Processing (CEP) engine.")
                        .contact(new Contact()
                                .name("Technology Leadership Team")
                                .email("tech-lead@notification-platform.io"))
                        .license(new License().name("Apache 2.0").url("https://spring.io")));
    }
}
