package com.olarize.platform.internal.web;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RequestIdFilter.class);
    public static final String HEADER = "X-Request-ID";
    public static final String MDC_KEY = "requestId";
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String supplied = request.getHeader(HEADER);
        String requestId = supplied != null && SAFE_ID.matcher(supplied).matches()
                ? supplied : UUID.randomUUID().toString();
        response.setHeader(HEADER, requestId);
        MDC.put(MDC_KEY, requestId);
        long started = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            if (!request.getRequestURI().startsWith("/actuator/health")) {
                long durationMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                LOG.info("http.request method={} status={} durationMs={}",
                    request.getMethod(), response.getStatus(), durationMs);
            }
            MDC.remove(MDC_KEY);
        }
    }
}
