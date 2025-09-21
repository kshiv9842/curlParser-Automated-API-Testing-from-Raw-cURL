package com.apiautomation;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import com.apiautomation.Utils.SqlInjectionHelper;
import com.apiautomation.Utils.CurlParserHelper;
import org.json.JSONObject;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ApiSmokeTest {
    // @Test
    public static String runBasicSmokeTest(String curlCommand) {
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
        Response response =null;
        try {
         response = switch (curl.getMethod().toUpperCase()) {
            case "GET" -> request.get(curl.getUrl());
            case "POST" -> request.post(curl.getUrl());
            case "PUT" -> request.put(curl.getUrl());
            case "DELETE" -> request.delete(curl.getUrl());
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + curl.getMethod());
        };
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
        System.out.println("Reponse URL: " + curl.getUrl());
        System.out.println("Response Method: " + curl.getMethod());
        System.out.println("API Status Code: " + response.getStatusCode());
        System.out.println("Response Time: " + response.getTime() + "ms");


        logs.append("=== Smoke Test Results ===").append("\n\n\n");
        logs.append("- Request Headers - "+curl.getHeaders()).append("\n");
        logs.append("- Request Body - "+curl.getBody()).append("\n");
        logs.append("- Request Path Param - "+curl.getPathParams()).append("\n");
        logs.append("Reponse URL: " + curl.getUrl()).append("\n");
        logs.append("Response Method: " + curl.getMethod()).append("\n");
        logs.append("API Status Code: " + response.getStatusCode()).append("\n");
        logs.append("Response Time: " + response.getTime() + "ms").append("\n");

        // Assert.assertTrue(response.getStatusCode() < 500, "Server error occurred");

        if (response.getContentType().contains("json")) {
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

        boolean passed = response.getStatusCode() == 200;
        if (passed) {
            logs.append("Passed");
        } else {
            logs.append("Failed");
        }

        //Assert.assertEquals(response.getStatusCode(),200);
        return logs.toString();
    }
    //@Test
    public static String runNegativeMissingHeaders(String curlCommand) {
        StringBuilder logs = new StringBuilder();

        ParsedCurl curl = CurlParser.parseCurl(curlCommand);
        RequestSpecification request = RestAssured.given();

        if (curl.getHeaders().containsKey("Content-Type")) {
            request.contentType(curl.getHeaders().get("Content-Type").toString());
        }
        List<String> headerkeys = new ArrayList<>(curl.getHeaders().keySet());
        for (int i = 0; i < headerkeys.size(); i++) {
            curl.getHeaders().put(headerkeys.get(i), null);
        }
        Response response = null;
        try{
        response = switch (curl.getMethod().toUpperCase()) {
            case "GET" -> request.get(curl.getUrl());
            case "POST" -> request.get(curl.getUrl());
            case "PUT" -> request.get(curl.getUrl());
            case "DELETE" -> request.get(curl.getUrl());
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + curl.getMethod());
        };
    }
    catch(Exception e){
         System.err.println("API/network error: " + e.getMessage());
        logs.append("API/network error: ").append(e.getMessage()).append("\n");
        // Optionally, add stack trace
        java.io.StringWriter sw = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        logs.append(sw.toString()).append("\n");
    }

        logs.append("=== Smoke Test Results ===").append("\n\n");
        logs.append("- Request Headers - " + curl.getHeaders()).append("\n");
        logs.append("- Request Method - " + curl.getMethod()).append("\n");
        logs.append("- Request URL - " + curl.getUrl()).append("\n");
        logs.append("- Request Body - " + curl.getBody()).append("\n");
        logs.append("API Status Code: " + response.getStatusCode()).append("\n");
        logs.append("Response Headers: " + response.getHeaders()).append("\n");
        logs.append("Response Body: " + response.body().asPrettyString()).append("\n");
        logs.append("Response Time: " + response.getTime() + "ms").append("\n\n");
        //Assert.assertTrue(statusCode == 400 || statusCode == 404 || statusCode == 401);
        boolean passed = (response.getStatusCode() == 400 ||response.getStatusCode() == 401);
        if (passed) {
            logs.append("Passed");
        } else {
            logs.append("Failed");
        }
        return logs.toString();
    }

    // @Test
    public static String runNegativeTestRemoveAuth(String curlCommand){
        StringBuilder logs = new StringBuilder();

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

            Response response = switch (curl.getMethod().toUpperCase()) {
                case "GET" -> request.get(curl.getUrl());
                case "POST" -> request.post(curl.getUrl());
                case "PUT" -> request.put(curl.getUrl());
                case "DELETE" -> request.delete(curl.getUrl());
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + curl.getMethod());
            };

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

        ParsedCurl curl = CurlParser.parseCurl(curlCommand);
        RequestSpecification request = RestAssured.given();

        curl.getHeaders().forEach(request::header);
        if (curl.getHeaders().containsKey("Content-Type")) {
            request.contentType(curl.getHeaders().get("Content-Type").toString());
        }
        if (curl.getBody() != null) {
            request.body("Test data");
            logs.append("---- Body Removed Successfully. ----").append("\n");

            Response response = switch (curl.getMethod().toUpperCase()) {
                case "GET" -> request.get(curl.getUrl());
                case "POST" -> request.post(curl.getUrl());
                case "PUT" -> request.put(curl.getUrl());
                case "DELETE" -> request.delete(curl.getUrl());
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + curl.getMethod());
            };

            logs.append("=== Smoke Test Results ===").append("\n\n");
            logs.append("- Request Headers - "+curl.getHeaders()).append("\n");
            logs.append("- Request Body - "+curl.getBody()).append("\n");
            logs.append("API Status Code: " + response.getStatusCode()).append("\n");
            logs.append("Response Body: " + response.body().asPrettyString()).append("\n");
            logs.append("Response Time: " + response.getTime() + "ms").append("\n");
            // Assert.assertTrue(statusCode==400 ||statusCode==404||statusCode==401);
            boolean passed = (response.getStatusCode() == 400 ||response.getStatusCode() == 422);
            if (passed) {
                logs.append("Passed");
            } else {
                logs.append("Failed");
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

            Response response = switch (curl.getMethod().toUpperCase()) {
                case "GET" -> request.get(modifiedUrl);
                case "POST" -> request.post(modifiedUrl);
                case "PUT" -> request.put(modifiedUrl);
                case "DELETE" -> request.delete(modifiedUrl);
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + curl.getMethod());
            };
            logs.append("=== Smoke Test Results ===").append("\n\n");
            logs.append("API Status Code: " + response.getStatusCode()).append("\n");
            logs.append("Response Body: " + response.body().asPrettyString()).append("\n");
            logs.append("Response Time: " + response.getTime() + "ms").append("\n");
            boolean passed = (response.getStatusCode() == 404);
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
        ParsedCurl curl = CurlParser.parseCurl(curlcommand);
        RequestSpecification request = RestAssured.given();

        curl.getHeaders().forEach(request::header);
        if(curl.getMethod()!=null){
            if(curl.getMethod().equals("GET"))
                curl.setMethod("POST");
            else
                curl.setMethod("GET");
        }
        if(curl.getBody()!=null)
            request.body(curl.getBody());

        Response response = switch (curl.getMethod().toUpperCase()){
            case "GET" -> request.delete(curl.getUrl());
            case "POST" ->request.delete(curl.getUrl());
            case "PUT" -> request.delete(curl.getUrl());
            case "DELETE" -> request.put(curl.getUrl());
            default -> throw new IllegalArgumentException("Unsupported Method: "+curl.getMethod());
        };
        logs.append("=== Smoke Test Results ====").append("\n\n");
        logs.append("- Request Headers: "+curl.getHeaders()).append("\n");
        logs.append("API Status Code: "+response.getStatusCode()).append("\n");
        logs.append("Response Body "+response.body().asPrettyString()).append("\n");
        logs.append("Response Time "+response.getTime()).append("\n");
        logs.append("Response header "+response.getHeaders()).append("\n");
        boolean passed = (response.getStatusCode() == 405);
        if (passed) {
            logs.append("Passed");
        } else {
            logs.append("Failed");
        }
        return logs.toString();
    }

    //@Test
    public static String runNegativePayloadBody(String curlCommand) {
        StringBuilder logs = new StringBuilder();

        String mutatedPayload = CurlParserHelper.UpdatePayload(curlCommand);
        ParsedCurl curl = CurlParser.parseCurl(curlCommand);
        curl.setBody(mutatedPayload);

        RequestSpecification request = RestAssured.given();

        curl.getHeaders().forEach(request::header);

        if (curl.getHeaders().containsKey("Content-Type")) {
            request.contentType(curl.getHeaders().get("Content-Type").toString());
        }

        if (curl.getBody() != null) {
            request.body(curl.getBody());

            Response response = switch (curl.getMethod().toUpperCase()) {
                case "GET" -> request.get(curl.getUrl());
                case "POST" -> request.get(curl.getUrl());
                case "PUT" -> request.get(curl.getUrl());
                case "DELETE" -> request.get(curl.getUrl());
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + curl.getMethod());
            };

            logs.append("=== Smoke Test Results ===").append("\n\n");
            logs.append("- Request Headers - " + curl.getHeaders()).append("\n");
            logs.append("- Request Method - " + curl.getMethod()).append("\n");
            logs.append("- Request URL - " + curl.getUrl()).append("\n");
            logs.append("- Request Body - " + curl.getBody()).append("\n");
            logs.append("API Status Code: " + response.getStatusCode()).append("\n");
            logs.append("Response Headers: " + response.getHeaders()).append("\n");
            logs.append("Response Body: " + response.body().asPrettyString()).append("\n");
            logs.append("Response Time: " + response.getTime() + "ms").append("\n");

            boolean passed = (response.getStatusCode() == 400 || response.getStatusCode() == 422);
            if (passed) {
                logs.append("Passed");
            } else {
                logs.append("Failed");
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

        Response response = switch (curl.getMethod().toUpperCase()) {
            case "GET" -> request.get(modifiedUrl);
            case "POST" -> request.post(modifiedUrl);
            case "PUT" -> request.put(modifiedUrl);
            case "DELETE" -> request.delete(modifiedUrl);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + curl.getMethod());
        };

        logs.append("=== Invalid Query Parameters Test Results ===").append("\n\n");
        logs.append("- Original URL: ").append(curl.getUrl()).append("\n");
        logs.append("- Modified URL: ").append(modifiedUrl).append("\n");
        logs.append("- Original Query Params: ").append(curl.getQueryParams()).append("\n");
        logs.append("- Request Method: ").append(curl.getMethod()).append("\n");
        logs.append("API Status Code: ").append(response.getStatusCode()).append("\n");
        logs.append("Response Body: ").append(response.body().asPrettyString()).append("\n");
        logs.append("Response Time: ").append(response.getTime()).append("ms").append("\n");

        boolean passed = (response.getStatusCode() == 400 || response.getStatusCode() == 422);
        if (passed) {
            logs.append("Passed");
        } else {
            logs.append("Failed");
        }
        return logs.toString();
    }

    // @Test - Test with missing required query parameters
    public static String runNegativeTestMissingQueryParams(String curlCommand) {
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

        // Remove all query parameters
        String modifiedUrl = curl.getUrl();
        int queryIndex = modifiedUrl.indexOf('?');
        if (queryIndex != -1) {
            modifiedUrl = modifiedUrl.substring(0, queryIndex);
        }

        Response response = switch (curl.getMethod().toUpperCase()) {
            case "GET" -> request.get(modifiedUrl);
            case "POST" -> request.post(modifiedUrl);
            case "PUT" -> request.put(modifiedUrl);
            case "DELETE" -> request.delete(modifiedUrl);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + curl.getMethod());
        };

        logs.append("=== Missing Query Parameters Test Results ===").append("\n\n");
        logs.append("- Original URL: ").append(curl.getUrl()).append("\n");
        logs.append("- Modified URL (no query params): ").append(modifiedUrl).append("\n");
        logs.append("- Removed Query Params: ").append(curl.getQueryParams()).append("\n");
        logs.append("- Request Method: ").append(curl.getMethod()).append("\n");
        logs.append("API Status Code: ").append(response.getStatusCode()).append("\n");
        logs.append("Response Body: ").append(response.body().asPrettyString()).append("\n");
        logs.append("Response Time: ").append(response.getTime()).append("ms").append("\n");

        boolean passed = (response.getStatusCode() == 400 || response.getStatusCode() == 422);
        if (passed) {
            logs.append("Passed");
        } else {
            logs.append("Failed");
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

        Response response = switch (curl.getMethod().toUpperCase()) {
            case "GET" -> request.get(curl.getUrl());
            case "POST" -> request.post(curl.getUrl());
            case "PUT" -> request.put(curl.getUrl());
            case "DELETE" -> request.delete(curl.getUrl());
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + curl.getMethod());
        };

        logs.append("=== Wrong Content-Type Test Results ===").append("\n\n");
        logs.append("- Request Headers: ").append("Content-Type: text/plain").append("\n");
        logs.append("- Request Method: ").append(curl.getMethod()).append("\n");
        logs.append("- Request URL: ").append(curl.getUrl()).append("\n");
        logs.append("API Status Code: ").append(response.getStatusCode()).append("\n");
        logs.append("Response Body: ").append(response.body().asPrettyString()).append("\n");
        logs.append("Response Time: ").append(response.getTime()).append("ms").append("\n");

        boolean passed = (response.getStatusCode() == 400 || response.getStatusCode() == 415);
        if (passed) {
            logs.append("Passed");
        } else {
            logs.append("Failed");
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

            Response response = switch (curl.getMethod().toUpperCase()) {
                case "GET" -> request.get(curl.getUrl());
                case "POST" -> request.post(curl.getUrl());
                case "PUT" -> request.put(curl.getUrl());
                case "DELETE" -> request.delete(curl.getUrl());
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + curl.getMethod());
            };

            logs.append("=== Malformed JSON Test Results ===").append("\n\n");
            logs.append("- Request Headers: ").append(curl.getHeaders()).append("\n");
            logs.append("- Request Method: ").append(curl.getMethod()).append("\n");
            logs.append("- Request URL: ").append(curl.getUrl()).append("\n");
            logs.append("- Malformed Body: ").append(malformedJson).append("\n");
            logs.append("API Status Code: ").append(response.getStatusCode()).append("\n");
            logs.append("Response Body: ").append(response.body().asPrettyString()).append("\n");
            logs.append("Response Time: ").append(response.getTime()).append("ms").append("\n");

            boolean passed = (response.getStatusCode() == 400 || response.getStatusCode() == 422);
            if (passed) {
                logs.append("Passed");
            } else {
                logs.append("Failed");
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

            Response response = switch (curl.getMethod().toUpperCase()) {
                case "GET" -> request.get(curl.getUrl());
                case "POST" -> request.post(curl.getUrl());
                case "PUT" -> request.put(curl.getUrl());
                case "DELETE" -> request.delete(curl.getUrl());
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + curl.getMethod());
            };

            logs.append("=== Oversized Payload Test Results ===").append("\n\n");
            logs.append("- Request Headers: ").append(curl.getHeaders()).append("\n");
            logs.append("- Request Method: ").append(curl.getMethod()).append("\n");
            logs.append("- Request URL: ").append(curl.getUrl()).append("\n");
            logs.append("- Payload Size: ").append(oversizedPayload).append(" characters").append("\n");
            logs.append("- Payload Size: ").append(oversizedPayload.length()).append(" characters").append("\n");
            logs.append("API Status Code: ").append(response.getStatusCode()).append("\n");
            logs.append("Response Body: ").append(response.body().asPrettyString()).append("\n");
            logs.append("Response Time: ").append(response.getTime()).append("ms").append("\n");

            boolean passed = (response.getStatusCode() == 413 || response.getStatusCode() == 400);
            if (passed) {
                logs.append("Passed");
            } else {
                logs.append("Failed");
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

            Response response = switch (curl.getMethod().toUpperCase()) {
                case "GET" -> request.get(curl.getUrl());
                case "POST" -> request.post(curl.getUrl());
                case "PUT" -> request.put(curl.getUrl());
                case "DELETE" -> request.delete(curl.getUrl());
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + curl.getMethod());
            };

            logs.append("=== SQL Injection Test Results ===").append("\n\n");
            logs.append("- Request Headers: ").append(curl.getHeaders()).append("\n");
            logs.append("- Request Method: ").append(curl.getMethod()).append("\n");
            logs.append("- Request URL: ").append(curl.getUrl()).append("\n");
            logs.append("- Original Body: ").append(curl.getBody()).append("\n");
            logs.append("- SQL Injection Payload: ").append(sqlInjectionPayload).append("\n");
            logs.append("API Status Code: ").append(response.getStatusCode()).append("\n");
            logs.append("Response Body: ").append(response.body().asPrettyString()).append("\n");
            logs.append("Response Time: ").append(response.getTime()).append("ms").append("\n");

            boolean passed = (response.getStatusCode() == 400 || response.getStatusCode() == 422);
            if (passed) {
                logs.append("Passed");
            } else {
                logs.append("Failed");
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

            Response response = switch (curl.getMethod().toUpperCase()) {
                case "GET" -> request.get(curl.getUrl());
                case "POST" -> request.post(curl.getUrl());
                case "PUT" -> request.put(curl.getUrl());
                case "DELETE" -> request.delete(curl.getUrl());
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + curl.getMethod());
            };

            logs.append("=== Special Characters Test Results ===").append("\n\n");
            logs.append("- Request Headers: ").append(curl.getHeaders()).append("\n");
            logs.append("- Request Method: ").append(curl.getMethod()).append("\n");
            logs.append("- Request URL: ").append(curl.getUrl()).append("\n");
            logs.append("- Special Char Payload: ").append(specialCharPayload).append("\n");
            logs.append("API Status Code: ").append(response.getStatusCode()).append("\n");
            logs.append("Response Body: ").append(response.body().asPrettyString()).append("\n");
            logs.append("Response Time: ").append(response.getTime()).append("ms").append("\n");

            boolean passed = (response.getStatusCode() == 200 || response.getStatusCode() == 400);
            if (passed) {
                logs.append("Passed");
            } else {
                logs.append("Failed");
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

            Response response = switch (curl.getMethod().toUpperCase()) {
                case "GET" -> request.get(curl.getUrl());
                case "POST" -> request.post(curl.getUrl());
                case "PUT" -> request.put(curl.getUrl());
                case "DELETE" -> request.delete(curl.getUrl());
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + curl.getMethod());
            };

            logs.append("=== Empty Values Test Results ===").append("\n\n");
            logs.append("- Request Headers: ").append(curl.getHeaders()).append("\n");
            logs.append("- Request Method: ").append(curl.getMethod()).append("\n");
            logs.append("- Request URL: ").append(curl.getUrl()).append("\n");
            logs.append("- Empty Values Payload: ").append(emptyValuesPayload).append("\n");
            logs.append("API Status Code: ").append(response.getStatusCode()).append("\n");
            logs.append("Response Body: ").append(response.body().asPrettyString()).append("\n");
            logs.append("Response Time: ").append(response.getTime()).append("ms").append("\n");

            boolean passed = (response.getStatusCode() == 400 || response.getStatusCode() == 422);
            if (passed) {
                logs.append("Passed");
            } else {
                logs.append("Failed");
            }
        } else {
            logs.append("---- No payload for empty values test ----").append("\n\n");
            logs.append("Test Case Skipped.");
        }
        return logs.toString();
    }
}
