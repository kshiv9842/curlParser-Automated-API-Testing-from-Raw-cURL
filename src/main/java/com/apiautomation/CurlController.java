package com.apiautomation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.net.MalformedURLException;

@RestController
@RequestMapping("/api")
public class CurlController {
    @PostMapping(value = "/execute-curl", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String executeCurl(@RequestBody String curlCommand) throws MalformedURLException {
        StringBuilder logs = new StringBuilder();
        logs.append("=== Running Basic Smoke Test (Verifies API is working with valid request) ===\n")
                .append("Expected: API should return 200 (Success)\n\n");
        logs.append(ApiSmokeTest.runBasicSmokeTest(curlCommand)).append("\n\n");

        logs.append("=== Running Negative Test: Invalid Payload Body (Checks API response to incorrect payload) ===\n")
                .append("Expected: API should return 400 or 422 (Bad Request / Unprocessable Entity)\n\n");
        logs.append(ApiSmokeTest.runNegativePayloadBody(curlCommand)).append("\n\n");

        logs.append("=== Running Negative Test: Missing Headers (Validates error handling when required headers are absent) ===\n")
                .append("Expected: API should return 400 or 401 (Bad Request / Unauthorized)\n\n");
        logs.append(ApiSmokeTest.runNegativeMissingHeaders(curlCommand)).append("\n\n");

        logs.append("=== Running Negative Test: Missing Authentication (Ensures API blocks unauthorized requests) ===\n")
                .append("Expected: API should return 401 or 403 (Unauthorized / Forbidden)\n\n");
        logs.append(ApiSmokeTest.runNegativeTestRemoveAuth(curlCommand)).append("\n\n");

        logs.append("=== Running Negative Test: Unsupported HTTP Method (Checks API behavior when using wrong method) ===\n")
                .append("Expected: API should return 405 (Method Not Allowed)\n\n");
        logs.append(ApiSmokeTest.runNegativeUnsupportedMethod(curlCommand)).append("\n\n");

        logs.append("=== Running Negative Test: Missing Request Body (Verifies API response when body is removed) ===\n")
                .append("Expected: API should return 400 or 422 (Bad Request / Unprocessable Entity)\n\n");
        logs.append(ApiSmokeTest.runNegativeTestRemoveBody(curlCommand)).append("\n\n");

        logs.append("=== Running Negative Test: Invalid Path Parameters (Tests API response to incorrect path params) ===\n")
                .append("Expected: API should return 404 (Not Found)\n\n");
        logs.append(ApiSmokeTest.runNegativeTestUpdatePathParam(curlCommand)).append("\n\n");

        return logs.toString();
    }
}
