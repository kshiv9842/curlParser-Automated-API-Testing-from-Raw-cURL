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
        logs.append("=== Running Basic Smoke Test ===\n\n");
        logs.append(ApiSmokeTest.runBasicSmokeTest(curlCommand)).append("\n\n");

        logs.append("=== Running Negative Payload Body Test ===\n\n");
        logs.append(ApiSmokeTest.runNegativePayloadBody(curlCommand)).append("\n\n");

        logs.append("=== Running Negative Test - Missing Headers Test ===\n\n");
        logs.append(ApiSmokeTest.runNegativeMissingHeaders(curlCommand)).append("\n\n");

        logs.append("=== Running Negative Test - Remove Auth Test ===\n\n");
        logs.append(ApiSmokeTest.runNegativeTestRemoveAuth(curlCommand)).append("\n\n");

        logs.append("=== Running Negative Test - Change Method Test ===\n\n");
        logs.append(ApiSmokeTest.runNegativeUnsupportedMethod(curlCommand)).append("\n\n");

        logs.append("=== Running Negative Test - Remove Body from the Payload Test ===\n\n");
        logs.append(ApiSmokeTest.runNegativeTestRemoveBody(curlCommand)).append("\n\n");

        logs.append("=== Running Negative Test - Update PathParams Test ===\n\n");
        logs.append(ApiSmokeTest.runNegativeTestUpdatePathParam(curlCommand)).append("\n\n");

        return logs.toString();
    }
}

