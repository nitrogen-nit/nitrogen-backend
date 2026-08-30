package vn.nitrogen.architecture.global;

import org.junit.jupiter.api.Test;
import vn.nitrogen.architecture.AbstractArchitectureTest;
import vn.nitrogen.architecture.rules.ArchitectureRules;

class PersistenceAccessTest extends AbstractArchitectureTest {

    @Test
    void entityManagerShouldOnlyBeUsedByCustomRepositoryImplementations() {
        ArchitectureRules.noEntityManagerOutsideCustomRepositoryImplementation(ROOT_PACKAGE)
                .check(productionClasses);
    }
}
