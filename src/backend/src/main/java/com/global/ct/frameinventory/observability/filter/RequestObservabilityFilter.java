package com.global.ct.frameinventory.observability.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestObservabilityFilter extends OncePerRequestFilter {

    static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestObservabilityFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = requestId(request.getHeader(REQUEST_ID_HEADER));
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        long startedAt = System.nanoTime();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
            logRequest(request, response, durationMillis);
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    private String requestId(String suppliedRequestId) {
        if (suppliedRequestId != null && SAFE_REQUEST_ID.matcher(suppliedRequestId).matches()) {
            return suppliedRequestId;
        }
        return UUID.randomUUID().toString();
    }

    private void logRequest(HttpServletRequest request, HttpServletResponse response, long durationMillis) {
        if (request.getRequestURI().startsWith("/actuator/health")) {
            LOGGER.debug("HTTP request method={} path={} status={} durationMs={}",
                request.getMethod(), request.getRequestURI(), response.getStatus(), durationMillis);
            return;
        }
        LOGGER.info("HTTP request method={} path={} status={} durationMs={}",
            request.getMethod(), request.getRequestURI(), response.getStatus(), durationMillis);
    }
}
