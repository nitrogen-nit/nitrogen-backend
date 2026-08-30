package vn.nitrogen.architecture.global;

import org.junit.jupiter.api.Test;
import vn.nitrogen.architecture.AbstractArchitectureTest;
import vn.nitrogen.architecture.rules.ArchitectureRules;

class TransactionBoundaryTest extends AbstractArchitectureTest {

    @Test
    void transactionalClassesShouldOnlyLiveInServicePackages() {
        ArchitectureRules.transactionalClassesShouldResideInServicePackage(ROOT_PACKAGE)
                .check(productionClasses);
    }

    @Test
    void transactionalMethodsShouldOnlyLiveInServicePackages() {
        ArchitectureRules.transactionalMethodsShouldResideInServicePackage(ROOT_PACKAGE)
                .check(productionClasses);
    }
}
