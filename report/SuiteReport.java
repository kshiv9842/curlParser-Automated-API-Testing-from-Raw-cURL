package com.apiautomation.report;

import java.util.ArrayList;
import java.util.List;

public class SuiteReport {
    private String method;
    private String url;
    private long durationMs;
    private int passed;
    private int failed;
    private int warnings;
    private int skipped;
    private int errors;
    private int total;
    private List<ScenarioResult> scenarios = new ArrayList<>();
    private AiMeta aiMeta;

    public static class AiMeta {
        private boolean enabled;
        private String source;
        private String message;
        private int proposed;
        private int accepted;
        private int rejected;
        private List<String> rejectReasons = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getProposed() {
            return proposed;
        }

        public void setProposed(int proposed) {
            this.proposed = proposed;
        }

        public int getAccepted() {
            return accepted;
        }

        public void setAccepted(int accepted) {
            this.accepted = accepted;
        }

        public int getRejected() {
            return rejected;
        }

        public void setRejected(int rejected) {
            this.rejected = rejected;
        }

        public List<String> getRejectReasons() {
            return rejectReasons;
        }

        public void setRejectReasons(List<String> rejectReasons) {
            this.rejectReasons = rejectReasons;
        }
    }

    public static SuiteReport from(String method, String url, List<ScenarioResult> scenarios, long durationMs) {
        SuiteReport report = new SuiteReport();
        report.method = method;
        report.url = url;
        report.scenarios = scenarios;
        report.durationMs = durationMs;
        for (ScenarioResult scenario : scenarios) {
            switch (scenario.getVerdict()) {
                case PASSED -> report.passed++;
                case FAILED -> report.failed++;
                case WARNING -> report.warnings++;
                case SKIPPED -> report.skipped++;
                case ERROR -> report.errors++;
            }
        }
        report.total = scenarios.size();
        return report;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public int getPassed() {
        return passed;
    }

    public void setPassed(int passed) {
        this.passed = passed;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public int getWarnings() {
        return warnings;
    }

    public void setWarnings(int warnings) {
        this.warnings = warnings;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<ScenarioResult> getScenarios() {
        return scenarios;
    }

    public void setScenarios(List<ScenarioResult> scenarios) {
        this.scenarios = scenarios;
    }

    public AiMeta getAiMeta() {
        return aiMeta;
    }

    public void setAiMeta(AiMeta aiMeta) {
        this.aiMeta = aiMeta;
    }
}
