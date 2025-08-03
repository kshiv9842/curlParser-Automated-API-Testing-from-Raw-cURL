package com.apiautomation;

import com.apiautomation.Utils.DataReader;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ApiSmokeTest {
    @Test
    public void runBasicSmokeTest() {

        String curlCommand = DataReader.readDataReader().getCurl().toString();

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

        System.out.println("=== Smoke Test Results ===");
        System.out.println("- Request Headers - "+curl.getHeaders());
        System.out.println("- Request Body - "+curl.getBody());
        System.out.println("- Request Path Param - "+curl.getPathParams());
        System.out.println("Reponse URL: " + curl.getUrl());
        System.out.println("Response Method: " + curl.getMethod());
        System.out.println("API Status Code: " + response.getStatusCode());
        System.out.println("Response Time: " + response.getTime() + "ms");

        Assert.assertTrue(response.getStatusCode() < 500, "Server error occurred");

        if (response.getContentType().contains("json")) {
            try {
                new JSONObject(response.getBody().asString());
                System.out.println("Response Body Format: Valid JSON");
            } catch (Exception e) {
                System.out.println("Response Body Format: Invalid JSON");
                Assert.fail("Invalid JSON response");
            }
        }
        System.out.println("Response Body:");
        System.out.println(response.getBody().asPrettyString());
        Assert.assertEquals(response.getStatusCode(),200);

    }
    @Test
    public void runNegativeMissingHeaders() {
        String curlCommand = DataReader.readDataReader().getCurl();

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

        System.out.println("=== Smoke Test Results ===");
        System.out.println("- Request Headers - " + curl.getHeaders());
        System.out.println("- Request Method - " + curl.getMethod());
        System.out.println("- Request URL - " + curl.getUrl());
        System.out.println("- Request Body - " + curl.getBody());
        System.out.println("API Status Code: " + response.getStatusCode());
        System.out.println("Response Headers: " + response.getHeaders());
        System.out.println("Response Body: " + response.body().asPrettyString());
        System.out.println("Response Time: " + response.getTime() + "ms");
        int statusCode = response.getStatusCode();
        Assert.assertTrue(statusCode == 400 || statusCode == 404 || statusCode == 401);
    }

    @Test
    public void runNegativeTestRemoveAuth(){
        String curlCommand = DataReader.readDataReader().getCurl().toString();

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

            System.out.println("=== Smoke Test Results ===");
            System.out.println("- Request Headers - "+curl.getHeaders());
            System.out.println("- Request Body - "+curl.getBody());
            System.out.println("API Status Code: " + response.getStatusCode());
            System.out.println("Response Body: " + response.body().asPrettyString());
            System.out.println("Response Time: " + response.getTime() + "ms");
            int statusCode = response.getStatusCode();
            Assert.assertTrue(statusCode == 400 || statusCode == 404 || statusCode == 401);
        }
        else {
            System.out.println("---- Auth not Exist. ----");
        }
    }

    @Test
    public void runNegativeTestRemoveBody() {
        String curlCommand = DataReader.readDataReader().getCurl().toString();

        ParsedCurl curl = CurlParser.parseCurl(curlCommand);
        RequestSpecification request = RestAssured.given();

        curl.getHeaders().forEach(request::header);
        if (curl.getHeaders().containsKey("Content-Type")) {
            request.contentType(curl.getHeaders().get("Content-Type").toString());
        }
        if (curl.getBody() != null) {
            request.body("Test data");
            System.out.println("---- Body Removed Successfully. ----");

            Response response = switch (curl.getMethod().toUpperCase()) {
                case "GET" -> request.get(curl.getUrl());
                case "POST" -> request.post(curl.getUrl());
                case "PUT" -> request.put(curl.getUrl());
                case "DELETE" -> request.delete(curl.getUrl());
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + curl.getMethod());
            };

            System.out.println("=== Smoke Test Results ===");
            System.out.println("- Request Headers - "+curl.getHeaders());
            System.out.println("- Request Body - "+curl.getBody());
            System.out.println("API Status Code: " + response.getStatusCode());
            System.out.println("Response Body: " + response.body().asPrettyString());
            System.out.println("Response Time: " + response.getTime() + "ms");
            int statusCode = response.getStatusCode();
            Assert.assertTrue(statusCode==400 ||statusCode==404||statusCode==401);
        }
        else {
            System.out.println("---- API don't have body. ----");
        }
    }

    @Test
    public void runNegativeTestUpdatePathParam() throws MalformedURLException {
        String curlCommand = DataReader.readDataReader().getCurl().toString();
        ParsedCurl curl = CurlParser.parseCurl(curlCommand);
        RequestSpecification request = RestAssured.given();

        String originalUrl = "";
        Object invalidPathParam;
        String modifiedUrl = "";

        System.out.println("Path Param List - " + curl.getPathParams());

        if (curl.getPathParams() == null||curl.getPathParams().isEmpty()) {
            System.out.println("---- Path Param Not Exist ----");
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

            System.out.println("-------- " + request.log().all());

            Response response = switch (curl.getMethod().toUpperCase()) {
                case "GET" -> request.get(modifiedUrl);
                case "POST" -> request.post(modifiedUrl);
                case "PUT" -> request.put(modifiedUrl);
                case "DELETE" -> request.delete(modifiedUrl);
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + curl.getMethod());
            };
            System.out.println("=== Smoke Test Results ===");
            System.out.println("API Status Code: " + response.getStatusCode());
            System.out.println("Response Body: " + response.body().asPrettyString());
            System.out.println("Response Time: " + response.getTime() + "ms");
            int statusCode = response.getStatusCode();
            Assert.assertTrue(statusCode==400 ||statusCode==404||statusCode==401);
        }
    }

    @Test
    public void runNegativeUnsupportedMethod(){
        String curlcommand = DataReader.readDataReader().getCurl();
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

        System.out.println("=== Smoke Test Results ====");
        System.out.println("- Request Headers: "+curl.getHeaders());
        System.out.println("Response Status Code "+response.getStatusCode());
        System.out.println("Response Body "+response.body().asPrettyString());
        System.out.println("Response Time "+response.getTime());
        System.out.println("Response header "+response.getHeaders());
        int statusCode = response.getStatusCode();
        Assert.assertTrue(statusCode==400 ||statusCode==404||statusCode==401);
    }
}
