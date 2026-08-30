package vn.nitrogen.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Tag;

/**
 * Nền cho mọi test kiến trúc.
 *
 * <p>Import class một lần rồi dùng chung: quét bytecode là phần đắt nhất, và bộ
 * test này chạy trên mọi module nên nếu mỗi lớp con tự quét thì thời gian build
 * tăng theo cấp số nhân số module.
 */
@Tag("architecture")
public abstract class AbstractArchitectureTest {

    protected static final String ROOT_PACKAGE = "vn.nitrogen";

    protected static final JavaClasses productionClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
            .importPackages(ROOT_PACKAGE);

    protected static final JavaClasses allClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
            .importPackages(ROOT_PACKAGE);
}
