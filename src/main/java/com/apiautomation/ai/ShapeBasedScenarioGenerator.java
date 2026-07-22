package com.apiautomation.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic smart-assist proposals when AI Agents are unavailable.
 * Same schema + validator — not free-form hallucination.
 */
public final class ShapeBasedScenarioGenerator implements AiScenarioGenerator {

    @Override
    public GenerationOutcome propose(ContextPack pack) {
        List<AiScenarioSpec> out = new ArrayList<>();

        if (Boolean.TRUE.equals(pack.getFacts().get("isJson")) && !pack.getFields().isEmpty()) {
            for (String field : pack.getFields()) {
                if (out.size() >= pack.getMaxScenarios()) {
                    break;
                }
                String idSafe = field.replace('.', '_').replaceAll("[^a-zA-Z0-9_]", "").toLowerCase(Locale.ROOT);
                if (idSafe.isBlank()) {
                    continue;
                }
                AiScenarioSpec omit = new AiScenarioSpec();
                omit.setId("ai_omit_" + idSafe);
                omit.setObjective("Identify if API accepts request when required-looking field '" + field + "' is omitted.");
                omit.setExpectedResult("HTTP 400 or 422 (reject missing field); not 2xx or 5xx");
                omit.setRisk("P1");
                omit.setOracle("REJECT");
                omit.setRationale("Shape-based missing field");
                omit.setRequiresFacts(List.of("hasBody", "isJson"));
                AiMutation m = new AiMutation();
                m.setType("OMIT_FIELD");
                m.setField(field);
                omit.setMutation(m);
                out.add(omit);

                if (out.size() >= pack.getMaxScenarios()) {
                    break;
                }

                // Login-ish: wrong password / invalid email format
                String lower = field.toLowerCase(Locale.ROOT);
                if (lower.contains("password") || lower.equals("pass") || lower.equals("pwd")) {
                    AiScenarioSpec wrong = new AiScenarioSpec();
                    wrong.setId("ai_wrong_" + idSafe);
                    wrong.setObjective("Identify if API rejects an incorrect password value.");
                    wrong.setExpectedResult("HTTP 401 or 403");
                    wrong.setRisk("P0");
                    wrong.setOracle("REJECT");
                    wrong.setRationale("Auth credential mismatch");
                    wrong.setRequiresFacts(List.of("hasBody", "isJson"));
                    AiMutation rm = new AiMutation();
                    rm.setType("REPLACE_FIELD");
                    rm.setField(field);
                    rm.setValue("invalid_password_for_bug_check");
                    wrong.setMutation(rm);
                    out.add(wrong);
                } else if (lower.contains("email")) {
                    AiScenarioSpec badEmail = new AiScenarioSpec();
                    badEmail.setId("ai_bademail_" + idSafe);
                    badEmail.setObjective("Identify if API rejects an invalid email format.");
                    badEmail.setExpectedResult("HTTP 400 or 422");
                    badEmail.setRisk("P1");
                    badEmail.setOracle("REJECT");
                    badEmail.setRationale("Email format validation");
                    badEmail.setRequiresFacts(List.of("hasBody", "isJson"));
                    AiMutation rm = new AiMutation();
                    rm.setType("REPLACE_FIELD");
                    rm.setField(field);
                    rm.setValue("not-an-email");
                    badEmail.setMutation(rm);
                    out.add(badEmail);
                }
            }
        }

        if (out.isEmpty() && Boolean.TRUE.equals(pack.getFacts().get("hasAuth"))) {
            AiScenarioSpec invalidAuth = new AiScenarioSpec();
            invalidAuth.setId("ai_invalid_auth_header");
            invalidAuth.setObjective("Identify if API rejects a garbled Authorization header.");
            invalidAuth.setExpectedResult("HTTP 401 or 403");
            invalidAuth.setRisk("P0");
            invalidAuth.setOracle("REJECT");
            invalidAuth.setRationale("Invalid auth value");
            invalidAuth.setRequiresFacts(List.of("hasAuth"));
            AiMutation m = new AiMutation();
            m.setType("OMIT_HEADER");
            m.setHeader("Authorization");
            // REPLACE via OMIT is weak; use SET by replacing in executor — use REPLACE pattern via OMIT_HEADER
            // Better: custom — we'll use OMIT_HEADER for invalid by executor setting garbage — add REPLACE_HEADER?
            // Keep OMIT_HEADER — missing auth already in catalog. Skip if covered.
            invalidAuth.setMutation(m);
            // Don't add if duplicate of missing_auth intent — skip
        }

        String msg = out.isEmpty()
                ? "No smart-assist proposals for this request"
                : "Generated " + out.size() + " smart-assist proposals (AI Agents unavailable — using field-shape rules)";
        return new GenerationOutcome(out, "SMART_ASSIST", msg);
    }
}
