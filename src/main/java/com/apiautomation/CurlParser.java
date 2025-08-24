package com.apiautomation;

import com.google.gson.*;

import java.util.*;
import java.util.regex.*;

public class CurlParser {

    public static ParsedCurl parseCurl(String curlCommand) {
        ParsedCurl parsedCurl = new ParsedCurl();

        // Extract method
        Matcher methodMatcher = Pattern.compile("(?i)--request\\s+(\\w+)|-X\\s+(\\w+)").matcher(curlCommand);
        if (methodMatcher.find()) {
            parsedCurl.setMethod(methodMatcher.group(1) != null ? methodMatcher.group(1).toUpperCase() : methodMatcher.group(2).toUpperCase());
        }

        // Check if --data implies POST
        if (curlCommand.contains("--data") || curlCommand.contains("--data-raw") || curlCommand.contains("--data-binary")) {
            parsedCurl.setMethod("POST");
        } else if (parsedCurl.getMethod() == null) {
            parsedCurl.setMethod("GET");
        }

        // Extract URL
        Matcher urlMatcher = Pattern.compile("--request\\s+\\w+\\s+'([^']+)'").matcher(curlCommand);
        if (urlMatcher.find()) {
            parsedCurl.setUrl(urlMatcher.group(1));
        } else {
            Matcher fallbackUrl = Pattern.compile("curl(?:\\s+--\\w+)*\\s+'([^']+)'").matcher(curlCommand);
            if (fallbackUrl.find()) {
                parsedCurl.setUrl(fallbackUrl.group(1));
            }
        }

        // Extract headers
        Map<String, Object> headers = new HashMap<>();
        Matcher headerMatcher = Pattern.compile("--header\\s+'([^:]+):\\s?([^']+)'").matcher(curlCommand);
        while (headerMatcher.find()) {
            headers.put(headerMatcher.group(1).trim(), headerMatcher.group(2).trim());
        }
        parsedCurl.setHeaders(headers);

        // Extract body
       Matcher bodyMatcher = Pattern.compile("--data(?:-raw)?\\s+'(\\{[\\s\\S]*?})'").matcher(curlCommand);
        if (bodyMatcher.find()) {
            parsedCurl.setBody(bodyMatcher.group(1).replace("\\n", "").replace("\\", ""));
        }

        // Extract path parameters from URL
        if (parsedCurl.getUrl() != null) {
            parsedCurl.setPathParams(extractPathParams(parsedCurl.getUrl()));
        }
        return parsedCurl;
    }
    // Helper method to extract path params
    private static List<String> extractPathParams(String url) {
        final Pattern UUID_PATTERN = Pattern.compile("^[a-f0-9\\-]{20,}$", Pattern.CASE_INSENSITIVE);
        final Pattern NUMERIC_ID_PATTERN = Pattern.compile("^\\d{4,}$");

        // Only allow alphanumeric patterns with both letters and digits, at least 10 chars
        final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z0-9_-]{10,}$");
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