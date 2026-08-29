package vn.nitrogen.assessment.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Luật tính điểm của một question type.
 *
 * @param rule          payload JSONB khớp contracts/json-schema/scoring-rule
 * @param schemaVersion version của {@code rule}; grader chọn adapter theo đây
 */
public record ScoringPolicySummary(
        UUID id, String policyCode, int schemaVersion, Map<String, Object> rule) {
}
