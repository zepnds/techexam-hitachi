package com.notification.gateway.filter;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class EdgeCorrelationFilter implements WebFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "traceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = "gw-" + UUID.randomUUID().toString().substring(0, 8);
        }

        final String traceId = correlationId;
        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, traceId);

        return chain.filter(exchange)
                .doOnSubscribe(sub -> MDC.put(MDC_KEY, traceId))
                .doFinally(signal -> MDC.remove(MDC_KEY));
    }
}
