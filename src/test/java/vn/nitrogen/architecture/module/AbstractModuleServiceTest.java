package vn.nitrogen.architecture.module;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nitrogen.architecture.AbstractArchitectureTest;

/**
 * Service là transaction owner (§4.3, §6.2).
 *
 * <p>Một {@code @Service} phải khai báo {@code @Transactional} — ở class hoặc ở
 * ít nhất một method public. Nếu ranh giới transaction trôi lên controller hoặc
 * xuống repository thì không ai còn đọc được một use case commit tới đâu.
 */
public abstract class AbstractModuleServiceTest extends AbstractArchitectureTest
        implements ModuleArchitectureTest {

    @Test
    void servicesShouldResideInServicePackage() {
        classes().that()
                .areAnnotatedWith(Service.class)
                .and().resideInAPackage(getModuleWithSubpackage())
                .should().resideInAPackage(getModuleServiceSubpackage())
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void servicesShouldDeclareTransactionBoundary() {
        classes().that()
                .areAnnotatedWith(Service.class)
                .and().resideInAPackage(getModuleServiceSubpackage())
                .should(declareTransactionBoundary())
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void repositoriesShouldNotBeTransactional() {
        // Transaction mở tại repository khiến mỗi lời gọi là một transaction
        // riêng — use case gồm nhiều lời gọi sẽ mất tính nguyên tử.
        noClasses().that()
                .resideInAPackage(getModuleRepositorySubpackage())
                .should().beAnnotatedWith(Transactional.class)
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    private static ArchCondition<JavaClass> declareTransactionBoundary() {
        return new ArchCondition<>("khai báo @Transactional ở class hoặc ở method public") {

            @Override
            public void check(JavaClass service, ConditionEvents events) {
                boolean onClass = service.isAnnotatedWith(Transactional.class);
                boolean onPublicMethod = service.getMethods().stream()
                        .filter(m -> m.getModifiers().contains(JavaModifier.PUBLIC))
                        .anyMatch(m -> m.isAnnotatedWith(Transactional.class));

                if (!onClass && !onPublicMethod) {
                    events.add(SimpleConditionEvent.violated(service,
                            "%s là @Service nhưng không khai báo ranh giới transaction ở đâu cả"
                                    .formatted(service.getName())));
                }
            }
        };
    }
}
