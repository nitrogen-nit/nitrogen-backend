package vn.nitrogen.architecture.global;

import org.junit.jupiter.api.Test;
import vn.nitrogen.architecture.AbstractArchitectureTest;
import vn.nitrogen.architecture.rules.ArchitectureRules;

class CodingConventionTest extends AbstractArchitectureTest {

    @Test
    void productionCodeShouldNotUseFieldInjection() {
        ArchitectureRules.noFieldInjection(ROOT_PACKAGE).check(productionClasses);
    }
}
