package com.apiautomation;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.http.params.CoreConnectionPNames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lightweight performance smoke checks (not full load testing).
 */
public final class ApiPerfTest {

    public static final long WARN_LATENCY_MS = 1_000L;
    public static final long FAIL_LATENCY_MS = 2_000L;
    public static final long HANG_TIMEOUT_MS = 10_000L;
    public static final long ERROR_PATH_WARN_MS = 500L;
    public static final long LARGE_ABS_CAP_MS = 5_000L;
    public static final int REPEAT_GET_COUNT = 5;

    private ApiPerfTest() {
    }

    public static String runBaselineLatencySla(String curlCommand) {
        StringBuilder logs = new StringBuilder();
        logs.append("=== Baseline Latency SLA ===\n\n");
        try {
            TimedResponse tr = send(curlCommand, null, (int) HANG_TIMEOUT_MS);
            logs.append(formatMeta(tr));
            if (tr.status >= 500) {
                finish(logs, "FAILED", tr.status, tr.latencyMs,
                        "Failed — server error on baseline (HTTP " + tr.status + ", " + tr.latencyMs + "ms)");
            } else if (tr.latencyMs >= FAIL_LATENCY_MS) {
                finish(logs, "FAILED", tr.status, tr.latencyMs,
                        "Failed — latency " + tr.latencyMs + "ms exceeds SLA " + FAIL_LATENCY_MS + "ms");
            } else if (tr.latencyMs >= WARN_LATENCY_MS) {
                finish(logs, "WARNING", tr.status, tr.latencyMs,
                        "Warning — latency " + tr.latencyMs + "ms is above soft threshold " + WARN_LATENCY_MS + "ms");
            } else {
                finish(logs, "PASSED", tr.status, tr.latencyMs,
                        "Passed — latency " + tr.latencyMs + "ms within SLA (" + FAIL_LATENCY_MS + "ms)");
            }
        } catch (Exception e) {
            hangFail(logs, e);
        }
        return logs.toString();
    }

    public static String runTimeoutHang(String curlCommand) {
        StringBuilder logs = new StringBuilder();
        logs.append("=== Timeout / Hang Detection ===\n\n");
        logs.append("Client timeout: ").append(HANG_TIMEOUT_MS).append("ms\n");
        long wallStart = System.currentTimeMillis();
        try {
            TimedResponse tr = send(curlCommand, null, (int) HANG_TIMEOUT_MS);
            long wall = System.currentTimeMillis() - wallStart;
            logs.append(formatMeta(tr));
            logs.append("Wall clock: ").append(wall).append("ms\n");
            if (wall >= HANG_TIMEOUT_MS - 50) {
                finish(logs, "FAILED", tr.status, tr.latencyMs,
                        "Failed — request approached hang timeout (" + wall + "ms)");
            } else {
                finish(logs, "PASSED", tr.status, tr.latencyMs,
                        "Passed — responded in " + tr.latencyMs + "ms (within " + HANG_TIMEOUT_MS + "ms)");
            }
        } catch (Exception e) {
            hangFail(logs, e);
        }
        return logs.toString();
    }

