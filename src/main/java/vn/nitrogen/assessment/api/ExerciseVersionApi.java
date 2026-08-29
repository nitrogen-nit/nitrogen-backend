package vn.nitrogen.assessment.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import vn.nitrogen.assessment.dto.ExerciseVersionSummary;
import vn.nitrogen.common.api.ModuleApi;

/**
 * Cấp version câu hỏi đã publish cho Practice và Examination snapshot.
 *
 * <p>TODO: inject {@code ExerciseVersionRepository} nội bộ module và hiện thực.
 */
@Profile("core")
@Controller
@Lazy
public class ExerciseVersionApi implements ModuleApi {

    public Optional<ExerciseVersionSummary> findById(UUID exerciseVersionId) {
        throw new UnsupportedOperationException("TODO: chưa hiện thực ExerciseVersionApi#findById");
    }

    /**
     * Nạp toàn bộ version cho một attempt trong đúng một query.
     * Submit 40 câu không được sinh N+1 (§15.2).
     */
    public List<ExerciseVersionSummary> findAllById(Collection<UUID> exerciseVersionIds) {
        throw new UnsupportedOperationException("TODO: chưa hiện thực ExerciseVersionApi#findAllById");
    }
}
