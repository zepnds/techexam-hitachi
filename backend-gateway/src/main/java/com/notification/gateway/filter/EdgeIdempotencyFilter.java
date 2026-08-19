package com.notification.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.common.dto.NotificationResponse;
import com.notification.gateway.idempotency.IdempotencyStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EdgeIdempotencyFilter implements WebFilter {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final String ALT_IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    private final IdempotencyStore idempotencyStore;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!HttpMethod.POST.equals(exchange.getRequest().getMethod()) ||
                !exchange.getRequest().getPath().value().startsWith("/notifications")) {
            return chain.filter(exchange);
        }

        String idempotencyKey = resolveIdempotencyKey(exchange);
        if (idempotencyKey == null) {
            return chain.filter(exchange);
        }

        Optional<NotificationResponse> cachedResponse = idempotencyStore.get(idempotencyKey);
        if (cachedResponse.isPresent()) {
            log.info("[IDEMPOTENCY-HIT] Returning cached response for key: {}", idempotencyKey);
            exchange.getResponse().setStatusCode(HttpStatus.ACCEPTED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            exchange.getResponse().getHeaders().set("X-Cache-Hit", "true");
            exchange.getResponse().getHeaders().set(IDEMPOTENCY_KEY_HEADER, idempotencyKey);

            try {
                byte[] bytes = objectMapper.writeValueAsBytes(cachedResponse.get());
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                return exchange.getResponse().writeWith(Mono.just(buffer));
            } catch (Exception e) {
                log.error("Failed to serialize cached idempotency response: {}", e.getMessage());
            }
        }

        log.debug("[IDEMPOTENCY-MISS] Processing new request with key: {}", idempotencyKey);
        exchange.getResponse().getHeaders().set(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
        return chain.filter(exchange);
    }

    private String resolveIdempotencyKey(ServerWebExchange exchange) {
        String key = exchange.getRequest().getHeaders().getFirst(IDEMPOTENCY_KEY_HEADER);
        if (key == null || key.isBlank()) {
            key = exchange.getRequest().getHeaders().getFirst(ALT_IDEMPOTENCY_KEY_HEADER);
        }
        return (key != null && !key.isBlank()) ? key.trim() : null;
    }
}
