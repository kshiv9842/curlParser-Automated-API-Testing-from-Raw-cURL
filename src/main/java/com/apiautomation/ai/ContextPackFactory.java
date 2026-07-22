package com.apiautomation.ai;

import com.apiautomation.ParsedCurl;
import com.apiautomation.testcase.BugTestCatalog;
import com.apiautomation.testcase.RequestFacts;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ContextPackFactory {

    private ContextPackFactory() {
    }

    public static ContextPack from(ParsedCurl curl, RequestFacts facts) {
        ContextPack pack = new ContextPack();
        pack.setMethod(facts.getMethod());
        pack.setPath(extractPath(curl.getUrl()));
        pack.setProfileHint(detectProfile(pack.getPath(), facts.getMethod()));

        Map<String, Boolean> f = new LinkedHashMap<>();
        f.put("hasAuth", facts.hasAuth());
        f.put("hasBody", facts.hasBody());
        f.put("isJson", facts.isJson());
        f.put("hasQuery", facts.hasQuery());
        f.put("hasPathId", facts.hasPathId());
        f.put("hasHeaders", facts.hasHeaders());
        pack.setFacts(f);

        List<String> fields = new ArrayList<>();
        Map<String, String> types = new LinkedHashMap<>();
        if (facts.hasBody() && facts.isJson() && curl.getBody() != null) {
            try {
                JsonElement el = JsonParser.parseString(curl.getBody());
                if (el.isJsonObject()) {
                    collectFields(el.getAsJsonObject(), "", fields, types);
                }
            } catch (Exception ignored) {
                // leave fields empty
            }
        }
        pack.setFields(fields);
        pack.setFieldTypes(types);

        List<String> covered = new ArrayList<>();
        BugTestCatalog.all().forEach(d -> covered.add(d.getId()));
        pack.setAlreadyCoveredCaseIds(covered);
        pack.setMaxScenarios(5);
        return pack;
    }

    private static void collectFields(JsonObject obj, String prefix, List<String> fields, Map<String, String> types) {
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            JsonElement v = e.getValue();
            if (v.isJsonPrimitive()) {
                fields.add(key);
                if (v.getAsJsonPrimitive().isNumber()) {
                    types.put(key, "number");
                } else if (v.getAsJsonPrimitive().isBoolean()) {
                    types.put(key, "boolean");
                } else {
                    types.put(key, "string");
                }
            } else if (v.isJsonNull()) {
                fields.add(key);
                types.put(key, "null");
            } else if (v.isJsonObject()) {
                collectFields(v.getAsJsonObject(), key, fields, types);
            }
            // skip arrays for v1 simplicity
        }
    }

    private static String extractPath(String url) {
        if (url == null) {
            return "";
        }
        try {
            String path = URI.create(url).getPath();
            return path == null ? "" : path;
        } catch (Exception e) {
            return url;
        }
    }

    private static String detectProfile(String path, String method) {
        String p = path == null ? "" : path.toLowerCase(Locale.ROOT);
        if (p.contains("login") || p.contains("signin") || p.contains("sign-in") || p.contains("/token")) {
            return "AUTH_LOGIN";
        }
        if ("GET".equals(method) && (p.contains("list") || p.contains("search") || p.contains("home"))) {
            return "LIST_READ";
        }
        if ("POST".equals(method)) {
            return "CREATE_OR_ACTION";
        }
        if ("PUT".equals(method) || "PATCH".equals(method)) {
            return "UPDATE";
        }
        if ("DELETE".equals(method)) {
            return "DELETE";
        }
        return "GENERIC";
    }
}
