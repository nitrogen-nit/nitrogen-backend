package vn.nitrogen.architecture.global;

import org.junit.jupiter.api.Test;
import vn.nitrogen.architecture.AbstractArchitectureTest;
import vn.nitrogen.architecture.rules.ArchitectureRules;

class RestControllerConventionTest extends AbstractArchitectureTest {

    @Test
    void restControllersShouldBeStateless() {
        ArchitectureRules.restControllersShouldBeStateless().check(productionClasses);
    }

    @Test
    void restControllersShouldNotUseRepositoriesOrEntityManager() {
        ArchitectureRules.restControllersShouldNotDependOnRepositoriesOrEntityManagers()
                .check(productionClasses);
    }

    @Test
    void restControllerSignaturesShouldUseDtosInsteadOfEntities() {
        ArchitectureRules.restControllerMethodsShouldNotExposeEntities(ROOT_PACKAGE)
                .check(productionClasses);
    }
}
