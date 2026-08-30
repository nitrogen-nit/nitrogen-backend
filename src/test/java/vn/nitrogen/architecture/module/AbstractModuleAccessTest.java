package vn.nitrogen.architecture.module;

import java.util.Set;
import org.junit.jupiter.api.Test;
import vn.nitrogen.architecture.AbstractArchitectureTest;
import vn.nitrogen.architecture.rules.ArchitectureRules;

/**
 * Luật boundary quan trọng nhất (§4.3).
 *
 * <p>Class NGOÀI module chỉ được phụ thuộc vào {@code api/}, {@code dto/} hoặc
 * {@code events/} của module đó.
 *
 * <p>Chặt hơn Artemis một bậc có chủ đích: Artemis còn cho phép truy cập
 * {@code domain/} xuyên module. Nitrogen cấm, vì §4.3 mục 3–4 quy định không
 * import domain entity của module khác và không map JPA association xuyên
 * module — chỉ lưu foreign key UUID rồi tra cứu qua API khi cần. Cho phép
 * {@code domain/} sẽ mở lại đúng cánh cửa đó.
 */
public abstract class AbstractModuleAccessTest extends AbstractArchitectureTest implements ModuleArchitectureTest {

    /**
     * Class ngoại lệ được phép chạm vào phần nội bộ của module. Ghi tường minh
     * và kèm lý do — danh sách này dài ra là dấu hiệu boundary đang xói mòn.
     */
    protected Set<Class<?>> getIgnoredClasses() {
        return Set.of();
    }

    @Test
    void shouldOnlyAccessPublicModuleInterfaces() {
        ArchitectureRules.onlyAccessPublicModuleInterfaces(ROOT_PACKAGE, getModulePackage(), getIgnoredClasses())
                .check(productionClasses);
    }
}
