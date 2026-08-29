package vn.nitrogen.architecture.module;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import org.junit.jupiter.api.Test;
import vn.nitrogen.architecture.AbstractArchitectureTest;

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
        fields().that()
                .areDeclaredInClassesThat().resideInAPackage(getModuleDomainSubpackage())
                .should(targetOwnModuleOnly())
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    private ArchCondition<JavaField> targetOwnModuleOnly() {
        return new ArchCondition<>("không map @ManyToOne/@OneToOne sang module khác") {

            @Override
            public void check(JavaField field, ConditionEvents events) {
                boolean isAssociation = field.isAnnotatedWith(ManyToOne.class)
                        || field.isAnnotatedWith(OneToOne.class);
                if (!isAssociation) {
                    return;
                }

                String targetPackage = field.getRawType().getPackageName();
                boolean insideProject = targetPackage.startsWith(ROOT_PACKAGE);
                boolean insideOwnModule = targetPackage.startsWith(getModulePackage());

                if (insideProject && !insideOwnModule) {
                    events.add(SimpleConditionEvent.violated(field,
                            "%s map association sang %s — xuyên ranh giới module %s. "
                                    .formatted(field.getFullName(), field.getRawType().getName(),
                                            getModulePackage())
                                    + "Lưu UUID và tra qua module API thay vì mapping."));
                }
            }
        };
    }
}
