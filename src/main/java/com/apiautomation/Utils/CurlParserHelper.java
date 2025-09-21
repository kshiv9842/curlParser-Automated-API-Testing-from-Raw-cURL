package com.apiautomation.Utils;

import com.apiautomation.ParsedCurl;
import com.apiautomation.PayloadMutator;
import java.util.*;
import java.util.regex.*;

public class CurlParserHelper {

    // Helper method to extract path params
    public static List<String> extractPathParams(String url) {
        final Pattern UUID_PATTERN = Pattern.compile("^[a-f0-9\\-]{20,}$", Pattern.CASE_INSENSITIVE);
        final Pattern NUMERIC_ID_PATTERN = Pattern.compile("^\\d+$");

        // Only allow alphanumeric patterns with both letters and digits, at least 10 chars
        final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z0-9_-]+$");
        List<String> dynamicParams = new ArrayList<>();
        try {
            // Remove domain and query parameters
            String path = url.replaceAll("https?://[^/]+", "");
            int queryIndex = path.indexOf('?');
            if (queryIndex != -1) {
                path = path.substring(0, queryIndex);
            }
            String[] segments = path.split("/");
            for (String segment : segments) {
                if (segment.isEmpty()) continue;
                if (segment.matches("^v\\d+$")) continue;
                // Match only if the segment looks like an ID or dynamic token
                if (UUID_PATTERN.matcher(segment).matches() ||
                        NUMERIC_ID_PATTERN.matcher(segment).matches() ||
                        ALPHANUMERIC_PATTERN.matcher(segment).matches()) {
                    dynamicParams.add(segment);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dynamicParams;
    }

    // Helper method to extract query parameters
    public static Map<String, Object> extractQueryParams(String url) {
        Map<String, Object> queryParams = new HashMap<>();
        try {
            int queryIndex = url.indexOf('?');
            if (queryIndex == -1) {
                return queryParams; // No query parameters
            }

            String queryString = url.substring(queryIndex + 1);
            String[] pairs = queryString.split("&");
            
            for (String pair : pairs) {
                if (pair.contains("=")) {
                    String[] keyValue = pair.split("=", 2);
                    String key = keyValue[0];
                    String value = keyValue.length > 1 ? keyValue[1] : "";
                    queryParams.put(key, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return queryParams;
    }

    // Helper method to modify existing query parameters
    public static String modifyExistingQueryParams(String url) {
        try {
            int queryIndex = url.indexOf('?');
            if (queryIndex == -1) {
                return url; // No query parameters to modify
            }

            String baseUrl = url.substring(0, queryIndex);
            String queryString = url.substring(queryIndex + 1);

            Map<String, String> queryParams = new HashMap<>();
            String[] pairs = queryString.split("&");

            for (String pair : pairs) {
                if (pair.contains("=")) {
                    String[] keyValue = pair.split("=", 2);
                    String key = keyValue[0];
                    String value = keyValue.length > 1 ? keyValue[1] : "";

                    if (key.toLowerCase().contains("id") || key.toLowerCase().contains("user")) {
                        queryParams.put(key, "invalid_id_123");
                    } else if (key.toLowerCase().contains("page") || key.toLowerCase().contains("limit")) {
                        queryParams.put(key, "-1");
                    } else if (key.toLowerCase().contains("status") || key.toLowerCase().contains("type")) {
                        queryParams.put(key, "invalid_status");
                    } else {
                        queryParams.put(key, "modified_" + value);
                    }
                }
            }

            StringBuilder newQueryString = new StringBuilder();
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (newQueryString.length() > 0) {
                    newQueryString.append("&");
                }
                newQueryString.append(entry.getKey()).append("=").append(entry.getValue());
            }

            return baseUrl + "?" + newQueryString.toString();

        } catch (Exception e) {
            return url + "&invalidParam=test";
        }
    }

    public static String UpdatePayload(String curlCommand){
        ParsedCurl parsedCurl = new ParsedCurl();
        // Extract body
        Matcher bodyMatcher = Pattern.compile("--data(?:-raw)?\\s+'(\\{[\\s\\S]*?})'").matcher(curlCommand);
        String mutatedPayload = null;
        if (bodyMatcher.find()) {
            String body = bodyMatcher.group(1).replace("\\n", "").replace("\\", "");
            parsedCurl.setBody(body);

            // Auto-generate mutation rules
            Map<String, String> mutationRules = PayloadMutator.generateMutationRules(body);

            // Mutate payload
            mutatedPayload = PayloadMutator.mutatePayload(body, mutationRules);
            parsedCurl.setBody(mutatedPayload);
        }
        return mutatedPayload;
    }
}
