package vn.nitrogen.architecture.module;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import vn.nitrogen.architecture.AbstractArchitectureTest;
import vn.nitrogen.architecture.NitrogenModules;
import vn.nitrogen.architecture.rules.ArchitectureRules;

/**
 * Cấm JPA association xuyên module (§4.3 mục 4, §15.1).
 *
 * <p>Một {@code @ManyToOne} trỏ sang entity của module khác biến hai module
 * thành một aggregate ở tầng Hibernate: lazy load đi xuyên ranh giới, cascade
 * đi xuyên ranh giới, và ranh giới transaction không còn nghĩa. Cách đúng là
 * lưu {@code UUID} rồi gọi module API khi thật sự cần dữ liệu nghiệp vụ.
 *
 * <p>Database VẪN có FK cross-schema để bảo vệ toàn vẹn dữ liệu (§7.1) — luật
 * này chỉ nói về mapping JPA.
 */
public abstract class AbstractModuleEntityTest extends AbstractArchitectureTest
        implements ModuleArchitectureTest {

    @Test
    void entitiesShouldResideInDomainPackage() {
        classes().that()
                .areAnnotatedWith(Entity.class)
                .and().resideInAPackage(getModuleWithSubpackage())
                .should().resideInAPackage(getModuleDomainSubpackage())
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void entitiesShouldNotMapAssociationsAcrossModules() {
        ArchitectureRules.entitiesShouldNotReferenceOtherModuleEntities(ROOT_PACKAGE, getModulePackage())
                .check(productionClasses);
    }

    @Test
    void moduleReferencesShouldUseUuid() {
        ArchitectureRules.moduleReferencesShouldUseUuid(
                        getModulePackage(), NitrogenModules.BUSINESS_MODULE_NAMES)
                .check(productionClasses);
    }
}
