package vn.nitrogen.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import vn.nitrogen.NitrogenApplication;

/**
 * Spring Modulith kiểm cấu trúc module dựa trên {@code @ApplicationModule} và
 * {@code allowedDependencies} khai báo trong từng {@code package-info.java}.
 *
 * <p>Bổ sung cho ArchUnit chứ không thay thế: Modulith bắt phụ thuộc vòng và
 * phụ thuộc không khai báo giữa các module; ArchUnit bắt vi phạm chi tiết hơn
 * bên trong module (tầng, entity, transaction).
 */
class ModularityTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(NitrogenApplication.class);

    @Test
    void verifiesModularStructure() {
        MODULES.verify();
    }

    /**
     * Sinh tài liệu module (PlantUML + bảng) vào {@code target/spring-modulith-docs}.
     * Không phải assertion — nhưng giữ ở đây để tài liệu luôn khớp code thay vì
     * được vẽ tay rồi lạc hậu.
     */
    @Test
    void writeDocumentationSnippets() {
        new Documenter(MODULES)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }
}