    public static String runErrorPathLatency(String curlCommand) {
        StringBuilder logs = new StringBuilder();
        logs.append("=== Error-Path Latency ===\n\n");
        try {
            ParsedCurl curl = CurlParser.parseCurl(curlCommand);
            String errorBody = buildErrorBody(curl);
            logs.append("Error probe: ").append(errorBody != null ? "malformed/invalid body" : "no-body force").append("\n");

            TimedResponse tr;
            if (errorBody != null) {
                tr = send(curlCommand, errorBody, (int) HANG_TIMEOUT_MS);
            } else {
                // Force a likely 4xx: strip auth if present, else wrong content-type GET/HEAD with junk query
                tr = sendErrorProbe(curl);
            }

            logs.append(formatMeta(tr));
            if (tr.status >= 500) {
                finish(logs, "FAILED", tr.status, tr.latencyMs,
                        "Failed — error path returned 5xx (HTTP " + tr.status + ", " + tr.latencyMs + "ms)");
            } else if (tr.status >= 400 && tr.status < 500) {
                if (tr.latencyMs >= ERROR_PATH_WARN_MS) {
                    finish(logs, "WARNING", tr.status, tr.latencyMs,
                            "Warning — error path slow: " + tr.latencyMs + "ms (threshold " + ERROR_PATH_WARN_MS + "ms)");
                } else {
                    finish(logs, "PASSED", tr.status, tr.latencyMs,
                            "Passed — error path HTTP " + tr.status + " in " + tr.latencyMs + "ms");
                }
            } else if (tr.status >= 200 && tr.status < 300) {
                finish(logs, "WARNING", tr.status, tr.latencyMs,
                        "Warning — error probe still accepted (HTTP " + tr.status + "); latency "
                                + tr.latencyMs + "ms");
            } else {
                finish(logs, "WARNING", tr.status, tr.latencyMs,
                        "Warning — unexpected status HTTP " + tr.status + " in " + tr.latencyMs + "ms");
            }
        } catch (Exception e) {
            hangFail(logs, e);
        }
        return logs.toString();
    }

    public static String runLargePayloadLatency(String curlCommand) {
        StringBuilder logs = new StringBuilder();
        logs.append("=== Large Payload Latency ===\n\n");
        try {
            ParsedCurl curl = CurlParser.parseCurl(curlCommand);
            if (curl.getBody() == null || curl.getBody().isBlank()) {
                logs.append("Test Case Skipped.\nNo request body");
                return logs.toString();
            }

            TimedResponse baseline = send(curlCommand, null, (int) HANG_TIMEOUT_MS);
            String large = enlargeBody(curl.getBody(), 40);
            TimedResponse largeTr = send(curlCommand, large, (int) HANG_TIMEOUT_MS);

            long budget = Math.max(LARGE_ABS_CAP_MS, baseline.latencyMs * 5);
            logs.append("Baseline latency: ").append(baseline.latencyMs).append("ms (HTTP ")
                    .append(baseline.status).append(")\n");
            logs.append("Large payload size: ").append(large.length()).append(" chars\n");
            logs.append("Budget: ").append(budget).append("ms (max of ")
                    .append(LARGE_ABS_CAP_MS).append("ms or 5× baseline)\n");
            logs.append(formatMeta(largeTr));

            if (largeTr.status >= 500) {
                finish(logs, "FAILED", largeTr.status, largeTr.latencyMs,
                        "Failed — large payload caused HTTP " + largeTr.status + " in " + largeTr.latencyMs + "ms");
            } else if (largeTr.latencyMs > budget) {
                finish(logs, "FAILED", largeTr.status, largeTr.latencyMs,
                        "Failed — large payload latency " + largeTr.latencyMs + "ms exceeds budget " + budget + "ms");
            } else if (largeTr.latencyMs > Math.max(FAIL_LATENCY_MS, baseline.latencyMs * 3)) {
                finish(logs, "WARNING", largeTr.status, largeTr.latencyMs,
                        "Warning — large payload slower than expected (" + largeTr.latencyMs
                                + "ms vs baseline " + baseline.latencyMs + "ms)");
            } else {
                finish(logs, "PASSED", largeTr.status, largeTr.latencyMs,
                        "Passed — large payload HTTP " + largeTr.status + " in " + largeTr.latencyMs
                                + "ms (budget " + budget + "ms)");
            }
        } catch (Exception e) {
            hangFail(logs, e);
        }
        return logs.toString();
    }

