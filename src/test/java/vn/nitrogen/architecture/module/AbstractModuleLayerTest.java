package vn.nitrogen.architecture.module;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.Test;
import vn.nitrogen.architecture.AbstractArchitectureTest;
import vn.nitrogen.common.api.ModuleApi;

/**
 * Chiều phụ thuộc bên trong một module (§4.3 mục 2).
 *
 * <p>{@code web → service → repository → domain}, một chiều.
 *
 * <p>Luật về {@code web/} và {@code repository/} nằm ở
 * {@link AbstractModuleWebTest} và {@link AbstractModuleRepositoryTest}; ở đây
 * chỉ giữ phần chưa được hai lớp đó phủ.
 */
public abstract class AbstractModuleLayerTest extends AbstractArchitectureTest
        implements ModuleArchitectureTest {

    @Test
    void domainShouldNotDependOnOuterLayers() {
        // Domain là tầng trong cùng. Biết tới repository hay web nghĩa là quy tắc
        // nghiệp vụ đã dính vào cách lưu trữ và cách phơi ra HTTP.
        noClasses().that()
                .resideInAPackage(getModuleDomainSubpackage())
                .should().dependOnClassesThat()
                .resideInAnyPackage(getModuleRepositorySubpackage(), getModuleWebSubpackage())
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void moduleApisShouldResideInApiPackage() {
        // Facade phải ở đúng một nơi, nếu không AbstractModuleAccessTest sẽ cho
        // qua một cửa mà nó tưởng là nội bộ.
        classes().that()
                .areAssignableTo(ModuleApi.class)
                .and().resideInAPackage(getModuleWithSubpackage())
                .should().resideInAPackage(getModuleApiSubpackage())
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void apiShouldNotDependOnWeb() {
        // api/ phục vụ module khác, web/ phục vụ HTTP client. Facade phụ thuộc
        // web/ nghĩa là hai bề mặt đã dính vào nhau.
        noClasses().that()
                .resideInAPackage(getModuleApiSubpackage())
                .should().dependOnClassesThat().resideInAPackage(getModuleWebSubpackage())
                .allowEmptyShould(true)
                .check(productionClasses);
    }
}
