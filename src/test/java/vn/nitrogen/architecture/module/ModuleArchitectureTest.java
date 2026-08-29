package vn.nitrogen.architecture.module;

/**
 * Hợp đồng cho bộ test kiến trúc của một module.
 *
 * <p>Khuôn theo Artemis: mọi luật viết một lần trong các {@code AbstractModule*Test},
 * mỗi module chỉ cần một lớp mỏng khai báo package của mình. Thêm module mới
 * ⇒ thêm một file ~10 dòng và nó tự động chịu toàn bộ luật.
 */
public interface ModuleArchitectureTest {

    /** Ví dụ: {@code "vn.nitrogen.practice"}. */
    String getModulePackage();

    default String getModuleWithSubpackage() {
        return getModulePackage() + "..";
    }

    default String getModuleApiSubpackage() {
        return getModulePackage() + ".api..";
    }

    default String getModuleDtoSubpackage() {
        return getModulePackage() + ".dto..";
    }

    default String getModuleDomainSubpackage() {
        return getModulePackage() + ".domain..";
    }

    default String getModuleRepositorySubpackage() {
        return getModulePackage() + ".repository..";
    }

    default String getModuleServiceSubpackage() {
        return getModulePackage() + ".service..";
    }

    default String getModuleWebSubpackage() {
        return getModulePackage() + ".web..";
    }
}
