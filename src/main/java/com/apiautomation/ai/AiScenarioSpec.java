package com.apiautomation.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AiScenarioSpec {
    public static final Set<String> ALLOWED_ORACLES = Set.of("ACCEPT", "REJECT", "OBSERVE");
    public static final Set<String> ALLOWED_RISKS = Set.of("P0", "P1", "P2");

    private String id;
    private String objective;
    private String expectedResult;
    private String risk;
    private String oracle;
    private AiMutation mutation;
    private String rationale;
    private List<String> requiresFacts = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getRisk() {
        return risk;
    }

    public void setRisk(String risk) {
        this.risk = risk;
    }

    public String getOracle() {
        return oracle;
    }

    public void setOracle(String oracle) {
        this.oracle = oracle;
    }

    public AiMutation getMutation() {
        return mutation;
    }

    public void setMutation(AiMutation mutation) {
        this.mutation = mutation;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public List<String> getRequiresFacts() {
        return requiresFacts;
    }

    public void setRequiresFacts(List<String> requiresFacts) {
        this.requiresFacts = requiresFacts;
    }
}
