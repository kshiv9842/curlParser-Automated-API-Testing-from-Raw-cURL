package com.apiautomation.testcase;

import com.apiautomation.ParsedCurl;

/**
 * Facts derived from a parsed curl — used to skip non-applicable bug cases.
 */
public final class RequestFacts {
    private final boolean hasAuth;
    private final boolean hasBody;
    private final boolean isJson;
    private final boolean hasQuery;
    private final boolean hasPathId;
    private final boolean hasHeaders;
    private final String method;

    private RequestFacts(boolean hasAuth, boolean hasBody, boolean isJson,
                         boolean hasQuery, boolean hasPathId, boolean hasHeaders, String method) {
        this.hasAuth = hasAuth;
        this.hasBody = hasBody;
        this.isJson = isJson;
        this.hasQuery = hasQuery;
        this.hasPathId = hasPathId;
        this.hasHeaders = hasHeaders;
        this.method = method;
    }

    public static RequestFacts from(ParsedCurl curl) {
        boolean hasAuth = false;
        boolean hasHeaders = curl.getHeaders() != null && !curl.getHeaders().isEmpty();
        if (curl.getHeaders() != null) {
            for (String key : curl.getHeaders().keySet()) {
                if (key != null && (key.equalsIgnoreCase("Authorization")
                        || key.equalsIgnoreCase("Proxy-Authorization"))) {
                    Object value = curl.getHeaders().get(key);
                    if (value != null && !value.toString().isBlank()) {
                        hasAuth = true;
                        break;
                    }
                }
            }
        }
        if (!hasAuth && curl.getAuthorization() != null && !curl.getAuthorization().isBlank()) {
            hasAuth = true;
        }

        String body = curl.getBody();
        boolean hasBody = body != null && !body.isBlank();
        boolean isJson = false;
        if (hasBody) {
            String trimmed = body.trim();
            isJson = trimmed.startsWith("{") || trimmed.startsWith("[");
        }
        if (!isJson && curl.getHeaders() != null) {
            for (String key : curl.getHeaders().keySet()) {
                if (key != null && key.equalsIgnoreCase("Content-Type")) {
                    Object ct = curl.getHeaders().get(key);
                    if (ct != null && ct.toString().toLowerCase().contains("json")) {
                        isJson = hasBody;
                    }
                }
            }
        }

        boolean hasQuery = curl.getQueryParams() != null && !curl.getQueryParams().isEmpty();
        boolean hasPathId = curl.getPathParams() != null && !curl.getPathParams().isEmpty();
        String method = curl.getMethod() == null ? "GET" : curl.getMethod().toUpperCase();

        return new RequestFacts(hasAuth, hasBody, isJson, hasQuery, hasPathId, hasHeaders, method);
    }

    public boolean hasAuth() {
        return hasAuth;
    }

    public boolean hasBody() {
        return hasBody;
    }

    public boolean isJson() {
        return isJson;
    }

    public boolean hasQuery() {
        return hasQuery;
    }

    public boolean hasPathId() {
        return hasPathId;
    }

    public boolean hasHeaders() {
        return hasHeaders;
    }

    public String getMethod() {
        return method;
    }

    public boolean isBodyMethod() {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }
}
