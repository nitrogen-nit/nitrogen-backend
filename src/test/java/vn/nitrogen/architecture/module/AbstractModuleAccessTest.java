package vn.nitrogen.architecture.module;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import vn.nitrogen.architecture.AbstractArchitectureTest;

/**
 * Luật boundary quan trọng nhất (§4.3).
 *
 * <p>Class NGOÀI module chỉ được phụ thuộc vào {@code api/} và {@code dto/} của
 * module đó.
 *
 * <p>Chặt hơn Artemis một bậc có chủ đích: Artemis còn cho phép truy cập
 * {@code domain/} xuyên module. Nitrogen cấm, vì §4.3 mục 3–4 quy định không
 * import domain entity của module khác và không map JPA association xuyên
 * module — chỉ lưu foreign key UUID rồi tra cứu qua API khi cần. Cho phép
 * {@code domain/} sẽ mở lại đúng cánh cửa đó.
 */
public abstract class AbstractModuleAccessTest extends AbstractArchitectureTest implements ModuleArchitectureTest {

    /**
     * Class ngoại lệ được phép chạm vào phần nội bộ của module. Ghi tường minh
     * và kèm lý do — danh sách này dài ra là dấu hiệu boundary đang xói mòn.
     */
    protected Set<Class<?>> getIgnoredClasses() {
        return Set.of();
    }

    @Test
    void shouldOnlyAccessApiAndDto() {
        ArchCondition<JavaClass> onlyAllowedDependencies =
                new ArchCondition<>("chỉ phụ thuộc vào api/ và dto/ của module khác") {

                    @Override
                    public void check(JavaClass origin, ConditionEvents events) {
                        List<Dependency> targetsInModule = origin.getDirectDependenciesFromSelf().stream()
                                .filter(d -> resideInAPackage(getModuleWithSubpackage()).test(d.getTargetClass()))
                                .toList();

                        for (Dependency dependency : targetsInModule) {
                            JavaClass target = dependency.getTargetClass();

                            boolean allowed = resideInAnyPackage(getModuleApiSubpackage(), getModuleDtoSubpackage())
                                    .test(target);
                            if (allowed) {
                                continue;
                            }
                            if (getIgnoredClasses().stream().anyMatch(c -> c.getName().equals(target.getName()))) {
                                continue;
                            }

                            events.add(SimpleConditionEvent.violated(origin,
                                    "%s phụ thuộc vào %s — nằm ngoài api/ và dto/ của module %s"
                                            .formatted(origin.getName(), target.getName(), getModulePackage())));
                        }
                    }
                };

        classes().that()
                .resideInAPackage(ROOT_PACKAGE + "..")
                .and().resideOutsideOfPackage(getModuleWithSubpackage())
                // common và platform là shared kernel — chúng không phải module
                // nghiệp vụ nên không chịu luật boundary này.
                .and().resideOutsideOfPackage(ROOT_PACKAGE + ".common..")
                .and().resideOutsideOfPackage(ROOT_PACKAGE + ".platform..")
                .should(onlyAllowedDependencies)
                .allowEmptyShould(true)
                .check(productionClasses);
    }
}
