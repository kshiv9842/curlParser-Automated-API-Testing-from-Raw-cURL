package com.apiautomation.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiSecurityProperties {

    /** Max execute requests per client IP per rolling minute. */
    @Value("${RATE_LIMIT_PER_MINUTE:10}")
    private int rateLimitPerMinute;

    /** Max curl commands accepted in one multi-execute call. */
    @Value("${MAX_CURLS_PER_REQUEST:5}")
    private int maxCurlsPerRequest;

    /** Max characters of curl payload text per request. */
    @Value("${MAX_REQUEST_CHARS:100000}")
    private int maxRequestChars;

    public int getRateLimitPerMinute() {
        return Math.max(1, rateLimitPerMinute);
    }

    public int getMaxCurlsPerRequest() {
        return Math.max(1, maxCurlsPerRequest);
    }

    public int getMaxRequestChars() {
        return Math.max(1000, maxRequestChars);
    }
}
