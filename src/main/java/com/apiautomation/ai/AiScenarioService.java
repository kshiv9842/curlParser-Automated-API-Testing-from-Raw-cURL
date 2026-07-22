package com.apiautomation.ai;

import com.apiautomation.ParsedCurl;
import com.apiautomation.report.ScenarioResult;
import com.apiautomation.testcase.RequestFacts;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates: ContextPack → generate → validate (optional 1 repair via re-shape) → execute.
 */
public final class AiScenarioService {

    private final AiScenarioGenerator primary;
    private final AiScenarioGenerator fallback;

    public AiScenarioService() {
        this.primary = new CursorSdkScenarioGenerator();
        this.fallback = new ShapeBasedScenarioGenerator();
    }

    public AiRunResult runAiScenarios(String curlCommand, ParsedCurl parsed, RequestFacts facts) {
        ContextPack pack = ContextPackFactory.from(parsed, facts);
        AiScenarioGenerator.GenerationOutcome outcome = primary.propose(pack);

        if (outcome.getProposed().isEmpty()) {
            // One fallback path — still goes through the same validator
            AiScenarioGenerator.GenerationOutcome shape = fallback.propose(pack);
            outcome = new AiScenarioGenerator.GenerationOutcome(
                    shape.getProposed(),
                    shape.getSource(),
                    outcome.getMessage() + " | fallback: " + shape.getMessage()
            );
        }

        AiScenarioValidator.Result validated = AiScenarioValidator.validate(outcome.getProposed(), pack);

        // Single "repair" attempt: if SDK returned junk and nothing accepted, try shape fallback once
        if (validated.getAccepted().isEmpty() && "AI_AGENT".equals(outcome.getSource())) {
            AiScenarioGenerator.GenerationOutcome shape = fallback.propose(pack);
            validated = AiScenarioValidator.validate(shape.getProposed(), pack);
            outcome = new AiScenarioGenerator.GenerationOutcome(
                    shape.getProposed(),
                    "SMART_ASSIST",
                    outcome.getMessage() + " | assist-fallback: " + shape.getMessage()
            );
        }

        List<ScenarioResult> results = new ArrayList<>();
        String sourceTag = outcome.getSource();
        for (AiScenarioSpec spec : validated.getAccepted()) {
            results.add(AiScenarioExecutor.run(curlCommand, spec, sourceTag));
        }

        return new AiRunResult(
                outcome.getProposed().size(),
                validated.getAccepted().size(),
                validated.getRejected().size(),
                validated.getRejected(),
                outcome.getMessage(),
                sourceTag,
                results
        );
    }

    public static final class AiRunResult {
        private final int proposed;
        private final int accepted;
        private final int rejected;
        private final List<String> rejectReasons;
        private final String message;
        private final String source;
        private final List<ScenarioResult> results;

        public AiRunResult(int proposed, int accepted, int rejected, List<String> rejectReasons,
                           String message, String source, List<ScenarioResult> results) {
            this.proposed = proposed;
            this.accepted = accepted;
            this.rejected = rejected;
            this.rejectReasons = rejectReasons;
            this.message = message;
            this.source = source;
            this.results = results;
        }

        public int getProposed() {
            return proposed;
        }

        public int getAccepted() {
            return accepted;
        }

        public int getRejected() {
            return rejected;
        }

        public List<String> getRejectReasons() {
            return rejectReasons;
        }

        public String getMessage() {
            return message;
        }

        public String getSource() {
            return source;
        }

        public List<ScenarioResult> getResults() {
            return results;
        }
    }
}
