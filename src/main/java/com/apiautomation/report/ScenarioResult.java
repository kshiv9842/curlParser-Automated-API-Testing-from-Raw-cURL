package com.apiautomation.report;

import com.apiautomation.testcase.BugOracle;
import com.apiautomation.testcase.BugTestCaseDef;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScenarioResult {
    private String id;
    private String name;
    private String risk;
    private String objective;
    private String expectedResult;
    private String actualResult;
    private TestVerdict verdict;
    private Integer statusCode;
    private String reason;
    private String detail;
    private String source = "RULE";

    private static final Pattern STATUS_CODE = Pattern.compile("API Status Code:\\s*(\\d+)");

    public static ScenarioResult fromLog(String id, String name, String risk, String detail) {
        ScenarioResult result = new ScenarioResult();
        result.id = id;
        result.name = name;
        result.risk = risk;
        result.detail = detail == null ? "" : detail;
        result.statusCode = extractStatusCode(result.detail);
        result.verdict = parseVerdict(result.detail);
        result.reason = extractReason(result.detail, result.verdict);
        result.actualResult = buildActual(result.statusCode, result.verdict, result.reason);
        return result;
    }

    public static ScenarioResult fromCatalog(BugTestCaseDef def, String detail) {
        ScenarioResult result = new ScenarioResult();
        result.id = def.getId();
        result.name = def.getName();
        result.risk = def.getRisk();
        result.objective = def.getObjective();
        result.expectedResult = def.getExpectedResult();
        result.source = "RULE";
        result.detail = detail == null ? "" : detail;
        result.statusCode = extractStatusCode(result.detail);

        if (result.detail.contains("Test Case Skipped")) {
            result.verdict = TestVerdict.SKIPPED;
            result.reason = extractReason(result.detail, result.verdict);
        } else {
            result.verdict = judge(def.getOracle(), result.statusCode, def.getId());
            result.reason = verdictReason(def.getOracle(), result.verdict, result.statusCode);
        }
        result.actualResult = buildActual(result.statusCode, result.verdict, result.reason);
        return result;
    }

    public static ScenarioResult fromAiSpec(com.apiautomation.ai.AiScenarioSpec spec, String detail,
                                            int statusCode, BugOracle oracle, String sourceTag) {
        ScenarioResult result = new ScenarioResult();
        result.id = spec.getId();
        result.name = "AI: " + spec.getId();
        result.risk = spec.getRisk();
        result.objective = spec.getObjective();
        result.expectedResult = spec.getExpectedResult();
        result.source = sourceTag == null ? "AI" : sourceTag;
        result.detail = detail == null ? "" : detail;
        result.statusCode = statusCode;
        result.verdict = judge(oracle, statusCode, spec.getId());
        result.reason = verdictReason(oracle, result.verdict, statusCode);
        result.actualResult = buildActual(result.statusCode, result.verdict, result.reason);
        return result;
    }

    public static ScenarioResult aiError(com.apiautomation.ai.AiScenarioSpec spec, String message, String sourceTag) {
        ScenarioResult result = new ScenarioResult();
        result.id = spec.getId();
        result.name = "AI: " + spec.getId();
        result.risk = spec.getRisk() != null ? spec.getRisk() : "P1";
        result.objective = spec.getObjective();
        result.expectedResult = spec.getExpectedResult();
        result.source = sourceTag == null ? "AI" : sourceTag;
        result.verdict = TestVerdict.ERROR;
        result.reason = message;
        result.detail = message;
        result.actualResult = "Error — " + message;
        return result;
    }

    public static ScenarioResult skipped(BugTestCaseDef def, String skipReason) {
        ScenarioResult result = new ScenarioResult();
        result.id = def.getId();
        result.name = def.getName();
        result.risk = def.getRisk();
        result.objective = def.getObjective();
        result.expectedResult = def.getExpectedResult();
        result.source = "RULE";
        result.verdict = TestVerdict.SKIPPED;
        result.reason = skipReason;
        result.actualResult = "Skipped — " + skipReason;
        result.detail = "Test Case Skipped.\n" + skipReason;
        return result;
    }

    public static ScenarioResult error(String id, String name, String risk, String message) {
        ScenarioResult result = new ScenarioResult();
        result.id = id;
        result.name = name;
        result.risk = risk;
        result.objective = "Validate curl / execution preconditions";
        result.expectedResult = "Valid executable request";
        result.verdict = TestVerdict.ERROR;
        result.reason = message;
        result.detail = message;
        result.actualResult = "Error — " + message;
        return result;
    }

    public static ScenarioResult error(BugTestCaseDef def, String message) {
        ScenarioResult result = error(def.getId(), def.getName(), def.getRisk(), message);
        result.objective = def.getObjective();
        result.expectedResult = def.getExpectedResult();
        return result;
    }

    private static TestVerdict judge(BugOracle oracle, Integer status, String caseId) {
        if (status == null) {
            return TestVerdict.FAILED;
        }
        boolean ok2xx = status >= 200 && status < 300;
        boolean client4xx = status >= 400 && status < 500;
        boolean server5xx = status >= 500;

        if (server5xx) {
            return TestVerdict.FAILED;
        }

        return switch (oracle) {
            case ACCEPT -> ok2xx ? TestVerdict.PASSED : TestVerdict.FAILED;
            case REJECT -> client4xx ? TestVerdict.PASSED : TestVerdict.FAILED;
            case OBSERVE -> {
                if (client4xx) {
                    yield TestVerdict.PASSED;
                }
                if (ok2xx) {
                    // Unicode/special chars succeeding is OK for bug mode
                    if ("special_characters".equals(caseId)) {
                        yield TestVerdict.PASSED;
                    }
                    yield TestVerdict.WARNING;
                }
                yield TestVerdict.FAILED;
            }
        };
    }

    private static String verdictReason(BugOracle oracle, TestVerdict verdict, Integer status) {
        String http = status == null ? "no response" : "HTTP " + status;
        return switch (verdict) {
            case PASSED -> "Passed — behaviour matches objective (" + http + ")";
            case FAILED -> switch (oracle) {
                case ACCEPT -> "Failed — valid request did not succeed (" + http + ")";
                case REJECT -> "Failed — possible API bug: invalid input was not rejected (" + http + ")";
                case OBSERVE -> "Failed — unexpected server/error behaviour (" + http + ")";
            };
            case WARNING -> "Warning — API accepted input that is often rejected (" + http + "); review if validation is required";
            case SKIPPED -> "Skipped";
            case ERROR -> "Error during execution";
        };
    }

    private static String buildActual(Integer statusCode, TestVerdict verdict, String reason) {
        if (verdict == TestVerdict.SKIPPED) {
            return reason != null && reason.startsWith("Skipped") ? reason : "Skipped — " + reason;
        }
        if (statusCode != null) {
            return "HTTP " + statusCode + (reason != null ? " · " + shortReason(reason) : "");
        }
        return reason != null ? reason : verdict.name();
    }

    private static String shortReason(String reason) {
        if (reason.length() <= 120) {
            return reason;
        }
        return reason.substring(0, 117) + "...";
    }

    private static Integer extractStatusCode(String detail) {
        Matcher matcher = STATUS_CODE.matcher(detail);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private static TestVerdict parseVerdict(String detail) {
        if (detail.contains("Test Case Skipped")) {
            return TestVerdict.SKIPPED;
        }
        if (detail.contains("Warning")) {
            return TestVerdict.WARNING;
        }
        if (detail.contains("Passed")) {
            return TestVerdict.PASSED;
        }
        if (detail.contains("Failed") || detail.contains("ERROR") || detail.contains("error:")) {
            return TestVerdict.FAILED;
        }
        return TestVerdict.ERROR;
    }

    private static String extractReason(String detail, TestVerdict verdict) {
        String[] lines = detail.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("Passed") || line.startsWith("Failed") || line.startsWith("Warning")
                    || line.contains("Test Case Skipped")) {
                return line;
            }
        }
        return verdict.name();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRisk() {
        return risk;
    }

    public void setRisk(String risk) {
        this.risk = risk;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public String getActualResult() {
        return actualResult;
    }

    public void setActualResult(String actualResult) {
        this.actualResult = actualResult;
    }

    public TestVerdict getVerdict() {
        return verdict;
    }

    public void setVerdict(TestVerdict verdict) {
        this.verdict = verdict;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
