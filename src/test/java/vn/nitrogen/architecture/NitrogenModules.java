package vn.nitrogen.architecture;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class NitrogenModules {

    public static final String ROOT_PACKAGE = "vn.nitrogen";

    public static final List<String> BUSINESS_MODULE_NAMES = List.of(
            "identity",
            "curriculum",
            "chemistry",
            "content",
            "assessment",
            "examination",
            "practice",
            "progress",
            "flashcard",
            "simulation",
            "integration",
            "administration");

    public static final Set<String> OPEN_MODULE_NAMES = Set.of("common", "platform");

    private NitrogenModules() {}

    public static Optional<String> businessModuleNameOf(String packageName) {
        return BUSINESS_MODULE_NAMES.stream()
                .filter(module -> isInsidePackage(packageName, ROOT_PACKAGE + "." + module))
                .findFirst();
    }

    public static boolean isBusinessModulePackage(String packageName) {
        return businessModuleNameOf(packageName).isPresent();
    }

    public static boolean isOpenModulePackage(String packageName) {
        return OPEN_MODULE_NAMES.stream()
                .anyMatch(module -> isInsidePackage(packageName, ROOT_PACKAGE + "." + module));
    }

    public static boolean isInsidePackage(String candidatePackage, String packagePrefix) {
        return candidatePackage.equals(packagePrefix) || candidatePackage.startsWith(packagePrefix + ".");
    }
}
