package vn.nitrogen.architecture.module;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import vn.nitrogen.architecture.AbstractArchitectureTest;

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
        noClasses().that()
                .resideInAPackage(getModuleWebSubpackage())
                .should().dependOnClassesThat().resideInAPackage(getModuleRepositorySubpackage())
                .allowEmptyShould(true)
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
        classes().that()
                .areAnnotatedWith(RestController.class)
                .and().resideInAPackage(getModuleWithSubpackage())
                .should().resideInAPackage(getModuleWebSubpackage())
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void webShouldNotExposeDomainEntities() {
        // §4.3: Controller chỉ nhận/trả DTO. Entity lọt ra ngoài kéo theo lazy
        // proxy và khoá chặt hình dạng JSON vào hình dạng bảng.
        noClasses().that()
                .resideInAPackage(getModuleWebSubpackage())
                .should().dependOnClassesThat().resideInAPackage(getModuleDomainSubpackage())
                .allowEmptyShould(true)
                .check(productionClasses);
    }
}
