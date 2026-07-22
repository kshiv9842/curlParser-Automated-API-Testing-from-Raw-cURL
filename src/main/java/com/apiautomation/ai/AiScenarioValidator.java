package com.apiautomation.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic gates — hallucination stops here before execution.
 */
public final class AiScenarioValidator {

    private AiScenarioValidator() {
    }

    public static final class Result {
        private final List<AiScenarioSpec> accepted = new ArrayList<>();
        private final List<String> rejected = new ArrayList<>();

        public List<AiScenarioSpec> getAccepted() {
            return accepted;
        }

        public List<String> getRejected() {
            return rejected;
        }
    }

    public static Result validate(List<AiScenarioSpec> proposed, ContextPack pack) {
        Result result = new Result();
        if (proposed == null) {
            return result;
        }

        for (AiScenarioSpec spec : proposed) {
            String error = validateOne(spec, pack, result.accepted);
            if (error != null) {
                result.rejected.add((spec.getId() == null ? "?" : spec.getId()) + ": " + error);
            } else {
                result.accepted.add(spec);
            }
            if (result.accepted.size() >= pack.getMaxScenarios()) {
                break;
            }
        }
        return result;
    }

    private static String validateOne(AiScenarioSpec spec, ContextPack pack, List<AiScenarioSpec> alreadyAccepted) {
        if (spec == null) {
            return "null scenario";
        }
        if (spec.getId() == null || !spec.getId().matches("^[a-z][a-z0-9_]{1,64}$")) {
            return "invalid id";
        }
        if (pack.getAlreadyCoveredCaseIds().contains(spec.getId())) {
            return "duplicates rule catalog id";
        }
        for (AiScenarioSpec a : alreadyAccepted) {
            if (a.getId().equals(spec.getId())) {
                return "duplicate ai id";
            }
        }
        String blob = (safe(spec.getId()) + " " + safe(spec.getObjective()) + " " + safe(spec.getRationale())).toLowerCase(Locale.ROOT);
        for (String bad : pack.getForbidden()) {
            if (bad != null && !bad.isBlank() && blob.contains(bad.trim().toLowerCase(Locale.ROOT))) {
                return "forbidden topic: " + bad.trim();
            }
        }
        if (isBlank(spec.getObjective()) || spec.getObjective().length() > 200) {
            return "objective missing or too long";
        }
        if (isBlank(spec.getExpectedResult()) || spec.getExpectedResult().length() > 160) {
            return "expectedResult missing or too long";
        }
        if (!AiScenarioSpec.ALLOWED_RISKS.contains(spec.getRisk())) {
            return "invalid risk";
        }
        if (!AiScenarioSpec.ALLOWED_ORACLES.contains(spec.getOracle())) {
            return "invalid oracle";
        }
        AiMutation m = spec.getMutation();
        if (m == null || m.getType() == null || !AiMutation.ALLOWED_TYPES.contains(m.getType())) {
            return "mutation.type not in allowlist";
        }

        // requiresFacts
        if (spec.getRequiresFacts() != null) {
            for (String req : spec.getRequiresFacts()) {
                Boolean ok = pack.getFacts().get(req);
                if (ok == null || !ok) {
                    return "requiresFacts not satisfied: " + req;
                }
            }
        }

        switch (m.getType()) {
            case "OMIT_FIELD", "REPLACE_FIELD" -> {
                if (isBlank(m.getField())) {
                    return "field required for " + m.getType();
                }
                if (!pack.getFields().contains(m.getField()) && !pack.getFields().contains(topLevel(m.getField()))) {
                    // allow top-level match
                    boolean found = pack.getFields().stream().anyMatch(f -> f.equals(m.getField()) || f.startsWith(m.getField() + "."));
                    if (!found) {
                        return "field not in request body: " + m.getField();
                    }
                }
                if (!Boolean.TRUE.equals(pack.getFacts().get("hasBody")) || !Boolean.TRUE.equals(pack.getFacts().get("isJson"))) {
                    return "JSON body required for field mutation";
                }
                if ("REPLACE_FIELD".equals(m.getType()) && m.getValue() instanceof String s && s.length() > 500) {
                    return "replace value too long";
                }
            }
            case "OMIT_BODY", "INVALID_JSON" -> {
                if (!Boolean.TRUE.equals(pack.getFacts().get("hasBody"))) {
                    return "body required";
                }
            }
            case "OMIT_HEADER" -> {
                if (isBlank(m.getHeader())) {
                    return "header required";
                }
                if (!Boolean.TRUE.equals(pack.getFacts().get("hasHeaders"))) {
                    return "headers required";
                }
            }
            case "SET_CONTENT_TYPE" -> {
                // ok
            }
            case "WRONG_METHOD" -> {
                if (isBlank(m.getMethod())) {
                    return "method required";
                }
                String method = m.getMethod().toUpperCase(Locale.ROOT);
                if (!List.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS").contains(method)) {
                    return "invalid method";
                }
                if (method.equalsIgnoreCase(pack.getMethod())) {
                    return "WRONG_METHOD must differ from original";
                }
                m.setMethod(method);
            }
            default -> {
                return "unsupported mutation";
            }
        }
        return null;
    }

    private static String topLevel(String field) {
        int i = field.indexOf('.');
        return i < 0 ? field : field.substring(0, i);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
