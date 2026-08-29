package vn.nitrogen.architecture.global;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import java.util.List;
import org.junit.jupiter.api.Test;
import vn.nitrogen.architecture.AbstractArchitectureTest;

/**
 * Cấm Lombok sinh equals/hashCode/toString trên entity (§15).
 *
 * <p>{@code @Data}, {@code @EqualsAndHashCode} và {@code @ToString} sinh code
 * đụng tới MỌI field, kể cả association lazy. Gọi {@code toString()} khi debug
 * là kéo nguyên đồ thị đối tượng lên; đưa entity vào {@code HashSet} là nạp
 * lazy proxy vào lúc không ai ngờ. Với {@code open-in-view=false} thì thành
 * {@code LazyInitializationException} tại chỗ ngẫu nhiên.
 *
 * <p>Kiểm theo TÊN annotation, không theo class: dự án hiện chưa phụ thuộc
 * Lombok, và luật này phải chặn được ngay lần đầu ai đó thêm nó.
 */
class NoLombokDataOnEntityTest extends AbstractArchitectureTest {

    private static final List<String> FORBIDDEN_ON_ENTITY = List.of(
            "lombok.Data",
            "lombok.Value",
            "lombok.EqualsAndHashCode",
            "lombok.ToString");

    @Test
    void entitiesShouldNotUseLombokGeneratedIdentity() {
        classes().that()
                .areAnnotatedWith(Entity.class)
                .should(notUseForbiddenLombokAnnotations())
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    private static ArchCondition<JavaClass> notUseForbiddenLombokAnnotations() {
        return new ArchCondition<>("không dùng @Data/@Value/@EqualsAndHashCode/@ToString") {

            @Override
            public void check(JavaClass entity, ConditionEvents events) {
                entity.getAnnotations().stream()
                        .map(annotation -> annotation.getRawType().getName())
                        .filter(FORBIDDEN_ON_ENTITY::contains)
                        .forEach(name -> events.add(SimpleConditionEvent.violated(entity,
                                "%s là @Entity và dùng %s. Viết tay equals/hashCode theo id "
                                        .formatted(entity.getName(), name)
                                        + "(xem AbstractIdentifiableEntity) và không sinh toString "
                                        + "chạm association.")));
            }
        };
    }
}
