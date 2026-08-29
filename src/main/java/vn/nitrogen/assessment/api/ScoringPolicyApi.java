package vn.nitrogen.assessment.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import vn.nitrogen.assessment.dto.ScoringPolicySummary;
import vn.nitrogen.common.api.ModuleApi;

/**
 * Cấp luật tính điểm cho grader.
 *
 * <p>Tách khỏi {@link ExerciseVersionApi} vì scoring policy là dữ liệu immutable
 * cache được, còn version câu hỏi thì không cùng vòng đời.
 *
 * <p>TODO: inject {@code ScoringPolicyRepository} nội bộ module và hiện thực.
 */
@Profile("core")
@Controller
@Lazy
public class ScoringPolicyApi implements ModuleApi {

    public Optional<ScoringPolicySummary> findById(UUID scoringPolicyVersionId) {
        throw new UnsupportedOperationException("TODO: chưa hiện thực ScoringPolicyApi#findById");
    }

    public List<ScoringPolicySummary> findAllById(Collection<UUID> scoringPolicyVersionIds) {
        throw new UnsupportedOperationException("TODO: chưa hiện thực ScoringPolicyApi#findAllById");
    }
}
