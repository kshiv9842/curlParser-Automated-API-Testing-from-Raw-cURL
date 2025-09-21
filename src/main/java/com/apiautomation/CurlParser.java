package com.apiautomation;

import com.apiautomation.Utils.CurlParserHelper;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;
import java.util.regex.*;

public class CurlParser {

    public static ParsedCurl parseCurl(String curlCommand) {
        ParsedCurl parsedCurl = new ParsedCurl();

        // Normalize: join line continuations and collapse multiple spaces
        String normalized = curlCommand.replaceAll("\\\\\\n", " ").replaceAll("\\s+\\\\\\s*", " ");

        // Extract method (-X/--request) and other method hints (-I/--head)
        Matcher methodMatcher = Pattern.compile("(?i)(?:--request|-X)\\s+(\\w+)").matcher(normalized);
        if (methodMatcher.find()) {
            parsedCurl.setMethod(methodMatcher.group(1).toUpperCase());
        }
        if (Pattern.compile("(?i)(?:--head|-I)\\b").matcher(normalized).find()) {
            parsedCurl.setMethod("HEAD");
        }

        // Infer method from body/form flags if not explicitly set
        if (parsedCurl.getMethod() == null) {
            if (Pattern.compile("(?i)(?:--data|-d|--data-raw|--data-binary|--data-urlencode|--form|-F)\\b").matcher(normalized).find()) {
                parsedCurl.setMethod("POST");
            } else {
                parsedCurl.setMethod("GET");
            }
        }

        // Extract URL: support --url, quoted/unquoted http(s) tokens
        String url = null;
        Matcher urlFlag = Pattern.compile("(?i)--url\\s+([^\\s]+)|--url\\s+(['\"])\\s*([^\"]*?)\\2").matcher(normalized);
        if (urlFlag.find()) {
            url = urlFlag.group(1) != null ? urlFlag.group(1) : urlFlag.group(3);
        }
        if (url == null) {
            // quoted http(s)
            Matcher qHttp = Pattern.compile("(['\"])((?:https?://)[^'\"]+)\\1").matcher(normalized);
            if (qHttp.find()) url = qHttp.group(2);
        }
        if (url == null) {
            // unquoted http(s)
            Matcher uqHttp = Pattern.compile("\\bhttps?://\\S+").matcher(normalized);
            if (uqHttp.find()) url = uqHttp.group();
        }
        parsedCurl.setUrl(url);

        // Extract headers: -H/--header with single or double quotes
        Map<String, Object> headers = new HashMap<>();
        Matcher headerMatcher = Pattern.compile("(?i)(?:-H|--header)\\s+(['\"])\\s*([^:]+):\\s*([^\"]*?)\\1").matcher(normalized);
        while (headerMatcher.find()) {
            String key = headerMatcher.group(2).trim();
            String value = headerMatcher.group(3).trim();
            headers.put(key, value);
        }
        // Also capture unquoted header values (best-effort)
        Matcher headerMatcher2 = Pattern.compile("(?i)(?:-H|--header)\\s+([^'\"\n]+)").matcher(normalized);
        while (headerMatcher2.find()) {
            String hv = headerMatcher2.group(1).trim();
            int idx = hv.indexOf(":");
            if (idx > 0) {
                headers.put(hv.substring(0, idx).trim(), hv.substring(idx + 1).trim());
            }
        }

        // Cookies: --cookie/ -b
        Matcher cookieMatcher = Pattern.compile("(?i)(?:--cookie|-b)\\s+(['\"])\\s*([^\"]*?)\\1").matcher(normalized);
        if (cookieMatcher.find()) {
            headers.put("Cookie", cookieMatcher.group(2).trim());
        }

        // Basic auth: -u/--user user:pass
        Matcher userMatcher = Pattern.compile("(?i)(?:-u|--user)\\s+(['\"])?.*?([A-Za-z0-9._%+-]+:[^'\"\\s]+)\\1?").matcher(normalized);
        if (userMatcher.find()) {
            String cred = userMatcher.group(2);
            String encoded = Base64.getEncoder().encodeToString(cred.getBytes(StandardCharsets.UTF_8));
            headers.putIfAbsent("Authorization", "Basic " + encoded);
        }

        parsedCurl.setHeaders(headers);

        // Extract form params: -F/--form field=value
        Map<String, String> formParams = new HashMap<>();
        Matcher formMatcher = Pattern.compile("(?i)(?:-F|--form)\\s+(['\"])\\s*([^=]+)=([^\"]*?)\\1").matcher(normalized);
        while (formMatcher.find()) {
            formParams.put(formMatcher.group(2).trim(), formMatcher.group(3).trim());
        }
        if (!formParams.isEmpty()) {
            parsedCurl.setFormParams(formParams);
            parsedCurl.setContentType("multipart/form-data");
            headers.putIfAbsent("Content-Type", "multipart/form-data");
        }

        // Extract body: --data/-d/--data-raw/--data-binary/--data-urlencode (single or double quotes)
        String body = null;
        Matcher dataMatcher = Pattern.compile(
                "(?i)(?:--data|-d|--data-raw|--data-binary|--data-urlencode)\\s+(['\"])\\s*([\\r\\t\\s\\S]*?)\\1"
        ).matcher(normalized);
        if (dataMatcher.find()) {
            body = dataMatcher.group(2).trim();
        } else {
            // unquoted simple data
            Matcher dataMatcher2 = Pattern.compile("(?i)(?:--data|-d)\\s+([^'\\\\\"\\s][^\\\\n]*)").matcher(normalized);
            if (dataMatcher2.find()) body = dataMatcher2.group(1).trim();
        }
        if (body != null) {
            parsedCurl.setBody(body.replace("\\n", ""));
            // Infer JSON content type if body looks like JSON and not set
            if (!headers.containsKey("Content-Type") && body.trim().startsWith("{")) {
                parsedCurl.setContentType("application/json");
                headers.put("Content-Type", "application/json");
            }
        }

        // Extract path parameters and query parameters from URL
        if (parsedCurl.getUrl() != null) {
            parsedCurl.setPathParams(CurlParserHelper.extractPathParams(parsedCurl.getUrl()));
            parsedCurl.setQueryParams(CurlParserHelper.extractQueryParams(parsedCurl.getUrl()));
        }
        return parsedCurl;
    }
}
