package com.apiautomation.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Structured, redacted context sent to the AI generator (no secrets / raw tokens).
 */
public class ContextPack {
    private String method;
    private String path;
    private String profileHint;
    private Map<String, Boolean> facts = new LinkedHashMap<>();
    private List<String> fields = new ArrayList<>();
    private Map<String, String> fieldTypes = new LinkedHashMap<>();
    private List<String> alreadyCoveredCaseIds = new ArrayList<>();
    private List<String> forbidden = List.of(
            "sql_injection", "brute_force", "xss", "ssrf", "ddos", "exploit", "pentest"
    );
    private String goal = "functional_bug_detection_only";
    private int maxScenarios = 5;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getProfileHint() {
        return profileHint;
    }

    public void setProfileHint(String profileHint) {
        this.profileHint = profileHint;
    }

    public Map<String, Boolean> getFacts() {
        return facts;
    }

    public void setFacts(Map<String, Boolean> facts) {
        this.facts = facts;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public Map<String, String> getFieldTypes() {
        return fieldTypes;
    }

    public void setFieldTypes(Map<String, String> fieldTypes) {
        this.fieldTypes = fieldTypes;
    }

    public List<String> getAlreadyCoveredCaseIds() {
        return alreadyCoveredCaseIds;
    }

    public void setAlreadyCoveredCaseIds(List<String> alreadyCoveredCaseIds) {
        this.alreadyCoveredCaseIds = alreadyCoveredCaseIds;
    }

    public List<String> getForbidden() {
        return forbidden;
    }

    public void setForbidden(List<String> forbidden) {
        this.forbidden = forbidden;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public int getMaxScenarios() {
        return maxScenarios;
    }

    public void setMaxScenarios(int maxScenarios) {
        this.maxScenarios = maxScenarios;
    }
}
