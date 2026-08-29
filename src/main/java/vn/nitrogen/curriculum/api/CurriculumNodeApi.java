package vn.nitrogen.curriculum.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import vn.nitrogen.common.api.ModuleApi;
import vn.nitrogen.curriculum.dto.CurriculumNodeSummary;

/**
 * Tra cứu node chương trình cho Practice (attempt_topic_results.topic_node_id)
 * và Progress (projection theo chủ đề).
 *
 * <p>TODO: inject {@code CurriculumNodeRepository} nội bộ module và hiện thực.
 */
@Profile("core")
@Controller
@Lazy
public class CurriculumNodeApi implements ModuleApi {

    public Optional<CurriculumNodeSummary> findById(UUID nodeId) {
        throw new UnsupportedOperationException("TODO: chưa hiện thực CurriculumNodeApi#findById");
    }

    public List<CurriculumNodeSummary> findAllById(Collection<UUID> nodeIds) {
        throw new UnsupportedOperationException("TODO: chưa hiện thực CurriculumNodeApi#findAllById");
    }

    /** Đường dẫn từ gốc tới node, dùng để cộng dồn tiến độ lên node cha. */
    public List<CurriculumNodeSummary> findAncestors(UUID nodeId) {
        throw new UnsupportedOperationException("TODO: chưa hiện thực CurriculumNodeApi#findAncestors");
    }
}
