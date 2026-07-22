package com.apiautomation;

import com.apiautomation.ai.AiScenarioService;
import com.apiautomation.report.ScenarioResult;
import com.apiautomation.report.SuiteReport;
import com.apiautomation.testcase.BugTestCaseDef;
import com.apiautomation.testcase.BugTestCatalog;
import com.apiautomation.testcase.RequestFacts;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the bug-detection catalog; optionally adds validated AI/shape scenarios.
 */
public final class ApiTestSuite {

    private ApiTestSuite() {
    }

    public static SuiteReport runAll(String curlCommand) {
        return runAll(curlCommand, false);
    }

    public static SuiteReport runAll(String curlCommand, boolean enableAiScenarios) {
        long start = System.currentTimeMillis();
        ParsedCurl parsed = CurlParser.parseCurl(curlCommand);
        RequestFacts facts = RequestFacts.from(parsed);
        List<ScenarioResult> scenarios = new ArrayList<>();

        for (BugTestCaseDef def : BugTestCatalog.all()) {
            String skipReason = def.skipReason(facts);
            if (skipReason != null) {
                scenarios.add(ScenarioResult.skipped(def, skipReason));
                continue;
            }
            try {
                String detail = execute(def.getId(), curlCommand);
                scenarios.add(ScenarioResult.fromCatalog(def, detail));
            } catch (Exception e) {
                scenarios.add(ScenarioResult.error(def, e.getMessage()));
            }
        }

        SuiteReport.AiMeta aiMeta = new SuiteReport.AiMeta();
        aiMeta.setEnabled(enableAiScenarios);

        if (enableAiScenarios) {
            AiScenarioService.AiRunResult ai = new AiScenarioService().runAiScenarios(curlCommand, parsed, facts);
            scenarios.addAll(ai.getResults());
            aiMeta.setSource(ai.getSource());
            aiMeta.setMessage(ai.getMessage());
            aiMeta.setProposed(ai.getProposed());
            aiMeta.setAccepted(ai.getAccepted());
            aiMeta.setRejected(ai.getRejected());
            aiMeta.setRejectReasons(ai.getRejectReasons());
        }

        SuiteReport report = SuiteReport.from(
                parsed.getMethod(),
                parsed.getUrl(),
                scenarios,
                System.currentTimeMillis() - start
        );
        report.setAiMeta(aiMeta);
        return report;
    }

    private static String execute(String id, String curlCommand) throws Exception {
        return switch (id) {
            case "smoke" -> ApiSmokeTest.runBasicSmokeTest(curlCommand);
            case "invalid_payload" -> ApiSmokeTest.runNegativePayloadBody(curlCommand);
            case "missing_headers" -> ApiSmokeTest.runNegativeMissingHeaders(curlCommand);
            case "missing_auth" -> ApiSmokeTest.runNegativeTestRemoveAuth(curlCommand);
            case "wrong_method" -> ApiSmokeTest.runNegativeUnsupportedMethod(curlCommand);
            case "missing_body" -> ApiSmokeTest.runNegativeTestRemoveBody(curlCommand);
            case "invalid_path" -> ApiSmokeTest.runNegativeTestUpdatePathParam(curlCommand);
            case "invalid_query" -> ApiSmokeTest.runNegativeTestInvalidQueryParams(curlCommand);
            case "missing_query" -> ApiSmokeTest.runNegativeTestMissingQueryParams(curlCommand);
            case "wrong_content_type" -> ApiSmokeTest.runNegativeTestWrongContentType(curlCommand);
            case "malformed_json" -> ApiSmokeTest.runNegativeTestMalformedJson(curlCommand);
            case "boundary_values" -> ApiSmokeTest.runNegativeTestOversizedPayload(curlCommand);
            case "special_characters" -> ApiSmokeTest.runNegativeTestSpecialCharacters(curlCommand);
            case "empty_values" -> ApiSmokeTest.runNegativeTestEmptyValues(curlCommand);
            case "perf_latency_sla" -> ApiPerfTest.runBaselineLatencySla(curlCommand);
            case "perf_timeout" -> ApiPerfTest.runTimeoutHang(curlCommand);
            case "perf_error_latency" -> ApiPerfTest.runErrorPathLatency(curlCommand);
            case "perf_large_payload" -> ApiPerfTest.runLargePayloadLatency(curlCommand);
            case "perf_repeat_get" -> ApiPerfTest.runRepeatGetStability(curlCommand);
            default -> throw new IllegalArgumentException("Unknown bug test case: " + id);
        };
    }
}
