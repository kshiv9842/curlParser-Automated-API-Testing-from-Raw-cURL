package com.apiautomation;

import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import com.apiautomation.Utils.SqlInjectionHelper;
import com.apiautomation.Utils.CurlParserHelper;
import org.json.JSONObject;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.regex.Pattern;

public class ApiSmokeTest {
    
    /**
     * Configure RestAssured with proper encoding for different content types
     */
    private static void configureRestAssuredEncoding() {
        RestAssured.config = RestAssured.config()
            .encoderConfig(EncoderConfig.encoderConfig()
                .encodeContentTypeAs("application/json", ContentType.JSON)
                .encodeContentTypeAs("application/xml", ContentType.XML)
                .encodeContentTypeAs("text/plain", ContentType.TEXT)
                .encodeContentTypeAs("application/x-www-form-urlencoded", ContentType.URLENC)
                .encodeContentTypeAs("multipart/form-data", ContentType.MULTIPART));
    }
    
    /**
     * Safely encode body content to prevent encoding errors
     */
    private static String encodeBodySafely(Object body) {
        if (body == null) {
            return null;
        }
        
        // If it's already a string, return as is
        if (body instanceof String) {
            return (String) body;
        }
        
        // Convert other types to string
        return body.toString();
    }

    /** Execute request with the real HTTP method (fixes GET-for-POST bugs). */
    private static Response executeRequest(RequestSpecification request, String method, String url) {
        String m = method == null ? "GET" : method.toUpperCase();
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

    private static boolean is2xx(int status) {
        return status >= 200 && status < 300;
    }

    private static boolean isClientError(int status) {
        return status >= 400 && status < 500;
    }

    private static void appendVerdict(StringBuilder logs, boolean passed) {
        logs.append(passed ? "Passed" : "Failed");
    }

    private static void appendVerdict(StringBuilder logs, String verdict) {
        logs.append(verdict);
    }
    
    // @Test
    public static String runBasicSmokeTest(String curlCommand) {
        StringBuilder logs = new StringBuilder();
        
        // Configure RestAssured encoding
        configureRestAssuredEncoding();

        ParsedCurl curl = CurlParser.parseCurl(curlCommand);
        RequestSpecification request = RestAssured.given();
        curl.getHeaders().forEach(request::header);

        if (curl.getHeaders().containsKey("Content-Type")) {
            request.contentType(curl.getHeaders().get("Content-Type").toString());
        }

        if (curl.getBody() != null) {
            // Safely encode body to prevent encoding errors
            String bodyString = encodeBodySafely(curl.getBody());
            request.body(bodyString);
        }
        Response response =null;
        try {
         response = executeRequest(request, curl.getMethod(), curl.getUrl());
    } 
    catch (Exception e) 
    {
     System.err.println("API/network error: " + e.getMessage());
        logs.append("API/network error: ").append(e.getMessage()).append("\n");
        // Optionally, add stack trace
        java.io.StringWriter sw = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        logs.append(sw.toString()).append("\n");
    }

        System.out.println("=== Smoke Test Results ===");
        System.out.println("- Request Headers - "+curl.getHeaders());
        System.out.println("- Request Body - "+curl.getBody());
        System.out.println("- Request Path Param - "+curl.getPathParams());
        System.out.println("Response URL: " + curl.getUrl());
        System.out.println("Response Method: " + curl.getMethod());
        if (response != null) {
            System.out.println("API Status Code: " + response.getStatusCode());
            System.out.println("Response Time: " + response.getTime() + "ms");
        } else {
            System.out.println("API Status Code: No response received");
            System.out.println("Response Time: N/A");
        }

        logs.append("=== Smoke Test Results ===").append("\n\n\n");
        logs.append("- Request Headers - "+curl.getHeaders()).append("\n");
        logs.append("- Request Body - "+curl.getBody()).append("\n");
        logs.append("- Request Path Param - "+curl.getPathParams()).append("\n");
        logs.append("Response URL: " + curl.getUrl()).append("\n");
        logs.append("Response Method: " + curl.getMethod()).append("\n");
        if (response != null) {
            logs.append("API Status Code: " + response.getStatusCode()).append("\n");
            logs.append("Response Time: " + response.getTime() + "ms").append("\n");
        } else {
            logs.append("API Status Code: No response received").append("\n");
            logs.append("Response Time: N/A").append("\n");
        }

        // Assert.assertTrue(response.getStatusCode() < 500, "Server error occurred");

        if (response != null) {
            String contentType = response.getContentType();
            if (contentType != null && contentType.contains("json")) {
                try {
                    new JSONObject(response.getBody().asString());
                    logs.append("Response Body Format: Valid JSON").append("\n");
                } catch (Exception e) {
                    logs.append("Response Body Format: Invalid JSON").append("\n");
                    // Assert.fail("Invalid JSON response");
                }
            }
            logs.append("Response Body:").append("\n\n");
            logs.append(response.getBody().asPrettyString()).append("\n\n");

            boolean passed = is2xx(response.getStatusCode());
            if (passed) {
                logs.append("Passed");
            } else {
                logs.append("Failed");
            }
        } else {
            logs.append("No response received - Test Failed");
        }

        //Assert.assertEquals(response.getStatusCode(),200);
        return logs.toString();
    }
    //@Test
    public static String runNegativeMissingHeaders(String curlCommand) {
        StringBuilder logs = new StringBuilder();
        configureRestAssuredEncoding();

        ParsedCurl curl = CurlParser.parseCurl(curlCommand);
        RequestSpecification request = RestAssured.given();

        // Intentionally omit all headers; still send body with the correct method
        if (curl.getBody() != null) {
            request.body(encodeBodySafely(curl.getBody()));
        }

        Response response = null;
        try {
            response = executeRequest(request, curl.getMethod(), curl.getUrl());
        } catch (Exception e) {
            System.err.println("API/network error: " + e.getMessage());
            logs.append("API/network error: ").append(e.getMessage()).append("\n");
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            logs.append(sw.toString()).append("\n");
        }

        logs.append("=== Missing Headers Test Results ===").append("\n\n");
        logs.append("- Request Headers - (none sent)").append("\n");
        logs.append("- Request Method - " + curl.getMethod()).append("\n");
        logs.append("- Request URL - " + curl.getUrl()).append("\n");
        logs.append("- Request Body - " + curl.getBody()).append("\n");
        
        if (response != null) {
            int status = response.getStatusCode();
            logs.append("API Status Code: " + status).append("\n");
            logs.append("Response Headers: " + response.getHeaders()).append("\n");
            logs.append("Response Body: " + response.body().asPrettyString()).append("\n");
            logs.append("Response Time: " + response.getTime() + "ms").append("\n\n");
            // Reject (4xx) = Passed; accept (2xx) = Failed (headers not enforced); 5xx = Failed
            if (isClientError(status)) {
                appendVerdict(logs, true);
            } else if (is2xx(status)) {
                logs.append("Failed - API accepted request without required headers");
            } else {
                appendVerdict(logs, false);
            }
        } else {
            logs.append("API Status Code: No response received").append("\n");
            logs.append("Response Headers: N/A").append("\n");
            logs.append("Response Body: N/A").append("\n");
            logs.append("Response Time: N/A").append("\n\n");
            logs.append("Failed - No response received");
        }
        return logs.toString();
    }

    // @Test
    public static String runNegativeTestRemoveAuth(String curlCommand){
        StringBuilder logs = new StringBuilder();
        
        // Configure RestAssured encoding
        configureRestAssuredEncoding();

        ParsedCurl curl = CurlParser.parseCurl(curlCommand);
        RequestSpecification request = RestAssured.given();
        // remove auth from headers
        if(curl.getHeaders().containsKey("authorization")||curl.getHeaders().containsKey("Authorization")) {
            curl.getHeaders().put("authorization", "");
            curl.getHeaders().put("Authorization", "");

            curl.getHeaders().forEach(request::header);

            if (curl.getHeaders().containsKey("Content-Type")) {
                request.contentType(curl.getHeaders().get("Content-Type").toString());
            }
            if (curl.getBody() != null) {
                request.body(curl.getBody());
            }

            Response response = executeRequest(request, curl.getMethod(), curl.getUrl());

            logs.append("=== Smoke Test Results ===").append("\n\n");
            logs.append("- Request Headers - "+curl.getHeaders()).append("\n");
            logs.append("- Request Body - "+curl.getBody()).append("\n");
            logs.append("API Status Code: " + response.getStatusCode()).append("\n");
            logs.append("Response Body: " + response.body().asPrettyString()).append("\n");
            logs.append("Response Time: " + response.getTime() + "ms").append("\n");
            // Assert.assertTrue(statusCode == 400 || statusCode == 404 || statusCode == 401);
            boolean passed = (response.getStatusCode() == 401 || response.getStatusCode() == 403);
            if (passed) {
                logs.append("Passed");
            } else if (is2xx(response.getStatusCode())) {
                logs.append("Failed - API accepted request without authentication");
            } else {
                logs.append("Failed");
            }
        }
        else {
            logs.append("---- Auth not Exist. ----").append("\n\n");
            logs.append("Test Case Skipped.");
        }
        return logs.toString();
    }

    // @Test
    public static String runNegativeTestRemoveBody(String curlCommand) {
        StringBuilder logs = new StringBuilder();
        configureRestAssuredEncoding();

        ParsedCurl curl = CurlParser.parseCurl(curlCommand);
        RequestSpecification request = RestAssured.given();

        curl.getHeaders().forEach(request::header);
        if (curl.getHeaders().containsKey("Content-Type")) {
            request.contentType(curl.getHeaders().get("Content-Type").toString());
        }
        // Do NOT set body — that is the point of this negative test
        if (curl.getBody() != null) {
            logs.append("---- Body Removed Successfully. ----").append("\n");

            Response response = executeRequest(request, curl.getMethod(), curl.getUrl());

            logs.append("=== Missing Body Test Results ===").append("\n\n");
            logs.append("- Request Headers - "+curl.getHeaders()).append("\n");
            logs.append("- Request Body - (omitted)").append("\n");
            logs.append("API Status Code: " + response.getStatusCode()).append("\n");
            logs.append("Response Body: " + response.body().asPrettyString()).append("\n");
            logs.append("Response Time: " + response.getTime() + "ms").append("\n");
            int status = response.getStatusCode();
            if (status == 400 || status == 422 || status == 415) {
                appendVerdict(logs, true);
            } else if (is2xx(status)) {
                logs.append("Failed - API accepted request with missing body");
            } else {
                appendVerdict(logs, false);
            }
        }
        else {
            logs.append("---- API don't have body. ----").append("\n\n");
            logs.append("Test Case Skipped.");
        }
        return logs.toString();
    }

    // @Test
    public static String runNegativeTestUpdatePathParam(String curlCommand) throws MalformedURLException {
        StringBuilder logs = new StringBuilder();
        ParsedCurl curl = CurlParser.parseCurl(curlCommand);
        RequestSpecification request = RestAssured.given();

        String originalUrl = "";
        Object invalidPathParam;
        String modifiedUrl = "";

        logs.append("Path Param List - " + curl.getPathParams()).append("\n\n");
        if (curl.getPathParams() == null||curl.getPathParams().isEmpty()) {
            logs.append("---- Path Param Not Exist ----").append("\n\n");
            logs.append("Test Case Skipped.");
            //request.pathParam(curl.getPathParams().toString(),true);
        } else {
            originalUrl = curl.getUrl();
            invalidPathParam = true;

            int queryIndex = originalUrl.indexOf("?");
            String baseUrl = queryIndex >= 0 ? originalUrl.substring(0, queryIndex) : originalUrl;
            String queryParams = queryIndex >= 0 ? originalUrl.substring(queryIndex) : "";

            URL url = URI.create(baseUrl).toURL();  // Use URI.create() instead of deprecated URL constructor

            String protocol = url.getProtocol();
            String host = url.getHost();
            int port = url.getPort();
            String path = url.getPath();
            String[] pathSegments = path.split("/");
            Pattern numericPattern = Pattern.compile("^\\d+$");
            Pattern uuidPattern = Pattern.compile("^[0-9a-fA-F\\-]{8,36}$");
            StringBuilder newPath = new StringBuilder();

            for (String segment : pathSegments) {
                if (segment.isEmpty()) continue; // skip empty segment (leading slash)

                if (numericPattern.matcher(segment).matches() || uuidPattern.matcher(segment).matches()) {
                    newPath.append("/").append(invalidPathParam);
                } else {
                    newPath.append("/").append(segment);
                }
            }
            String portPart = (port == -1) ? "" : ":" + port;
            modifiedUrl = protocol + "://" + host + portPart + newPath.toString() + queryParams;

            curl.getHeaders().forEach(request::header);

            if (curl.getHeaders().containsKey("Content-Type")) {
                request.contentType(curl.getHeaders().get("Content-Type").toString());
            }
            if (curl.getBody() != null) {
                request.body(curl.getBody());
            }

            logs.append("-------- " + request.log().all()).append("\n\n");

            Response response = executeRequest(request, curl.getMethod(), modifiedUrl);
            logs.append("=== Smoke Test Results ===").append("\n\n");
            logs.append("API Status Code: " + response.getStatusCode()).append("\n");
            logs.append("Response Body: " + response.body().asPrettyString()).append("\n");
            logs.append("Response Time: " + response.getTime() + "ms").append("\n");
            boolean passed = (response.getStatusCode() == 404 || response.getStatusCode() == 400);
            if (passed) {
                logs.append("Passed");
            } else {
                logs.append("Failed");
            }
        }
        return logs.toString();
    }

    // @Test
    public static String runNegativeUnsupportedMethod(String curlcommand){
        StringBuilder logs = new StringBuilder();
        configureRestAssuredEncoding();
        ParsedCurl curl = CurlParser.parseCurl(curlcommand);
        RequestSpecification request = RestAssured.given();

        curl.getHeaders().forEach(request::header);
        if (curl.getBody() != null) {
            request.body(encodeBodySafely(curl.getBody()));
        }

        // Use a method the original request did not use
        String original = curl.getMethod() == null ? "GET" : curl.getMethod().toUpperCase();
        String wrongMethod = original.equals("DELETE") ? "PUT" : "DELETE";

        Response response = executeRequest(request, wrongMethod, curl.getUrl());
        logs.append("=== Unsupported Method Test Results ===").append("\n\n");
        logs.append("- Original Method: ").append(original).append("\n");
        logs.append("- Used Method: ").append(wrongMethod).append("\n");
        logs.append("- Request Headers: "+curl.getHeaders()).append("\n");
        logs.append("API Status Code: "+response.getStatusCode()).append("\n");
        logs.append("Response Body "+response.body().asPrettyString()).append("\n");
        logs.append("Response Time "+response.getTime()).append("\n");
        logs.append("Response header "+response.getHeaders()).append("\n");
        int status = response.getStatusCode();
        if (status == 405 || status == 404 || status == 501) {
            appendVerdict(logs, true);
        } else if (is2xx(status)) {
            logs.append("Warning - API accepted unsupported HTTP method (may be intentional)");
        } else {
            appendVerdict(logs, false);
        }
        return logs.toString();
    }

    //@Test
    public static String runNegativePayloadBody(String curlCommand) {
        StringBuilder logs = new StringBuilder();
        
        // Configure RestAssured encoding
        configureRestAssuredEncoding();

        String mutatedPayload = CurlParserHelper.UpdatePayload(curlCommand);
        ParsedCurl curl = CurlParser.parseCurl(curlCommand);
        curl.setBody(mutatedPayload);

        RequestSpecification request = RestAssured.given();

        curl.getHeaders().forEach(request::header);

        if (curl.getHeaders().containsKey("Content-Type")) {
            request.contentType(curl.getHeaders().get("Content-Type").toString());
        }

        if (curl.getBody() != null) {
            // Safely encode body to prevent encoding errors
            String bodyString = encodeBodySafely(curl.getBody());
            request.body(bodyString);

            Response response = executeRequest(request, curl.getMethod(), curl.getUrl());

            logs.append("=== Invalid Payload Body Test Results ===").append("\n\n");
            logs.append("- Request Headers - " + curl.getHeaders()).append("\n");
            logs.append("- Request Method - " + curl.getMethod()).append("\n");
            logs.append("- Request URL - " + curl.getUrl()).append("\n");
            logs.append("- Request Body - " + curl.getBody()).append("\n");
            logs.append("API Status Code: " + response.getStatusCode()).append("\n");
            logs.append("Response Headers: " + response.getHeaders()).append("\n");
            logs.append("Response Body: " + response.body().asPrettyString()).append("\n");
            logs.append("Response Time: " + response.getTime() + "ms").append("\n");

            int status = response.getStatusCode();
            if (status == 400 || status == 422) {
                appendVerdict(logs, true);
            } else if (is2xx(status)) {
                logs.append("Failed - API accepted invalid/mutated payload");
            } else {
                appendVerdict(logs, false);
            }
        }
        else {
            logs.append("---- This API runs payload-free 🚀.. No Payload Exist. ---");
            logs.append("Test Case Skipped.");
        }
        return logs.toString();
    }

    // @Test - Test with invalid query parameters
    public static String runNegativeTestInvalidQueryParams(String curlCommand) {
        StringBuilder logs = new StringBuilder();
        ParsedCurl curl = CurlParser.parseCurl(curlCommand);
        RequestSpecification request = RestAssured.given();

        curl.getHeaders().forEach(request::header);
        if (curl.getHeaders().containsKey("Content-Type")) {
            request.contentType(curl.getHeaders().get("Content-Type").toString());
        }
        if (curl.getBody() != null) {
            request.body(curl.getBody());
        }

        // Add invalid query parameters and modify existing ones
        String modifiedUrl = curl.getUrl();
        if (modifiedUrl.contains("?")) {
            // URL already has query parameters - modify existing ones and add invalid ones
            modifiedUrl = CurlParserHelper.modifyExistingQueryParams(modifiedUrl);
        } else {
            // No existing query parameters - add invalid ones
            modifiedUrl += "?invalidParam=test&malformed=value";
        }

        Response response = executeRequest(request, curl.getMethod(), modifiedUrl);

        logs.append("=== Invalid Query Parameters Test Results ===").append("\n\n");
        logs.append("- Original URL: ").append(curl.getUrl()).append("\n");
        logs.append("- Modified URL: ").append(modifiedUrl).append("\n");
        logs.append("- Original Query Params: ").append(curl.getQueryParams()).append("\n");
        logs.append("- Request Method: ").append(curl.getMethod()).append("\n");
        logs.append("API Status Code: ").append(response.getStatusCode()).append("\n");
        logs.append("Response Body: ").append(response.body().asPrettyString()).append("\n");
        logs.append("Response Time: ").append(response.getTime()).append("ms").append("\n");

        int status = response.getStatusCode();
        if (isClientError(status)) {
            appendVerdict(logs, true);
        } else if (is2xx(status)) {
            // Many APIs ignore unknown/extra query params — not a hard fail
            appendVerdict(logs, "Warning - API accepted/ignored invalid query params");
        } else {
            appendVerdict(logs, false);
        }
        return logs.toString();
    }

    // @Test - Test with missing required query parameters
    public static String runNegativeTestMissingQueryParams(String curlCommand) {
        StringBuilder logs = new StringBuilder();
        ParsedCurl curl = CurlParser.parseCurl(curlCommand);

        if (curl.getQueryParams() == null || curl.getQueryParams().isEmpty()) {
            logs.append("=== Missing Query Parameters Test Results ===").append("\n\n");
            logs.append("---- No query params on original request ----").append("\n\n");
            logs.append("Test Case Skipped.");
            return logs.toString();
        }

        RequestSpecification request = RestAssured.given();

        curl.getHeaders().forEach(request::header);
        if (curl.getHeaders().containsKey("Content-Type")) {
            request.contentType(curl.getHeaders().get("Content-Type").toString());
        }
        if (curl.getBody() != null) {
            request.body(curl.getBody());
        }

        // Remove all query parameters
        String modifiedUrl = curl.getUrl();
        int queryIndex = modifiedUrl.indexOf('?');
        if (queryIndex != -1) {
            modifiedUrl = modifiedUrl.substring(0, queryIndex);
        }

        Response response = executeRequest(request, curl.getMethod(), modifiedUrl);

        logs.append("=== Missing Query Parameters Test Results ===").append("\n\n");
        logs.append("- Original URL: ").append(curl.getUrl()).append("\n");
        logs.append("- Modified URL (no query params): ").append(modifiedUrl).append("\n");
        logs.append("- Removed Query Params: ").append(curl.getQueryParams()).append("\n");
        logs.append("- Request Method: ").append(curl.getMethod()).append("\n");
        logs.append("API Status Code: ").append(response.getStatusCode()).append("\n");
        logs.append("Response Body: ").append(response.body().asPrettyString()).append("\n");
        logs.append("Response Time: ").append(response.getTime()).append("ms").append("\n");

        int status = response.getStatusCode();
        if (isClientError(status)) {
            appendVerdict(logs, true);
        } else if (is2xx(status)) {
            appendVerdict(logs, "Warning - API accepted request without query params (may be optional)");
        } else {
            appendVerdict(logs, false);
        }
        return logs.toString();
    }

    // @Test - Test with wrong content type
    public static String runNegativeTestWrongContentType(String curlCommand) {
        StringBuilder logs = new StringBuilder();
        ParsedCurl curl = CurlParser.parseCurl(curlCommand);
        RequestSpecification request = RestAssured.given();

        // Set wrong content type
        request.contentType("text/plain");
        
        // Add other headers except content-type
        curl.getHeaders().forEach((key, value) -> {
            if (!key.toLowerCase().equals("content-type")) {
                request.header(key, value);
            }
        });

        if (curl.getBody() != null) {
            request.body(curl.getBody());
        }

        Response response = executeRequest(request, curl.getMethod(), curl.getUrl());

        logs.append("=== Wrong Content-Type Test Results ===").append("\n\n");
        logs.append("- Request Headers: ").append("Content-Type: text/plain").append("\n");
        logs.append("- Request Method: ").append(curl.getMethod()).append("\n");
        logs.append("- Request URL: ").append(curl.getUrl()).append("\n");
        logs.append("API Status Code: ").append(response.getStatusCode()).append("\n");
        logs.append("Response Body: ").append(response.body().asPrettyString()).append("\n");
        logs.append("Response Time: ").append(response.getTime()).append("ms").append("\n");

        int status = response.getStatusCode();
        if (status == 400 || status == 415 || status == 422) {
            appendVerdict(logs, true);
        } else if (is2xx(status)) {
            appendVerdict(logs, "Warning - API accepted wrong Content-Type");
        } else {
            appendVerdict(logs, false);
        }
        return logs.toString();
    }

    // @Test - Test with malformed JSON payload
    public static String runNegativeTestMalformedJson(String curlCommand) {
        StringBuilder logs = new StringBuilder();
        ParsedCurl curl = CurlParser.parseCurl(curlCommand);
        RequestSpecification request = RestAssured.given();

        curl.getHeaders().forEach(request::header);
        if (curl.getHeaders().containsKey("Content-Type")) {
            request.contentType(curl.getHeaders().get("Content-Type").toString());
        }

        if (curl.getBody() != null) {
            // Create malformed JSON
            String malformedJson = curl.getBody().replace("{", "").replace("}", "");
            request.body(malformedJson);

            Response response = executeRequest(request, curl.getMethod(), curl.getUrl());

            logs.append("=== Malformed JSON Test Results ===").append("\n\n");
            logs.append("- Request Headers: ").append(curl.getHeaders()).append("\n");
            logs.append("- Request Method: ").append(curl.getMethod()).append("\n");
            logs.append("- Request URL: ").append(curl.getUrl()).append("\n");
            logs.append("- Malformed Body: ").append(malformedJson).append("\n");
            logs.append("API Status Code: ").append(response.getStatusCode()).append("\n");
            logs.append("Response Body: ").append(response.body().asPrettyString()).append("\n");
            logs.append("Response Time: ").append(response.getTime()).append("ms").append("\n");

            int status = response.getStatusCode();
            if (status == 400 || status == 422) {
                appendVerdict(logs, true);
            } else if (is2xx(status)) {
                logs.append("Failed - API accepted malformed JSON");
            } else {
                appendVerdict(logs, false);
            }
        } else {
            logs.append("---- No JSON payload to malform ----").append("\n\n");
            logs.append("Test Case Skipped.");
        }
        return logs.toString();
    }

    // @Test - Test with oversized payload
    public static String runNegativeTestOversizedPayload(String curlCommand) {
        StringBuilder logs = new StringBuilder();
        ParsedCurl curl = CurlParser.parseCurl(curlCommand);
        RequestSpecification request = RestAssured.given();

        curl.getHeaders().forEach(request::header);
        if (curl.getHeaders().containsKey("Content-Type")) {
            request.contentType(curl.getHeaders().get("Content-Type").toString());
        }

        if (curl.getBody() != null) {
            // Create oversized payload by repeating the original payload
            String oversizedPayload = curl.getBody();
            for (int i = 0; i < 100; i++) {
                oversizedPayload += curl.getBody();
            }
            request.body(oversizedPayload);

            Response response = executeRequest(request, curl.getMethod(), curl.getUrl());

            logs.append("=== Oversized Payload Test Results ===").append("\n\n");
            logs.append("- Request Headers: ").append(curl.getHeaders()).append("\n");
            logs.append("- Request Method: ").append(curl.getMethod()).append("\n");
            logs.append("- Request URL: ").append(curl.getUrl()).append("\n");
            logs.append("- Payload Size: ").append(oversizedPayload.length()).append(" characters").append("\n");
            logs.append("API Status Code: ").append(response.getStatusCode()).append("\n");
            logs.append("Response Body: ").append(response.body().asPrettyString()).append("\n");
            logs.append("Response Time: ").append(response.getTime()).append("ms").append("\n");

            int status = response.getStatusCode();
            if (status == 413 || status == 400 || status == 422) {
                appendVerdict(logs, true);
            } else if (is2xx(status)) {
                appendVerdict(logs, "Warning - API accepted oversized payload");
            } else {
                appendVerdict(logs, false);
            }
        } else {
            logs.append("---- No payload to oversize ----").append("\n\n");
            logs.append("Test Case Skipped.");
        }
        return logs.toString();
    }

    // @Test - Test with SQL injection patterns
    public static String runNegativeTestSqlInjection(String curlCommand) {
        StringBuilder logs = new StringBuilder();
        ParsedCurl curl = CurlParser.parseCurl(curlCommand);
        RequestSpecification request = RestAssured.given();

        curl.getHeaders().forEach(request::header);
        if (curl.getHeaders().containsKey("Content-Type")) {
            request.contentType(curl.getHeaders().get("Content-Type").toString());
        }

        if (curl.getBody() != null) {
            // Inject SQL payloads into values of specific keys
            String sqlInjectionPayload = SqlInjectionHelper.injectSqlIntoJsonValues(curl.getBody());

            request.body(sqlInjectionPayload);

            Response response = executeRequest(request, curl.getMethod(), curl.getUrl());

            logs.append("=== SQL Injection Test Results ===").append("\n\n");
            logs.append("- Request Headers: ").append(curl.getHeaders()).append("\n");
            logs.append("- Request Method: ").append(curl.getMethod()).append("\n");
            logs.append("- Request URL: ").append(curl.getUrl()).append("\n");
            logs.append("- Original Body: ").append(curl.getBody()).append("\n");
            logs.append("- SQL Injection Payload: ").append(sqlInjectionPayload).append("\n");
            logs.append("API Status Code: ").append(response.getStatusCode()).append("\n");
            logs.append("Response Body: ").append(response.body().asPrettyString()).append("\n");
            logs.append("Response Time: ").append(response.getTime()).append("ms").append("\n");

            int status = response.getStatusCode();
            if (status >= 500) {
                logs.append("Failed - Server error on SQL injection probe (investigate)");
            } else if (isClientError(status)) {
                appendVerdict(logs, true);
            } else if (is2xx(status)) {
                appendVerdict(logs, "Warning - API accepted SQL-like input (review validation)");
            } else {
                appendVerdict(logs, false);
            }
        } else {
            logs.append("---- No payload for SQL injection test ----").append("\n\n");
            logs.append("Test Case Skipped.");
        }
        return logs.toString();
    }


    // @Test - Test with special characters and Unicode
    public static String runNegativeTestSpecialCharacters(String curlCommand) {
        StringBuilder logs = new StringBuilder();
        ParsedCurl curl = CurlParser.parseCurl(curlCommand);
        RequestSpecification request = RestAssured.given();

        curl.getHeaders().forEach(request::header);
        if (curl.getHeaders().containsKey("Content-Type")) {
            request.contentType(curl.getHeaders().get("Content-Type").toString());
        }

        if (curl.getBody() != null) {
            // Inject special characters and Unicode
            String specialCharPayload = curl.getBody()
                .replace("\"name\"", "\"name\": \"<script>alert('XSS')</script>\"")
                .replace("\"email\"", "\"email\": \"test@example.com🚀💻\"")
                .replace("\"description\"", "\"description\": \"Special chars: !@#$%^&*()_+{}|:<>?[]\\;'\",./\"");
            
            request.body(specialCharPayload);

            Response response = executeRequest(request, curl.getMethod(), curl.getUrl());

            logs.append("=== Special Characters Test Results ===").append("\n\n");
            logs.append("- Request Headers: ").append(curl.getHeaders()).append("\n");
            logs.append("- Request Method: ").append(curl.getMethod()).append("\n");
            logs.append("- Request URL: ").append(curl.getUrl()).append("\n");
            logs.append("- Special Char Payload: ").append(specialCharPayload).append("\n");
            logs.append("API Status Code: ").append(response.getStatusCode()).append("\n");
            logs.append("Response Body: ").append(response.body().asPrettyString()).append("\n");
            logs.append("Response Time: ").append(response.getTime()).append("ms").append("\n");

            int status = response.getStatusCode();
            if (status >= 500) {
                appendVerdict(logs, false);
            } else if (is2xx(status) || isClientError(status)) {
                appendVerdict(logs, true);
            } else {
                appendVerdict(logs, false);
            }
        } else {
            logs.append("---- No payload for special characters test ----").append("\n\n");
            logs.append("Test Case Skipped.");
        }
        return logs.toString();
    }

    // @Test - Test with empty/null values
    public static String runNegativeTestEmptyValues(String curlCommand) {
        StringBuilder logs = new StringBuilder();
        ParsedCurl curl = CurlParser.parseCurl(curlCommand);
        RequestSpecification request = RestAssured.given();

        curl.getHeaders().forEach(request::header);
        if (curl.getHeaders().containsKey("Content-Type")) {
            request.contentType(curl.getHeaders().get("Content-Type").toString());
        }

        if (curl.getBody() != null) {
            // Create payload with empty/null values
            String emptyValuesPayload = curl.getBody()
                .replaceAll("\"[^\"]+\"", "\"\"")  // Replace all string values with empty strings
                .replaceAll("\\d+", "0");  // Replace all numbers with 0
            
            request.body(emptyValuesPayload);

            Response response = executeRequest(request, curl.getMethod(), curl.getUrl());

            logs.append("=== Empty Values Test Results ===").append("\n\n");
            logs.append("- Request Headers: ").append(curl.getHeaders()).append("\n");
            logs.append("- Request Method: ").append(curl.getMethod()).append("\n");
            logs.append("- Request URL: ").append(curl.getUrl()).append("\n");
            logs.append("- Empty Values Payload: ").append(emptyValuesPayload).append("\n");
            logs.append("API Status Code: ").append(response.getStatusCode()).append("\n");
            logs.append("Response Body: ").append(response.body().asPrettyString()).append("\n");
            logs.append("Response Time: ").append(response.getTime()).append("ms").append("\n");

            int status = response.getStatusCode();
            if (status == 400 || status == 422) {
                appendVerdict(logs, true);
            } else if (is2xx(status)) {
                appendVerdict(logs, "Warning - API accepted empty/null-like field values");
            } else {
                appendVerdict(logs, false);
            }
        } else {
            logs.append("---- No payload for empty values test ----").append("\n\n");
            logs.append("Test Case Skipped.");
        }
        return logs.toString();
    }
}
