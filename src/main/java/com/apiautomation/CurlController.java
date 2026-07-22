package com.apiautomation;
import com.apiautomation.report.MultiSuiteReport;
import com.apiautomation.report.SuiteReport;
import com.apiautomation.security.ApiSecurityProperties;
import com.apiautomation.security.SsrfGuard;
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

    private final ApiSecurityProperties securityProperties;

    public CurlController(ApiSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /**
     * Structured JSON report for a single curl (Pass / Fail / Warning / Skip).
     */
    @PostMapping(value = "/execute-curl/report",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SuiteReport executeCurlReport(@RequestBody String curlCommand) {
        String sizeError = validateRequestSize(curlCommand);
        if (sizeError != null) {
            return validationSuiteError(sizeError);
        }
        String curlValidationResult = validateCurlCommand(curlCommand);
        if (curlValidationResult != null) {
            return validationSuiteError(curlValidationResult);
        }
        String validationResult = validateSingleCurlCommand(curlCommand);
        if (validationResult != null) {
            return validationSuiteError(validationResult);
        }
        String ssrf = validateSsrf(curlCommand);
        if (ssrf != null) {
            return validationSuiteError(ssrf);
        }
        return ApiTestSuite.runAll(curlCommand);
    }

    /**
     * Structured JSON report for one or more curls.
     */
    @PostMapping(value = "/execute-multi-curl/report",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public MultiSuiteReport executeMultiCurlReport(@RequestBody Map<String, Object> request) {
        Object rawCommands = request.get("curlCommands");
        List<String> curlCommands = new ArrayList<>();
        if (rawCommands instanceof List<?> list) {
            for (Object o : list) {
                if (o != null) {
                    curlCommands.add(o.toString());
                }
            }
        }
        boolean aiScenarios = Boolean.TRUE.equals(request.get("aiScenarios"))
                || "true".equalsIgnoreCase(String.valueOf(request.get("aiScenarios")));

        long startTime = System.currentTimeMillis();

        if (curlCommands.isEmpty()) {
            return MultiSuiteReport.validationFailed("No curl commands provided.");
        }

        String sizeError = validateRequestSize(String.join("\n", curlCommands));
        if (sizeError != null) {
            return MultiSuiteReport.validationFailed(sizeError);
        }

        List<String> normalizedCommands = parseAndNormalizeCurlCommands(curlCommands);
        if (normalizedCommands.isEmpty()) {
            return MultiSuiteReport.validationFailed("No valid curl commands found after parsing.");
        }

        if (normalizedCommands.size() > securityProperties.getMaxCurlsPerRequest()) {
            return MultiSuiteReport.validationFailed(
                    "Too many curl commands — max " + securityProperties.getMaxCurlsPerRequest()
                            + " per request (got " + normalizedCommands.size() + ").");
        }

        String validationResult = validateMultipleCurlCommands(normalizedCommands);
        if (validationResult != null) {
            return MultiSuiteReport.validationFailed(validationResult);
        }

        for (String curlCommand : normalizedCommands) {
            String ssrf = validateSsrf(curlCommand);
            if (ssrf != null) {
                return MultiSuiteReport.validationFailed(ssrf);
            }
        }

        List<SuiteReport> reports = new ArrayList<>();
        for (String curlCommand : normalizedCommands) {
            reports.add(ApiTestSuite.runAll(curlCommand, aiScenarios));
        }
        return MultiSuiteReport.from(reports, System.currentTimeMillis() - startTime);
    }

    private SuiteReport validationSuiteError(String message) {
        SuiteReport report = SuiteReport.from(null, null, List.of(), 0);
        report.setScenarios(List.of(
                com.apiautomation.report.ScenarioResult.error(
                        "validation", "Curl Validation", "P0", message)));
        report.setErrors(1);
        report.setTotal(1);
        return report;
    }

    private String validateRequestSize(String payload) {
        if (payload != null && payload.length() > securityProperties.getMaxRequestChars()) {
            return "Request too large — max " + securityProperties.getMaxRequestChars()
                    + " characters (got " + payload.length() + ").";
        }
        return null;
    }

    private String validateSsrf(String curlCommand) {
        try {
            ParsedCurl parsed = CurlParser.parseCurl(curlCommand);
            return SsrfGuard.check(parsed.getUrl());
        } catch (Exception e) {
            return "Unable to validate URL for SSRF protection: " + e.getMessage();
        }
    }

    @PostMapping(value = "/execute-curl", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String executeCurl(@RequestBody String curlCommand) {
        String sizeError = validateRequestSize(curlCommand);
        if (sizeError != null) {
            return "ERROR: " + sizeError;
        }
        String curlValidationResult = validateCurlCommand(curlCommand);
        if (curlValidationResult != null) {
            return "CURL VALIDATION ERROR: " + curlValidationResult;
        }
        String validationResult = validateSingleCurlCommand(curlCommand);
        if (validationResult != null) {
            return "ERROR: " + validationResult;
        }
        if (isMultipleCurlCommands(curlCommand)) {
            return "ERROR: Multiple curl commands detected. This endpoint only accepts single curl commands.";
        }
        String ssrf = validateSsrf(curlCommand);
        if (ssrf != null) {
            return "ERROR: " + ssrf;
        }
        return formatSuiteAsText(ApiTestSuite.runAll(curlCommand));
    }

    private String formatSuiteAsText(SuiteReport report) {
        StringBuilder logs = new StringBuilder();
        logs.append("Bug-detection suite (no attack packs)\n");
        logs.append("API: ").append(report.getMethod()).append(" ").append(report.getUrl()).append("\n");
        logs.append("Passed: ").append(report.getPassed())
                .append(" | Failed: ").append(report.getFailed())
                .append(" | Warnings: ").append(report.getWarnings())
                .append(" | Skipped: ").append(report.getSkipped())
                .append(" | Duration: ").append(report.getDurationMs()).append("ms\n\n");

        for (com.apiautomation.report.ScenarioResult s : report.getScenarios()) {
            logs.append("=== ").append(s.getName()).append(" ===\n");
            logs.append("Objective: ").append(nullToEmpty(s.getObjective())).append("\n");
            logs.append("Expected Result: ").append(nullToEmpty(s.getExpectedResult())).append("\n");
            logs.append("Actual Result: ").append(nullToEmpty(s.getActualResult())).append("\n");
            logs.append("Risk: ").append(s.getRisk()).append(" | Verdict: ").append(s.getVerdict()).append("\n");
            logs.append(nullToEmpty(s.getDetail())).append("\n\n");
        }
        return logs.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * Execute all bug-detection scenarios for a single cURL command
     */
    private String executeAllTestsForCurl(String curlCommand) {
        return formatSuiteAsText(ApiTestSuite.runAll(curlCommand));
    }

    /**
     * Execute multiple cURL commands with validation filtering (executes only valid commands)
     */
    @PostMapping(value = "/execute-multi-curl", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String executeMultiCurl(@RequestBody Map<String, List<String>> request) throws MalformedURLException {
        List<String> curlCommands = request.get("curlCommands");
        StringBuilder logs = new StringBuilder();
        long startTime = System.currentTimeMillis();

        if (curlCommands == null || curlCommands.isEmpty()) {
            return "VALIDATION FAILED:\nNo curl commands provided.";
        }
        String sizeError = validateRequestSize(String.join("\n", curlCommands));
        if (sizeError != null) {
            return "VALIDATION FAILED:\n" + sizeError;
        }
        
        // Validate all curl commands first
        String validationResult = validateMultipleCurlCommands(curlCommands);
        if (validationResult != null) {
            return "VALIDATION FAILED:\n" + validationResult + 
                   "\nPlease fix the validation errors and try again.";
        }
        
        // Parse and normalize cURL commands (handle line breaks)
        List<String> normalizedCommands = parseAndNormalizeCurlCommands(curlCommands);
        if (normalizedCommands.size() > securityProperties.getMaxCurlsPerRequest()) {
            return "VALIDATION FAILED:\nToo many curl commands — max "
                    + securityProperties.getMaxCurlsPerRequest() + " per request.";
        }
        for (String cmd : normalizedCommands) {
            String ssrf = validateSsrf(cmd);
            if (ssrf != null) {
                return "VALIDATION FAILED:\n" + ssrf;
            }
        }
        
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
                
                // Count per-scenario outcomes in the combined result blob
                int passedInApi = countOccurrences(result, "Passed");
                int failedInApi = countOccurrences(result, "Failed");
                int warningsInApi = countOccurrences(result, "Warning");
                passedTests += passedInApi;
                failedTests += failedInApi;
                totalTests += passedInApi + failedInApi + warningsInApi
                        + countOccurrences(result, "Test Case Skipped");
                logs.append("API scenario tally — Passed: ").append(passedInApi)
                        .append(", Failed: ").append(failedInApi)
                        .append(", Warnings: ").append(warningsInApi).append("\n");
                
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
        if (normalizedCommands.size() > securityProperties.getMaxCurlsPerRequest()) {
            return "ERROR: Too many curl commands — max "
                    + securityProperties.getMaxCurlsPerRequest() + " per request.";
        }
        for (String cmd : normalizedCommands) {
            String ssrf = validateSsrf(cmd);
            if (ssrf != null) {
                return "ERROR: " + ssrf;
            }
        }
        
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
                
                // Count per-scenario outcomes in the combined result blob
                int passedInApi = countOccurrences(result, "Passed");
                int failedInApi = countOccurrences(result, "Failed");
                int warningsInApi = countOccurrences(result, "Warning");
                passedTests += passedInApi;
                failedTests += failedInApi;
                totalTests += passedInApi + failedInApi + warningsInApi
                        + countOccurrences(result, "Test Case Skipped");
                logs.append("API scenario tally — Passed: ").append(passedInApi)
                        .append(", Failed: ").append(failedInApi)
                        .append(", Warnings: ").append(warningsInApi).append("\n");
                
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
