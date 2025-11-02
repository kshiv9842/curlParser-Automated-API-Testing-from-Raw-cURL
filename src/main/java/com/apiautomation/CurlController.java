package com.apiautomation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.net.MalformedURLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CurlController {
    @PostMapping(value = "/execute-curl", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String executeCurl(@RequestBody String curlCommand) throws MalformedURLException {
        // Comprehensive curl command validation
        String curlValidationResult = validateCurlCommand(curlCommand);
        if (curlValidationResult != null) {
            return "CURL VALIDATION ERROR: " + curlValidationResult;
        }
        
        // Enhanced validation for single curl command
        String validationResult = validateSingleCurlCommand(curlCommand);
        if (validationResult != null) {
            return "ERROR: " + validationResult;
        }
        
        // Basic validation to ensure only single curl command
        if (isMultipleCurlCommands(curlCommand)) {
            return "ERROR: Multiple curl commands detected. This endpoint only accepts single curl commands.";
        }
        
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

        logs.append("=== Running Negative Test: Invalid Query Parameters (Tests API response to malformed query strings) ===\n")
                .append("Expected: API should return 400 or 422 (Bad Request / Unprocessable Entity)\n\n");
        logs.append(ApiSmokeTest.runNegativeTestInvalidQueryParams(curlCommand)).append("\n\n");

        logs.append("=== Running Negative Test: Missing Query Parameters (Tests API response when required query params are removed) ===\n")
                .append("Expected: API should return 400 or 422 (Bad Request / Unprocessable Entity)\n\n");
        logs.append(ApiSmokeTest.runNegativeTestMissingQueryParams(curlCommand)).append("\n\n");

        logs.append("=== Running Negative Test: Wrong Content-Type (Tests API response to incorrect media type) ===\n")
                .append("Expected: API should return 400 or 415 (Bad Request / Unsupported Media Type)\n\n");
        logs.append(ApiSmokeTest.runNegativeTestWrongContentType(curlCommand)).append("\n\n");

        logs.append("=== Running Negative Test: Malformed JSON (Tests API response to invalid JSON syntax) ===\n")
                .append("Expected: API should return 400 or 422 (Bad Request / Unprocessable Entity)\n\n");
        logs.append(ApiSmokeTest.runNegativeTestMalformedJson(curlCommand)).append("\n\n");

        logs.append("=== Running Negative Test: Oversized Payload (Tests API response to extremely large payloads) ===\n")
                .append("Expected: API should return 413 or 400 (Payload Too Large / Bad Request)\n\n");
        logs.append(ApiSmokeTest.runNegativeTestOversizedPayload(curlCommand)).append("\n\n");

        logs.append("=== Running Security Test: SQL Injection (Tests API response to SQL injection patterns) ===\n")
                .append("Expected: API should return 400 or 422 (Bad Request / Unprocessable Entity)\n\n");
        logs.append(ApiSmokeTest.runNegativeTestSqlInjection(curlCommand)).append("\n\n");

        logs.append("=== Running Edge Case Test: Special Characters (Tests API response to Unicode and special chars) ===\n")
                .append("Expected: API should return 200 or 400 (Success or Bad Request)\n\n");
        logs.append(ApiSmokeTest.runNegativeTestSpecialCharacters(curlCommand)).append("\n\n");

        logs.append("=== Running Edge Case Test: Empty Values (Tests API response to empty/null field values) ===\n")
                .append("Expected: API should return 400 or 422 (Bad Request / Unprocessable Entity)\n\n");
        logs.append(ApiSmokeTest.runNegativeTestEmptyValues(curlCommand)).append("\n\n");

        return logs.toString();
    }
    
    /**
     * Execute multiple cURL commands with comprehensive testing
     */
    @PostMapping(value = "/execute-multi-curl", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String executeMultiCurl(@RequestBody Map<String, List<String>> request) throws MalformedURLException {
        List<String> curlCommands = request.get("curlCommands");
        StringBuilder logs = new StringBuilder();
        long startTime = System.currentTimeMillis();
        
        // Validate all curl commands first
        String validationResult = validateMultipleCurlCommands(curlCommands);
        if (validationResult != null) {
            return "VALIDATION FAILED:\n" + validationResult + 
                   "\nPlease fix the validation errors and try again.";
        }
        
        // Parse and normalize cURL commands (handle line breaks)
        List<String> normalizedCommands = parseAndNormalizeCurlCommands(curlCommands);
        
        logs.append("=== Multi-cURL Execution Started ===\n");
        logs.append("Original Commands: ").append(curlCommands.size()).append("\n");
        logs.append("Normalized Commands: ").append(normalizedCommands.size()).append("\n");
        logs.append("Execution Mode: Sequential (with all test scenarios)\n\n");
        
        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;
        
        for (int i = 0; i < normalizedCommands.size(); i++) {
            String curlCommand = normalizedCommands.get(i);
            logs.append("=== API ").append(i + 1).append(": ").append(extractApiName(curlCommand)).append(" ===\n");
            logs.append("Processing cURL command: ").append(curlCommand.substring(0, Math.min(curlCommand.length(), 100))).append("...\n");
            
            try {
                // Run all test scenarios for this cURL command
                String result = executeAllTestsForCurl(curlCommand);
                logs.append(result).append("\n\n");
                
                // Count results (simple parsing)
                if (result.contains("Passed")) {
                    passedTests++;
                } else {
                    failedTests++;
                }
                totalTests++;
                
            } catch (Exception e) {
                logs.append("ERROR executing API ").append(i + 1).append(": ").append(e.getMessage()).append("\n");
                logs.append("Stack trace: ").append(e.getClass().getSimpleName()).append("\n\n");
                failedTests++;
                totalTests++;
            }
        }
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        logs.append("=== Multi-cURL Execution Summary ===\n");
        logs.append("Total APIs: ").append(normalizedCommands.size()).append("\n");
        logs.append("Total Tests: ").append(totalTests).append("\n");
        logs.append("Passed: ").append(passedTests).append("\n");
        logs.append("Failed: ").append(failedTests).append("\n");
        logs.append("Execution Time: ").append(executionTime).append("ms\n");
        logs.append("Average Time per API: ").append(executionTime / curlCommands.size()).append("ms\n");
        
        return logs.toString();
    }
    
    
    /**
     * Execute all test scenarios for a single cURL command
     */
    private String executeAllTestsForCurl(String curlCommand) throws MalformedURLException {
        StringBuilder logs = new StringBuilder();
        
        logs.append("=== Running Basic Smoke Test ===\n");
        logs.append(ApiSmokeTest.runBasicSmokeTest(curlCommand)).append("\n\n");
        
        logs.append("=== Running Negative Test: Invalid Payload Body ===\n");
        logs.append(ApiSmokeTest.runNegativePayloadBody(curlCommand)).append("\n\n");
        
        logs.append("=== Running Negative Test: Missing Headers ===\n");
        logs.append(ApiSmokeTest.runNegativeMissingHeaders(curlCommand)).append("\n\n");
        
        logs.append("=== Running Negative Test: Missing Authentication ===\n");
        logs.append(ApiSmokeTest.runNegativeTestRemoveAuth(curlCommand)).append("\n\n");
        
        logs.append("=== Running Negative Test: Unsupported HTTP Method ===\n");
        logs.append(ApiSmokeTest.runNegativeUnsupportedMethod(curlCommand)).append("\n\n");
        
        logs.append("=== Running Negative Test: Missing Request Body ===\n");
        logs.append(ApiSmokeTest.runNegativeTestRemoveBody(curlCommand)).append("\n\n");
        
        logs.append("=== Running Negative Test: Invalid Path Parameters ===\n");
        logs.append(ApiSmokeTest.runNegativeTestUpdatePathParam(curlCommand)).append("\n\n");
        
        logs.append("=== Running Negative Test: Invalid Query Parameters ===\n");
        logs.append(ApiSmokeTest.runNegativeTestInvalidQueryParams(curlCommand)).append("\n\n");
        
        logs.append("=== Running Negative Test: Missing Query Parameters ===\n");
        logs.append(ApiSmokeTest.runNegativeTestMissingQueryParams(curlCommand)).append("\n\n");
        
        logs.append("=== Running Negative Test: Wrong Content-Type ===\n");
        logs.append(ApiSmokeTest.runNegativeTestWrongContentType(curlCommand)).append("\n\n");
        
        logs.append("=== Running Negative Test: Malformed JSON ===\n");
        logs.append(ApiSmokeTest.runNegativeTestMalformedJson(curlCommand)).append("\n\n");
        
        logs.append("=== Running Negative Test: Oversized Payload ===\n");
        logs.append(ApiSmokeTest.runNegativeTestOversizedPayload(curlCommand)).append("\n\n");
        
        logs.append("=== Running Security Test: SQL Injection ===\n");
        logs.append(ApiSmokeTest.runNegativeTestSqlInjection(curlCommand)).append("\n\n");
        
        logs.append("=== Running Edge Case Test: Special Characters ===\n");
        logs.append(ApiSmokeTest.runNegativeTestSpecialCharacters(curlCommand)).append("\n\n");
        
        logs.append("=== Running Edge Case Test: Empty Values ===\n");
        logs.append(ApiSmokeTest.runNegativeTestEmptyValues(curlCommand)).append("\n\n");
        
        return logs.toString();
    }
    
    /**
     * Execute multiple cURL commands with validation filtering (executes only valid commands)
     */
    @PostMapping(value = "/execute-multi-curl-filtered", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String executeMultiCurlFiltered(@RequestBody Map<String, List<String>> request) throws MalformedURLException {
        List<String> curlCommands = request.get("curlCommands");
        StringBuilder logs = new StringBuilder();
        long startTime = System.currentTimeMillis();
        
        // Filter and validate curl commands (keep only valid ones)
        List<String> validCommands = validateAndFilterCurlCommands(curlCommands);
        
        if (validCommands.isEmpty()) {
            return "ERROR: No valid curl commands found. All commands failed validation.";
        }
        
        if (validCommands.size() < curlCommands.size()) {
            logs.append("WARNING: ").append(curlCommands.size() - validCommands.size())
                .append(" invalid commands were filtered out.\n");
            logs.append("Executing ").append(validCommands.size()).append(" valid commands.\n\n");
        }
        
        // Parse and normalize cURL commands (handle line breaks)
        List<String> normalizedCommands = parseAndNormalizeCurlCommands(validCommands);
        
        logs.append("=== Multi-cURL Filtered Execution Started ===\n");
        logs.append("Original Commands: ").append(curlCommands.size()).append("\n");
        logs.append("Valid Commands: ").append(validCommands.size()).append("\n");
        logs.append("Normalized Commands: ").append(normalizedCommands.size()).append("\n");
        logs.append("Execution Mode: Sequential (with all test scenarios)\n\n");
        
        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;
        
        for (int i = 0; i < normalizedCommands.size(); i++) {
            String curlCommand = normalizedCommands.get(i);
            logs.append("=== API ").append(i + 1).append(": ").append(extractApiName(curlCommand)).append(" ===\n");
            logs.append("Processing cURL command: ").append(curlCommand.substring(0, Math.min(curlCommand.length(), 100))).append("...\n");
            
            try {
                // Run all test scenarios for this cURL command
                String result = executeAllTestsForCurl(curlCommand);
                logs.append(result).append("\n\n");
                
                // Count results (simple parsing)
                if (result.contains("Passed")) {
                    passedTests++;
                } else {
                    failedTests++;
                }
                totalTests++;
                
            } catch (Exception e) {
                logs.append("ERROR executing API ").append(i + 1).append(": ").append(e.getMessage()).append("\n");
                logs.append("Stack trace: ").append(e.getClass().getSimpleName()).append("\n\n");
                failedTests++;
                totalTests++;
            }
        }
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        logs.append("=== Multi-cURL Filtered Execution Summary ===\n");
        logs.append("Total APIs: ").append(normalizedCommands.size()).append("\n");
        logs.append("Total Tests: ").append(totalTests).append("\n");
        logs.append("Passed: ").append(passedTests).append("\n");
        logs.append("Failed: ").append(failedTests).append("\n");
        logs.append("Execution Time: ").append(executionTime).append("ms\n");
        logs.append("Average Time per API: ").append(executionTime / validCommands.size()).append("ms\n");
        
        return logs.toString();
    }
    
    /**
     * Test endpoint to debug multi-cURL parsing
     */
    @PostMapping(value = "/test-multi-curl", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String testMultiCurl(@RequestBody List<String> curlCommands) {
        StringBuilder logs = new StringBuilder();
        logs.append("=== Multi-cURL Debug Test ===\n");
        logs.append("Total Commands Received: ").append(curlCommands.size()).append("\n\n");
        
        for (int i = 0; i < curlCommands.size(); i++) {
            String curlCommand = curlCommands.get(i);
            logs.append("Command ").append(i + 1).append(": ").append(curlCommand).append("\n");
            logs.append("Length: ").append(curlCommand.length()).append(" characters\n");
            logs.append("Starts with curl: ").append(curlCommand.trim().startsWith("curl")).append("\n\n");
        }
        
        return logs.toString();
    }
    
    /**
     * Parse and normalize cURL commands to handle line breaks
     */
    private List<String> parseAndNormalizeCurlCommands(List<String> curlCommands) {
        List<String> normalizedCommands = new ArrayList<>();
        
        for (String command : curlCommands) {
            // Check if this is a single string containing multiple cURL commands
            if (command.contains("curl ") && command.split("curl ").length > 2) {
                // Split by "curl " and process each command
                String[] parts = command.split("curl ");
                for (int i = 1; i < parts.length; i++) { // Skip first empty part
                    String curlCommand = "curl " + parts[i].trim();
                    String normalized = normalizeCurlCommand(curlCommand);
                    if (!normalized.trim().isEmpty()) {
                        normalizedCommands.add(normalized);
                    }
                }
            } else {
                // Single cURL command
                String normalized = normalizeCurlCommand(command);
                if (!normalized.trim().isEmpty()) {
                    normalizedCommands.add(normalized);
                }
            }
        }
        
        return normalizedCommands;
    }
    
    /**
     * Normalize a single cURL command (handle line breaks, backslashes, etc.)
     */
    private String normalizeCurlCommand(String curlCommand) {
        if (curlCommand == null || curlCommand.trim().isEmpty()) {
            return "";
        }
        
        // Remove line breaks and normalize whitespace
        String normalized = curlCommand
            .replaceAll("\\\\\\s*\\n", " ")  // Handle backslash line continuations
            .replaceAll("\\n", " ")          // Replace newlines with spaces
            .replaceAll("\\r", " ")          // Replace carriage returns with spaces
            .replaceAll("\\s+", " ")         // Replace multiple spaces with single space
            .trim();
        
        // Ensure it starts with 'curl'
        if (!normalized.toLowerCase().startsWith("curl")) {
            return "";
        }
        
        return normalized;
    }
    
    /**
     * Extract API name from cURL command
     */
    private String extractApiName(String curlCommand) {
        try {
            // Extract URL from cURL command
            String url = curlCommand.replaceAll(".*https?://([^\\s]+).*", "$1");
            if (url.length() > 50) {
                url = url.substring(0, 47) + "...";
            }
            return url;
        } catch (Exception e) {
            return "Unknown API";
        }
    }
    
    /**
     * Validates if the input contains multiple curl commands
     */
    private boolean isMultipleCurlCommands(String curlCommand) {
        if (curlCommand == null || curlCommand.trim().isEmpty()) {
            return false;
        }
        
        // Count occurrences of "curl " (case insensitive)
        String normalized = curlCommand.toLowerCase().trim();
        int curlCount = 0;
        int index = 0;
        
        while ((index = normalized.indexOf("curl ", index)) != -1) {
            curlCount++;
            index += 5; // Move past "curl "
        }
        
        // If more than one "curl " found, it's multiple commands
        return curlCount > 1;
    }
    
    /**
     * Validates that the input contains exactly one curl command
     */
    private String validateSingleCurlCommand(String curlCommand) {
        if (curlCommand == null || curlCommand.trim().isEmpty()) {
            return "Empty curl command provided.";
        }
        
        String normalized = curlCommand.trim();
        
        // Check if it starts with curl
        if (!normalized.toLowerCase().startsWith("curl")) {
            return "Command must start with 'curl'.";
        }
        
        // Count curl commands
        int curlCount = countCurlCommands(normalized);
        
        if (curlCount == 0) {
            return "No valid curl command found.";
        } else if (curlCount > 1) {
            return "Multiple curl commands detected (" + curlCount + " found). " +
                   "This endpoint only accepts single curl commands. " +
                   "Use /execute-multi-curl endpoint for multiple commands.";
        }
        
        return null; // Validation passed
    }
    
    /**
     * Counts the number of curl commands in the input
     */
    private int countCurlCommands(String curlCommand) {
        String normalized = curlCommand.toLowerCase();
        int count = 0;
        int index = 0;
        
        while ((index = normalized.indexOf("curl ", index)) != -1) {
            count++;
            index += 5; // Move past "curl "
        }
        
        return count;
    }
    
    /**
     * Validates curl command syntax and structure
     */
    private String validateCurlSyntax(String curlCommand) {
        if (curlCommand == null || curlCommand.trim().isEmpty()) {
            return "Empty curl command provided.";
        }
        
        String normalized = curlCommand.trim();
        
        // Check if it starts with curl
        if (!normalized.toLowerCase().startsWith("curl")) {
            return "Command must start with 'curl'.";
        }
        
        // Check for basic curl structure
        if (normalized.length() < 10) {
            return "Curl command appears to be too short or incomplete.";
        }
        
        // Check for URL presence
        if (!containsValidUrl(normalized)) {
            return "No valid URL found in curl command.";
        }
        
        return null; // Validation passed
    }
    
    /**
     * Validates URL format in curl command
     */
    private boolean containsValidUrl(String curlCommand) {
        // Check for http:// or https://
        String lowerCurl = curlCommand.toLowerCase();
        return lowerCurl.contains("http://") || lowerCurl.contains("https://");
    }
    
    /**
     * Validates HTTP method in curl command
     */
    private String validateHttpMethod(String curlCommand) {
        String normalized = curlCommand.toLowerCase();
        
        // Check for valid HTTP methods with various formats
        String[] validMethods = {"get", "post", "put", "delete", "patch", "head", "options"};
        boolean hasValidMethod = false;
        
        for (String method : validMethods) {
            // Check for -X method, --request method, and quoted versions
            if (normalized.contains("-x " + method) || 
                normalized.contains("--request " + method) ||
                normalized.contains("-x '" + method + "'") ||
                normalized.contains("--request '" + method + "'") ||
                normalized.contains("-x \"" + method + "\"") ||
                normalized.contains("--request \"" + method + "\"")) {
                hasValidMethod = true;
                break;
            }
        }
        
        // Check for method hints
        if (normalized.contains("-i") || normalized.contains("--head")) {
            hasValidMethod = true;
        }
        
        // If no explicit method, it defaults to GET (which is valid)
        if (!normalized.contains("-x") && !normalized.contains("--request") && 
            !normalized.contains("-i") && !normalized.contains("--head")) {
            hasValidMethod = true;
        }
        
        if (!hasValidMethod) {
            return "Invalid or unsupported HTTP method specified.";
        }
        
        return null;
    }
    
    /**
     * Validates header format in curl command
     */
    private String validateHeaders(String curlCommand) {
        String normalized = curlCommand;
        
        // Check for malformed headers
        if (normalized.contains("-H ") || normalized.contains("--header ")) {
            // Basic check for header format
            if (normalized.contains("-H \"\"") || normalized.contains("--header \"\"")) {
                return "Empty header value detected.";
            }
            
            // Check for unclosed quotes in headers
            if (countOccurrences(normalized, "\"") % 2 != 0) {
                return "Unclosed quotes detected in headers.";
            }
        }
        
        return null;
    }
    
    /**
     * Comprehensive curl validation that checks all aspects
     */
    private String validateCurlCommand(String curlCommand) {
        // Basic syntax validation
        String syntaxError = validateCurlSyntax(curlCommand);
        if (syntaxError != null) {
            return syntaxError;
        }
        
        // HTTP method validation
        String methodError = validateHttpMethod(curlCommand);
        if (methodError != null) {
            return methodError;
        }
        
        // Header validation
        String headerError = validateHeaders(curlCommand);
        if (headerError != null) {
            return headerError;
        }
        
        // Check for common curl command issues
        String commonIssues = checkCommonCurlIssues(curlCommand);
        if (commonIssues != null) {
            return commonIssues;
        }
        
        return null; // All validations passed
    }
    
    /**
     * Checks for common curl command issues
     */
    private String checkCommonCurlIssues(String curlCommand) {
        String normalized = curlCommand.trim();
        
        // Check for malformed quotes
        if (countOccurrences(normalized, "'") % 2 != 0) {
            return "Unclosed single quotes detected.";
        }
        
        // Check for incomplete data flags
        if (normalized.contains("-d ") && !normalized.contains("-d \"") && !normalized.contains("-d '")) {
            return "Data flag (-d) appears to be incomplete or malformed.";
        }
        
        // Check for incomplete URL
        if (normalized.contains("http://") && !normalized.contains("http://") && 
            !normalized.contains("https://") && !normalized.contains(" ")) {
            return "URL appears to be incomplete.";
        }
        
        return null;
    }
    
    /**
     * Helper method to count occurrences of a character in a string
     */
    private int countOccurrences(String str, String target) {
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(target, index)) != -1) {
            count++;
            index += target.length();
        }
        return count;
    }
    
    /**
     * Validates multiple curl commands and returns validation results
     */
    private String validateMultipleCurlCommands(List<String> curlCommands) {
        if (curlCommands == null || curlCommands.isEmpty()) {
            return "No curl commands provided.";
        }
        
        StringBuilder validationErrors = new StringBuilder();
        int errorCount = 0;
        
        for (int i = 0; i < curlCommands.size(); i++) {
            String curlCommand = curlCommands.get(i);
            String validationResult = validateCurlCommand(curlCommand);
            
            if (validationResult != null) {
                errorCount++;
                validationErrors.append("Command ").append(i + 1).append(": ").append(validationResult).append("\n");
            }
        }
        
        if (errorCount > 0) {
            return "CURL VALIDATION ERRORS (" + errorCount + " commands failed):\n" + validationErrors.toString();
        }
        
        return null; // All validations passed
    }
    
    /**
     * Validates and filters valid curl commands from a list
     */
    private List<String> validateAndFilterCurlCommands(List<String> curlCommands) {
        List<String> validCommands = new ArrayList<>();
        
        for (String curlCommand : curlCommands) {
            String validationResult = validateCurlCommand(curlCommand);
            if (validationResult == null) {
                validCommands.add(curlCommand);
            }
        }
        
        return validCommands;
    }
}
