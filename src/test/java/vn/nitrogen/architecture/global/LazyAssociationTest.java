package vn.nitrogen.architecture.global;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import org.junit.jupiter.api.Test;
import vn.nitrogen.architecture.AbstractArchitectureTest;

/**
 * {@code @ManyToOne} và {@code @OneToOne} phải khai báo LAZY rõ ràng (§15).
 *
 * <p>Mặc định của JPA cho hai annotation này là EAGER — một mặc định im lặng,
 * và nó là nguồn N+1 phổ biến nhất. Với {@code open-in-view=false}, EAGER còn
 * kéo theo query thừa ngay trong transaction thay vì lỗi rõ ràng.
 *
 * <p>Luật đòi khai báo tường minh chứ không chỉ "không EAGER": đọc code phải
 * thấy ngay chiến lược fetch, không phải nhớ mặc định của spec.
 */
class LazyAssociationTest extends AbstractArchitectureTest {

    @Test
    void toOneAssociationsShouldBeExplicitlyLazy() {
        fields().should(beExplicitlyLazy())
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    private static ArchCondition<JavaField> beExplicitlyLazy() {
        return new ArchCondition<>("khai báo fetch = FetchType.LAZY trên @ManyToOne/@OneToOne") {

            @Override
            public void check(JavaField field, ConditionEvents events) {
                field.tryGetAnnotationOfType(ManyToOne.class)
                        .ifPresent(a -> checkFetch(field, a.fetch(), "@ManyToOne", events));
                field.tryGetAnnotationOfType(OneToOne.class)
                        .ifPresent(a -> checkFetch(field, a.fetch(), "@OneToOne", events));
            }

            private void checkFetch(JavaField field, FetchType fetch, String annotation,
                    ConditionEvents events) {
                if (fetch != FetchType.LAZY) {
                    events.add(SimpleConditionEvent.violated(field,
                            "%s khai báo %s với fetch = %s. Phải là LAZY; chữa N+1 bằng "
                                    .formatted(field.getFullName(), annotation, fetch)
                                    + "JOIN FETCH, EntityGraph hoặc DTO projection, không bằng EAGER."));
                }
            }
        };
    }
}
