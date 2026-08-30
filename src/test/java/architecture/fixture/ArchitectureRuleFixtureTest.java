package architecture.fixture;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import vn.nitrogen.architecture.rules.ArchitectureRules;

@Tag("architecture")
class ArchitectureRuleFixtureTest {

    private static final Set<String> FIXTURE_MODULES = Set.of("alpha", "beta");

    @Test
    void validFixturePassesRepresentativeRules() {
        String rootPackage = "architecture.fixture.valid";
        String modulePackage = rootPackage + ".alpha";
        JavaClasses classes = importPackages(rootPackage);

        assertThatNoException().isThrownBy(() -> {
            ArchitectureRules.onlyAccessPublicModuleInterfaces(rootPackage, modulePackage, Set.of()).check(classes);
            ArchitectureRules.repositoriesShouldOnlyBeUsedByOwnServiceOrApi(modulePackage).check(classes);
            ArchitectureRules.entitiesShouldNotReferenceOtherModuleEntities(rootPackage, modulePackage).check(classes);
            ArchitectureRules.moduleReferencesShouldUseUuid(modulePackage, FIXTURE_MODULES).check(classes);
            ArchitectureRules.transactionalClassesShouldResideInServicePackage(rootPackage).check(classes);
            ArchitectureRules.transactionalMethodsShouldResideInServicePackage(rootPackage).check(classes);
            ArchitectureRules.restControllersShouldBeStateless().check(classes);
            ArchitectureRules.restControllersShouldNotDependOnRepositoriesOrEntityManagers().check(classes);
            ArchitectureRules.restControllerMethodsShouldNotExposeEntities(rootPackage).check(classes);
        });
    }

    @Test
    void controllerToRepositoryFixtureIsRejected() {
        JavaClasses classes = importPackages("architecture.fixture.bad.controllerrepo");

        assertRuleViolation(
                ArchitectureRules.restControllersShouldNotDependOnRepositoriesOrEntityManagers(),
                classes,
                "REST controllers must use service/API",
                "ControllerRepoRepository");
    }

    @Test
    void controllerReturningEntityFixtureIsRejected() {
        String rootPackage = "architecture.fixture.bad.controllerentity";
        JavaClasses classes = importPackages(rootPackage);

        assertRuleViolation(
                ArchitectureRules.restControllerMethodsShouldNotExposeEntities(rootPackage),
                classes,
                "exposes JPA/domain type",
                "ExposedEntity");
    }

    @Test
    void mutableControllerFixtureIsRejected() {
        JavaClasses classes = importPackages("architecture.fixture.bad.mutablecontroller");

        assertRuleViolation(
                ArchitectureRules.restControllersShouldBeStateless(),
                classes,
                "mutable instance state",
                "MutableController.requestCount");
    }

    @Test
    void transactionalOutsideServiceFixtureIsRejected() {
        String rootPackage = "architecture.fixture.bad.transaction";
        JavaClasses classes = importPackages(rootPackage);

        assertRuleViolation(
                ArchitectureRules.transactionalClassesShouldResideInServicePackage(rootPackage),
                classes,
                "declares @Transactional outside a service package",
                "TransactionalController");
        assertRuleViolation(
                ArchitectureRules.transactionalMethodsShouldResideInServicePackage(rootPackage),
                classes,
                "declares @Transactional outside a service package",
                "TransactionalController.probe");
    }

    @Test
    void crossModuleRepositoryImportFixtureIsRejected() {
        String rootPackage = "architecture.fixture.bad.crossrepository";
        JavaClasses classes = importPackages(rootPackage);

        assertRuleViolation(
                ArchitectureRules.onlyAccessPublicModuleInterfaces(
                        rootPackage, rootPackage + ".alpha", Set.of()),
                classes,
                "outside api/, dto/ or events/",
                "AlphaRepository");
    }

    @Test
    void crossModuleEntityAssociationFixtureIsRejected() {
        String rootPackage = "architecture.fixture.bad.crossentity";
        JavaClasses classes = importPackages(rootPackage);

        assertRuleViolation(
                ArchitectureRules.entitiesShouldNotReferenceOtherModuleEntities(
                        rootPackage, rootPackage + ".beta"),
                classes,
                "across module boundary",
                "AlphaEntity");
    }

    @Test
    void crossModuleNonUuidReferenceFixtureIsRejected() {
        String rootPackage = "architecture.fixture.bad.moduleref";
        JavaClasses classes = importPackages(rootPackage);

        assertRuleViolation(
                ArchitectureRules.moduleReferencesShouldUseUuid(rootPackage + ".beta", FIXTURE_MODULES),
                classes,
                "does not use java.util.UUID",
                "BetaReferenceEntity.alphaId");
    }

    private static JavaClasses importPackages(String packageName) {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                .importPackages(packageName);
    }

    private static void assertRuleViolation(ArchRule rule, JavaClasses classes, String... messageParts) {
        assertThatThrownBy(() -> rule.check(classes))
                .isInstanceOf(AssertionError.class)
                .hasMessageContainingAll(messageParts);
    }
}
