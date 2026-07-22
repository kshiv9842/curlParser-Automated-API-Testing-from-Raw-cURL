package com.apiautomation.ai;

import com.apiautomation.CurlParser;
import com.apiautomation.ParsedCurl;
import com.apiautomation.report.ScenarioResult;
import com.apiautomation.testcase.BugOracle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Locale;
import java.util.Map;

/**
 * Executes only allowlisted mutations against the original curl URL.
 */
public final class AiScenarioExecutor {

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private AiScenarioExecutor() {
    }

    public static ScenarioResult run(String curlCommand, AiScenarioSpec spec, String sourceTag) {
        ParsedCurl original = CurlParser.parseCurl(curlCommand);
        try {
            MutatedRequest mutated = apply(original, spec.getMutation());
            Response response = send(mutated);

            StringBuilder detail = new StringBuilder();
            detail.append("=== AI Scenario: ").append(spec.getId()).append(" ===\n");
            detail.append("Source: ").append(sourceTag).append("\n");
            detail.append("Mutation: ").append(spec.getMutation().getType()).append("\n");
            detail.append("Request Method: ").append(mutated.method).append("\n");
            detail.append("Request URL: ").append(mutated.url).append("\n");
            detail.append("Request Body: ").append(mutated.body).append("\n");
            detail.append("API Status Code: ").append(response.getStatusCode()).append("\n");
            detail.append("Response Time: ").append(response.getTime()).append("ms\n");
            try {
                detail.append("Response Body: ").append(response.body().asPrettyString()).append("\n");
            } catch (Exception e) {
                detail.append("Response Body: (unavailable)\n");
            }

            BugOracle oracle = BugOracle.valueOf(spec.getOracle());
            return ScenarioResult.fromAiSpec(spec, detail.toString(), response.getStatusCode(), oracle, sourceTag);
        } catch (Exception e) {
            return ScenarioResult.aiError(spec, e.getMessage(), sourceTag);
        }
    }

    private static MutatedRequest apply(ParsedCurl original, AiMutation mutation) {
        MutatedRequest req = new MutatedRequest();
        req.method = original.getMethod() == null ? "GET" : original.getMethod().toUpperCase(Locale.ROOT);
        req.url = original.getUrl();
        req.body = original.getBody();
        req.headers = original.getHeaders() == null
                ? new java.util.HashMap<>()
                : new java.util.HashMap<>(original.getHeaders());

        switch (mutation.getType()) {
            case "OMIT_FIELD" -> req.body = omitField(req.body, mutation.getField());
            case "REPLACE_FIELD" -> req.body = replaceField(req.body, mutation.getField(), mutation.getValue());
            case "OMIT_BODY" -> req.body = null;
            case "OMIT_HEADER" -> {
                String h = mutation.getHeader();
                req.headers.entrySet().removeIf(e -> e.getKey() != null && e.getKey().equalsIgnoreCase(h));
            }
            case "SET_CONTENT_TYPE" -> {
                req.headers.entrySet().removeIf(e -> e.getKey() != null && e.getKey().equalsIgnoreCase("Content-Type"));
                req.headers.put("Content-Type", mutation.getValue() != null ? mutation.getValue().toString() : "text/plain");
            }
            case "WRONG_METHOD" -> req.method = mutation.getMethod().toUpperCase(Locale.ROOT);
            case "INVALID_JSON" -> {
                if (req.body != null) {
                    req.body = req.body.replace("{", "").replace("}", "");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported mutation: " + mutation.getType());
        }
        return req;
    }

    private static String omitField(String body, String fieldPath) {
        JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
        removePath(obj, fieldPath.split("\\."));
        return GSON.toJson(obj);
    }

    private static String replaceField(String body, String fieldPath, Object value) {
        JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
        setPath(obj, fieldPath.split("\\."), value);
        return GSON.toJson(obj);
    }

    private static void removePath(JsonObject obj, String[] parts) {
        JsonObject cur = obj;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonElement next = cur.get(parts[i]);
            if (next == null || !next.isJsonObject()) {
                return;
            }
            cur = next.getAsJsonObject();
        }
        cur.remove(parts[parts.length - 1]);
    }

    private static void setPath(JsonObject obj, String[] parts, Object value) {
        JsonObject cur = obj;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonElement next = cur.get(parts[i]);
            if (next == null || !next.isJsonObject()) {
                JsonObject created = new JsonObject();
                cur.add(parts[i], created);
                cur = created;
            } else {
                cur = next.getAsJsonObject();
            }
        }
        String leaf = parts[parts.length - 1];
        if (value == null) {
            cur.add(leaf, com.google.gson.JsonNull.INSTANCE);
        } else if (value instanceof Number n) {
            cur.addProperty(leaf, n);
        } else if (value instanceof Boolean b) {
            cur.addProperty(leaf, b);
        } else {
            cur.addProperty(leaf, value.toString());
        }
    }

    private static Response send(MutatedRequest req) {
        RequestSpecification request = RestAssured.given();
        for (Map.Entry<String, Object> e : req.headers.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                request.header(e.getKey(), e.getValue().toString());
            }
        }
        if (req.headers.keySet().stream().anyMatch(k -> k != null && k.equalsIgnoreCase("Content-Type"))) {
            Object ct = req.headers.entrySet().stream()
                    .filter(e -> e.getKey() != null && e.getKey().equalsIgnoreCase("Content-Type"))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
            if (ct != null) {
                request.contentType(ct.toString());
            }
        }
        if (req.body != null) {
            request.body(req.body);
        }
        String m = req.method;
        return switch (m) {
            case "GET" -> request.get(req.url);
            case "POST" -> request.post(req.url);
            case "PUT" -> request.put(req.url);
            case "PATCH" -> request.patch(req.url);
            case "DELETE" -> request.delete(req.url);
            case "HEAD" -> request.head(req.url);
            case "OPTIONS" -> request.options(req.url);
            default -> throw new IllegalArgumentException("Unsupported method " + m);
        };
    }

    private static final class MutatedRequest {
        String method;
        String url;
        String body;
        Map<String, Object> headers;
    }
}