    public static String runRepeatGetStability(String curlCommand) {
        StringBuilder logs = new StringBuilder();
        logs.append("=== Repeat GET Stability ===\n\n");
        try {
            ParsedCurl curl = CurlParser.parseCurl(curlCommand);
            if (!"GET".equalsIgnoreCase(curl.getMethod())) {
                logs.append("Test Case Skipped.\nNot a GET request");
                return logs.toString();
            }

            List<Long> times = new ArrayList<>();
            List<Integer> statuses = new ArrayList<>();
            for (int i = 0; i < REPEAT_GET_COUNT; i++) {
                TimedResponse tr = send(curlCommand, null, (int) HANG_TIMEOUT_MS);
                times.add(tr.latencyMs);
                statuses.add(tr.status);
                logs.append("Call ").append(i + 1).append(": HTTP ").append(tr.status)
                        .append(" · ").append(tr.latencyMs).append("ms\n");
            }

            long median = median(times);
            long max = Collections.max(times);
            boolean statusStable = statuses.stream().distinct().count() == 1;
            int lastStatus = statuses.get(statuses.size() - 1);
            long spreadBudget = Math.max(FAIL_LATENCY_MS, median * 3);

            logs.append("Median: ").append(median).append("ms · Max: ").append(max)
                    .append("ms · Spread budget: ").append(spreadBudget).append("ms\n");
            logs.append("API Status Code: ").append(lastStatus).append("\n");
            logs.append("Latency Ms: ").append(max).append("\n");

            if (!statusStable) {
                finish(logs, "FAILED", lastStatus, max,
                        "Failed — status codes unstable across repeats: " + statuses);
            } else if (max >= FAIL_LATENCY_MS && max > spreadBudget) {
                finish(logs, "FAILED", lastStatus, max,
                        "Failed — max latency " + max + "ms exceeds spread budget " + spreadBudget + "ms");
            } else if (max >= FAIL_LATENCY_MS || max > median * 3) {
                finish(logs, "WARNING", lastStatus, max,
                        "Warning — latency variance high (median " + median + "ms, max " + max + "ms)");
            } else {
                finish(logs, "PASSED", lastStatus, max,
                        "Passed — " + REPEAT_GET_COUNT + " GETs stable (median " + median + "ms, max " + max + "ms)");
            }
        } catch (Exception e) {
            hangFail(logs, e);
        }
        return logs.toString();
    }

    private static void hangFail(StringBuilder logs, Exception e) {
        logs.append("API/network error: ").append(e.getMessage()).append("\n");
        logs.append("Latency Ms: ").append(HANG_TIMEOUT_MS).append("\n");
        finish(logs, "FAILED", null, HANG_TIMEOUT_MS,
                "Failed — timeout/hang or transport error: " + e.getMessage());
    }

    private static void finish(StringBuilder logs, String verdict, Integer status, long latencyMs, String reason) {
        if (status != null && !logs.toString().contains("API Status Code:")) {
            logs.append("API Status Code: ").append(status).append("\n");
        }
        if (!logs.toString().contains("Latency Ms:")) {
            logs.append("Latency Ms: ").append(latencyMs).append("\n");
        }
        logs.append("PERF_VERDICT: ").append(verdict).append("\n");
        logs.append(reason);
    }

    private static String formatMeta(TimedResponse tr) {
        return "- Method: " + tr.method + "\n"
                + "- URL: " + tr.url + "\n"
                + "API Status Code: " + tr.status + "\n"
                + "Latency Ms: " + tr.latencyMs + "\n"
                + "Response Time (RA): " + tr.raTimeMs + "ms\n";
    }

    private static TimedResponse send(String curlCommand, String bodyOverride, int socketTimeoutMs) {
        ParsedCurl curl = CurlParser.parseCurl(curlCommand);
        RequestSpecification request = RestAssured.given()
                .config(RestAssured.config().httpClient(
                        HttpClientConfig.httpClientConfig()
                                .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 5_000)
                                .setParam(CoreConnectionPNames.SO_TIMEOUT, socketTimeoutMs)
                ));

        Map<String, Object> headers = curl.getHeaders();
        if (headers != null) {
            headers.forEach((k, v) -> request.header(k, v == null ? "" : v.toString()));
            if (headers.containsKey("Content-Type") && headers.get("Content-Type") != null) {
                request.contentType(headers.get("Content-Type").toString());
            }
        }

        String body = bodyOverride != null ? bodyOverride : curl.getBody();
        if (body != null) {
            request.body(body);
        }

