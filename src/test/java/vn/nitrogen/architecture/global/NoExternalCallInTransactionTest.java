package vn.nitrogen.architecture.global;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import vn.nitrogen.architecture.AbstractArchitectureTest;

/**
 * Cấm gọi HTTP / RabbitMQ / MinIO bên trong transaction (§4.3.6).
 *
 * <p>Một lời gọi mạng trong transaction giữ connection PostgreSQL suốt thời gian
 * chờ đầu bên kia. Broker chậm 30 giây là pool cạn và toàn bộ request khác chết
 * theo — lỗi lan từ hệ thống phụ sang đường ghi chính.
 *
 * <p>Cách đúng: commit trước, rồi mới gọi ra ngoài. Cần gọi ra ngoài mà vẫn phải
 * nguyên tử thì dùng outbox (§11.4).
 */
class NoExternalCallInTransactionTest extends AbstractArchitectureTest {

    /**
     * Package của client I/O ra ngoài process. Cố ý liệt kê theo package thay vì
     * theo tên class: thêm một client mới của cùng thư viện sẽ tự động bị chặn.
     */
    private static final List<String> EXTERNAL_IO_PACKAGES = List.of(
            "org.springframework.web.client",       // RestTemplate
            "org.springframework.web.reactive.function.client", // WebClient
            "org.springframework.amqp.rabbit.core",  // RabbitTemplate
            "org.springframework.amqp.core",
            "java.net.http",
            "io.minio",
            "software.amazon.awssdk.services.s3");

    @Test
    void transactionalMethodsShouldNotCallExternalSystems() {
        methods().that()
                .areAnnotatedWith(Transactional.class)
                .should(notCallExternalSystems())
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void methodsInTransactionalClassesShouldNotCallExternalSystems() {
        methods().that()
                .areDeclaredInClassesThat().areAnnotatedWith(Transactional.class)
                .should(notCallExternalSystems())
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    private static ArchCondition<JavaMethod> notCallExternalSystems() {
        return new ArchCondition<>("không gọi HTTP/RabbitMQ/MinIO trong transaction") {

            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                for (JavaMethodCall call : method.getMethodCallsFromSelf()) {
                    JavaClass target = call.getTargetOwner();
                    String targetPackage = target.getPackageName();

                    boolean external = EXTERNAL_IO_PACKAGES.stream()
                            .anyMatch(p -> targetPackage.equals(p) || targetPackage.startsWith(p + "."));

                    if (external) {
                        events.add(SimpleConditionEvent.violated(method,
                                "%s gọi %s trong transaction. Commit trước rồi mới gọi ra ngoài, "
                                        .formatted(method.getFullName(), target.getName())
                                        + "hoặc dùng outbox nếu cần nguyên tử."));
                    }
                }
            }
        };
    }
}
