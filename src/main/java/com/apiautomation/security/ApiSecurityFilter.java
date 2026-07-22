package com.apiautomation.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Rate limit for execute endpoints (per client IP).
 */
@Component
public class ApiSecurityFilter extends OncePerRequestFilter {

    private final ApiSecurityProperties props;
    private final RateLimiter rateLimiter;

    public ApiSecurityFilter(ApiSecurityProperties props, RateLimiter rateLimiter) {
        this.props = props;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        return !(path.startsWith("/api/execute")
                || path.startsWith("/api/test-multi-curl"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String bucketKey = "ip:" + clientIp(request);
        if (!rateLimiter.tryAcquire(bucketKey, props.getRateLimitPerMinute())) {
            long retry = rateLimiter.retryAfterSeconds(bucketKey);
            response.setHeader("Retry-After", String.valueOf(retry));
            writeError(response, 429,
                    "Rate limit exceeded — max " + props.getRateLimitPerMinute()
                            + " execute requests per minute. Retry after " + retry + "s.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String clientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String json = "{\"error\":\"" + message.replace("\"", "'") + "\"}";
        response.getWriter().write(json);
    }
}
