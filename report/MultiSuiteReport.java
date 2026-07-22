package com.apiautomation.report;

import java.util.ArrayList;
import java.util.List;

public class MultiSuiteReport {
    private int apiCount;
    private long durationMs;
    private int passed;
    private int failed;
    private int warnings;
    private int skipped;
    private int errors;
    private int total;
    private List<SuiteReport> reports = new ArrayList<>();
    private String validationError;

    public static MultiSuiteReport from(List<SuiteReport> reports, long durationMs) {
        MultiSuiteReport multi = new MultiSuiteReport();
        multi.reports = reports;
        multi.apiCount = reports.size();
        multi.durationMs = durationMs;
        for (SuiteReport report : reports) {
            multi.passed += report.getPassed();
            multi.failed += report.getFailed();
            multi.warnings += report.getWarnings();
            multi.skipped += report.getSkipped();
            multi.errors += report.getErrors();
            multi.total += report.getTotal();
        }
        return multi;
    }

    public static MultiSuiteReport validationFailed(String message) {
        MultiSuiteReport multi = new MultiSuiteReport();
        multi.validationError = message;
        return multi;
    }

    public int getApiCount() {
        return apiCount;
    }

    public void setApiCount(int apiCount) {
        this.apiCount = apiCount;
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

    public List<SuiteReport> getReports() {
        return reports;
    }

    public void setReports(List<SuiteReport> reports) {
        this.reports = reports;
    }

    public String getValidationError() {
        return validationError;
    }

    public void setValidationError(String validationError) {
        this.validationError = validationError;
    }
}
