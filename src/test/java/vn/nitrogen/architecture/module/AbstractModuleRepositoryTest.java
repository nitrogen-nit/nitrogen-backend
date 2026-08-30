package vn.nitrogen.architecture.module;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;
import vn.nitrogen.architecture.AbstractArchitectureTest;
import vn.nitrogen.architecture.rules.ArchitectureRules;

/**
 * Repository là tài sản riêng của module (§4.3).
 *
 * <p>Hai luật:
 * <ol>
 *   <li>Chỉ {@code service/} và {@code api/} cùng module được gọi repository —
 *       {@code web/} phải đi qua service, module khác phải đi qua facade.</li>
 *   <li>Repository phải nằm trong {@code repository/}, không rải ra chỗ khác.</li>
 * </ol>
 */
public abstract class AbstractModuleRepositoryTest extends AbstractArchitectureTest
        implements ModuleArchitectureTest {

    @Test
    void repositoriesShouldOnlyBeUsedByOwnServiceOrApi() {
        ArchitectureRules.repositoriesShouldOnlyBeUsedByOwnServiceOrApi(getModulePackage())
                .check(productionClasses);
    }

    @Test
    void springDataRepositoriesShouldResideInRepositoryPackage() {
        classes().that()
                .areAssignableTo(Repository.class)
                .and().resideInAPackage(getModuleWithSubpackage())
                .should().resideInAPackage(getModuleRepositorySubpackage())
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void repositoriesShouldNotOpenTransactionBoundary() {
        ArchitectureRules.repositoriesShouldNotDeclareTransactions(getModulePackage())
                .check(productionClasses);
        ArchitectureRules.repositoryMethodsShouldNotDeclareTransactions(getModulePackage())
                .check(productionClasses);
    }
}
