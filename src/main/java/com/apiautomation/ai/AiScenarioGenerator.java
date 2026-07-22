package com.apiautomation.ai;

import java.util.List;

public interface AiScenarioGenerator {
    /**
     * Propose raw scenarios (may include invalid ones). Validator runs next.
     */
    GenerationOutcome propose(ContextPack pack);

    class GenerationOutcome {
        private final List<AiScenarioSpec> proposed;
        private final String source;
        private final String message;

        public GenerationOutcome(List<AiScenarioSpec> proposed, String source, String message) {
            this.proposed = proposed;
            this.source = source;
            this.message = message;
        }

        public List<AiScenarioSpec> getProposed() {
            return proposed;
        }

        public String getSource() {
            return source;
        }

        public String getMessage() {
            return message;
        }
    }
}
