package com.apiautomation;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import java.net.MalformedURLException;
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
        Response response = switch (curl.getMethod().toUpperCase()) {
            case "GET" -> request.get(curl.getUrl());
            case "POST" -> request.post(curl.getUrl());
            case "PUT" -> request.put(curl.getUrl());
            case "DELETE" -> request.delete(curl.getUrl());
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + curl.getMethod());
        };

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
        logs.append("Response Time: " + response.getTime() + "ms").append("\n\n");
        //Assert.assertTrue(statusCode == 400 || statusCode == 404 || statusCode == 401);
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
        }
        else {
            logs.append("---- Auth not Exist. ----").append("\n\n");
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
        }
        else {
            logs.append("---- API don't have body. ----").append("\n\n");
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
            //request.pathParam(curl.getPathParams().toString(),true);
        } else {
            originalUrl = curl.getUrl();
            invalidPathParam = true;

            int queryIndex = originalUrl.indexOf("?");
            String baseUrl = queryIndex >= 0 ? originalUrl.substring(0, queryIndex) : originalUrl;
            String queryParams = queryIndex >= 0 ? originalUrl.substring(queryIndex) : "";

            URL url = new URL(baseUrl);  // Use java.net.URL to parse URL parts

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
            //Assert.assertTrue(statusCode==400 ||statusCode==404||statusCode==401);
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
        logs.append("Response Status Code "+response.getStatusCode()).append("\n");
        logs.append("Response Body "+response.body().asPrettyString()).append("\n");
        logs.append("Response Time "+response.getTime()).append("\n");
        logs.append("Response header "+response.getHeaders()).append("\n");
       // Assert.assertTrue(statusCode==400 ||statusCode==404||statusCode==401);
        return logs.toString();
    }

    //@Test
    public static String runNegativePayloadBody(String curlCommand) {
        StringBuilder logs = new StringBuilder();

        String mutatedPayload = CurlParser.UpdatePayload(curlCommand);
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
        }
       else {
            logs.append("---- This API runs payload-free 🚀.. No Payload Exist. ---");
        }
       //Assert.assertTrue(statusCode == 400 || statusCode == 404 || statusCode == 401 || statusCode == 403);
        return logs.toString();
    }
}
