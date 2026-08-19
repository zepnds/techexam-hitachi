package com.notification.gateway.filter;

import com.notification.gateway.config.GatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class EdgeRateLimitFilter implements WebFilter {

    private final GatewayProperties properties;
    private final ConcurrentHashMap<String, AtomicInteger> clientRequestCounts = new ConcurrentHashMap<>();
    private volatile long currentMinuteWindow = System.currentTimeMillis() / 60000;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!properties.getRateLimit().isEnabled()) {
            return chain.filter(exchange);
        }

        long nowMinute = System.currentTimeMillis() / 60000;
        if (nowMinute > currentMinuteWindow) {
            currentMinuteWindow = nowMinute;
            clientRequestCounts.clear();
        }

        String clientIp = resolveClientIp(exchange);
        AtomicInteger counter = clientRequestCounts.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
        int requests = counter.incrementAndGet();

        if (requests > properties.getRateLimit().getRequestsPerMinute()) {
            log.warn("Edge rate limit exceeded for client IP: {} ({} req/min)", clientIp, requests);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            byte[] bytes = "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Gateway rate limit exceeded\"}".getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        return chain.filter(exchange);
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return exchange.getRequest().getRemoteAddress() != null ?
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "127.0.0.1";
    }
}