        long start = System.currentTimeMillis();
        Response response = execute(request, curl.getMethod(), curl.getUrl());
        long wall = System.currentTimeMillis() - start;
        long ra = response.getTime();
        long latency = Math.max(wall, ra);

        TimedResponse tr = new TimedResponse();
        tr.method = curl.getMethod();
        tr.url = curl.getUrl();
        tr.status = response.getStatusCode();
        tr.latencyMs = latency;
        tr.raTimeMs = ra;
        return tr;
    }

    private static TimedResponse sendErrorProbe(ParsedCurl curl) {
        RequestSpecification request = RestAssured.given()
                .config(RestAssured.config().httpClient(
                        HttpClientConfig.httpClientConfig()
                                .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 5_000)
                                .setParam(CoreConnectionPNames.SO_TIMEOUT, (int) HANG_TIMEOUT_MS)
                ));

        if (curl.getHeaders() != null) {
            curl.getHeaders().forEach((k, v) -> {
                String key = k == null ? "" : k;
                if (key.equalsIgnoreCase("Authorization")
                        || key.equalsIgnoreCase("Cookie")
                        || key.toLowerCase(Locale.ROOT).contains("api-key")
                        || key.toLowerCase(Locale.ROOT).contains("apikey")
                        || key.equalsIgnoreCase("X-API-Key")) {
                    return;
                }
                request.header(k, v == null ? "" : v.toString());
            });
        }

        String url = curl.getUrl();
        if (url != null && !url.contains("perf_probe=1")) {
            url = url + (url.contains("?") ? "&" : "?") + "perf_probe=invalid";
        }

        long start = System.currentTimeMillis();
        Response response = execute(request, curl.getMethod(), url);
        long wall = System.currentTimeMillis() - start;

        TimedResponse tr = new TimedResponse();
        tr.method = curl.getMethod();
        tr.url = url;
        tr.status = response.getStatusCode();
        tr.latencyMs = Math.max(wall, response.getTime());
        tr.raTimeMs = response.getTime();
        return tr;
    }

    private static String buildErrorBody(ParsedCurl curl) {
        String method = curl.getMethod() == null ? "GET" : curl.getMethod().toUpperCase(Locale.ROOT);
        boolean bodyMethod = method.equals("POST") || method.equals("PUT") || method.equals("PATCH");
        if (!bodyMethod && (curl.getBody() == null || curl.getBody().isBlank())) {
            return null;
        }
        if (curl.getBody() != null && !curl.getBody().isBlank()) {
            return "{ \"__perf_invalid\": true, "; // malformed JSON
        }
        return "{\"__perf_invalid\":true,\"x\":"; // malformed
    }

    private static String enlargeBody(String body, int times) {
        StringBuilder sb = new StringBuilder(body.length() * Math.max(1, times) + 64);
        sb.append(body);
        // Prefer injecting a large string field if JSON-ish; else repeat
        if (body.trim().startsWith("{") && body.contains(":")) {
            String pad = "\"__perf_pad\":\"" + "x".repeat(8_192) + "\"";
            int brace = body.lastIndexOf('}');
            if (brace > 0) {
                String head = body.substring(0, brace).trim();
                if (head.endsWith("{")) {
                    return head + pad + "}";
                }
                return head + "," + pad + "}";
            }
        }
        for (int i = 1; i < times; i++) {
            sb.append(body);
        }
        return sb.toString();
    }

    private static long median(List<Long> values) {
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2;
    }

    private static Response execute(RequestSpecification request, String method, String url) {
        String m = method == null ? "GET" : method.toUpperCase(Locale.ROOT);
        return switch (m) {
            case "GET" -> request.get(url);
            case "POST" -> request.post(url);
            case "PUT" -> request.put(url);
            case "PATCH" -> request.patch(url);
            case "DELETE" -> request.delete(url);
            case "HEAD" -> request.head(url);
            case "OPTIONS" -> request.options(url);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };
    }

    private static final class TimedResponse {
        String method;
        String url;
        int status;
        long latencyMs;
        long raTimeMs;
    }
}
