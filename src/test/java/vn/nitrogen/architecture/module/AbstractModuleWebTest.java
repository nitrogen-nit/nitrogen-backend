package vn.nitrogen.architecture.module;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import vn.nitrogen.architecture.AbstractArchitectureTest;
import vn.nitrogen.architecture.rules.ArchitectureRules;

/**
 * Controller mỏng và không mở transaction (§4.3).
 *
 * <p>Controller mở transaction là đường dẫn thẳng tới việc giữ connection suốt
 * thời gian serialize response, và tới lazy loading ngoài ý muốn — đúng thứ mà
 * {@code open-in-view=false} (§15) dựng lên để chặn.
 */
public abstract class AbstractModuleWebTest extends AbstractArchitectureTest
        implements ModuleArchitectureTest {

    @Test
    void webShouldNotAccessRepositoryDirectly() {
        ArchitectureRules.webShouldNotAccessOwnRepository(getModulePackage())
                .check(productionClasses);
    }

    @Test
    void controllerClassesShouldNotBeTransactional() {
        noClasses().that()
                .resideInAPackage(getModuleWebSubpackage())
                .should().beAnnotatedWith(Transactional.class)
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void controllerMethodsShouldNotBeTransactional() {
        methods().that()
                .areDeclaredInClassesThat().resideInAPackage(getModuleWebSubpackage())
                .should().notBeAnnotatedWith(Transactional.class)
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void restControllersShouldResideInWebPackage() {
        ArchitectureRules.restControllersShouldResideInWebPackage(getModulePackage())
                .check(productionClasses);
    }

    @Test
    void webShouldNotExposeDomainEntities() {
        // §4.3: Controller chỉ nhận/trả DTO. Entity lọt ra ngoài kéo theo lazy
        // proxy và khoá chặt hình dạng JSON vào hình dạng bảng.
        ArchitectureRules.webShouldNotExposeOwnDomain(getModulePackage())
                .check(productionClasses);
    }
}
