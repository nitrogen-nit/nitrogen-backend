package vn.nitrogen.architecture.curriculum;

import org.junit.jupiter.api.Nested;
import vn.nitrogen.architecture.module.AbstractModuleAccessTest;
import vn.nitrogen.architecture.module.AbstractModuleEntityTest;
import vn.nitrogen.architecture.module.AbstractModuleLayerTest;
import vn.nitrogen.architecture.module.AbstractModuleRepositoryTest;
import vn.nitrogen.architecture.module.AbstractModuleServiceTest;
import vn.nitrogen.architecture.module.AbstractModuleWebTest;

/**
 * Toàn bộ luật kiến trúc áp cho module {@code curriculum}.
 *
 * <p>File này cố ý không chứa luật nào: luật sống trong các
 * {@code AbstractModule*Test}. Thêm module mới ⇒ copy file này, đổi tên package.
 * Thêm luật mới ⇒ sửa một chỗ, 12 module chịu ngay.
 */
class CurriculumArchitectureTest {

    private static final String MODULE_PACKAGE = "vn.nitrogen.curriculum";

    @Nested
    class Access extends AbstractModuleAccessTest {
        @Override
        public String getModulePackage() {
            return MODULE_PACKAGE;
        }
    }

    @Nested
    class Repository extends AbstractModuleRepositoryTest {
        @Override
        public String getModulePackage() {
            return MODULE_PACKAGE;
        }
    }

    @Nested
    class Web extends AbstractModuleWebTest {
        @Override
        public String getModulePackage() {
            return MODULE_PACKAGE;
        }
    }

    @Nested
    class Service extends AbstractModuleServiceTest {
        @Override
        public String getModulePackage() {
            return MODULE_PACKAGE;
        }
    }

    @Nested
    class Layer extends AbstractModuleLayerTest {
        @Override
        public String getModulePackage() {
            return MODULE_PACKAGE;
        }
    }

    @Nested
    class Entity extends AbstractModuleEntityTest {
        @Override
        public String getModulePackage() {
            return MODULE_PACKAGE;
        }
    }
}
