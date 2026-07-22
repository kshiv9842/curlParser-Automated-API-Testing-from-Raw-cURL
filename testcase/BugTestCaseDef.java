package com.apiautomation.testcase;

/**
 * Bug-oriented test case definition (Objective + Expected are design-time).
 * Attack cases (SQL injection, brute force, etc.) are intentionally excluded.
 */
public final class BugTestCaseDef {
    private final String id;
    private final String name;
    private final String risk;
    private final String objective;
    private final String expectedResult;
    private final BugOracle oracle;

    public BugTestCaseDef(String id, String name, String risk,
                          String objective, String expectedResult, BugOracle oracle) {
        this.id = id;
        this.name = name;
        this.risk = risk;
        this.objective = objective;
        this.expectedResult = expectedResult;
        this.oracle = oracle;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRisk() {
        return risk;
    }

    public String getObjective() {
        return objective;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public BugOracle getOracle() {
        return oracle;
    }

    /**
     * @return skip reason, or null if the case should run
     */
    public String skipReason(RequestFacts facts) {
        return switch (id) {
            case "smoke", "wrong_method" -> null;
            case "missing_auth", "invalid_auth" -> facts.hasAuth() ? null : "No authentication header in original request";
            case "missing_headers" -> facts.hasHeaders() ? null : "No headers in original request";
            case "missing_body", "invalid_payload", "malformed_json",
                 "empty_values", "boundary_values", "special_characters" -> {
                if (!facts.hasBody()) {
                    yield "No request body in original request";
                }
                if (("malformed_json".equals(id) || "invalid_payload".equals(id)
                        || "empty_values".equals(id)) && !facts.isJson()) {
                    yield "Request body is not JSON";
                }
                if ("missing_body".equals(id) && !facts.isBodyMethod()) {
                    yield "HTTP method typically has no body (" + facts.getMethod() + ")";
                }
                yield null;
            }
            case "wrong_content_type" -> facts.hasBody() || facts.isBodyMethod()
                    ? null : "No body / non-body method — Content-Type case not applicable";
            case "invalid_path" -> facts.hasPathId() ? null : "No path id/parameter detected in URL";
            case "invalid_query", "missing_query" -> facts.hasQuery() ? null : "No query parameters in original request";
            default -> null;
        };
    }
}
