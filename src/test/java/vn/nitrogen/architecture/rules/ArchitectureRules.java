package vn.nitrogen.architecture.rules;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import vn.nitrogen.architecture.NitrogenModules;
import vn.nitrogen.common.domain.ModuleReference;

public final class ArchitectureRules {

    private static final Set<String> JPA_REFERENCE_ANNOTATIONS = Set.of(
            ManyToOne.class.getName(),
            OneToOne.class.getName(),
            OneToMany.class.getName(),
            ManyToMany.class.getName(),
            ElementCollection.class.getName());

    private ArchitectureRules() {}

    public static ArchRule onlyAccessPublicModuleInterfaces(
            String rootPackage, String modulePackage, Set<Class<?>> ignoredClasses) {
        ArchCondition<JavaClass> onlyAllowedDependencies =
                new ArchCondition<>("depend only on api/, dto/ or events/ of another module") {

                    @Override
                    public void check(JavaClass origin, ConditionEvents events) {
                        List<Dependency> targetsInModule = origin.getDirectDependenciesFromSelf().stream()
                                .filter(dependency -> resideInAPackage(moduleWithSubpackages(modulePackage))
                                        .test(dependency.getTargetClass()))
                                .toList();

                        for (Dependency dependency : targetsInModule) {
                            JavaClass target = dependency.getTargetClass();
                            boolean allowed = resideInAnyPackage(
                                            moduleSubpackage(modulePackage, "api"),
                                            moduleSubpackage(modulePackage, "dto"),
                                            moduleSubpackage(modulePackage, "events"))
                                    .test(target);
                            if (allowed || isIgnored(target, ignoredClasses)) {
                                continue;
                            }

                            events.add(SimpleConditionEvent.violated(origin,
                                    "%s depends on %s outside api/, dto/ or events/ of module %s"
                                            .formatted(origin.getName(), target.getName(), modulePackage)));
                        }
                    }
                };

        return classes().that()
                .resideInAPackage(rootPackage + "..")
                .and().resideOutsideOfPackage(moduleWithSubpackages(modulePackage))
                .and().resideOutsideOfPackage(rootPackage + ".common..")
                .and().resideOutsideOfPackage(rootPackage + ".platform..")
                .should(onlyAllowedDependencies)
                .allowEmptyShould(true);
    }

    public static ArchRule repositoriesShouldOnlyBeUsedByOwnServiceOrApi(String modulePackage) {
        return classes().that()
                .resideInAPackage(moduleSubpackage(modulePackage, "repository"))
                .should().onlyBeAccessed().byAnyPackage(
                        moduleSubpackage(modulePackage, "repository"),
                        moduleSubpackage(modulePackage, "service"),
                        moduleSubpackage(modulePackage, "api"))
                .allowEmptyShould(true);
    }

    public static ArchRule springDataRepositoriesShouldResideInRepositoryPackage(String modulePackage) {
        return classes().that()
                .areAssignableTo(Repository.class)
                .and().resideInAPackage(moduleWithSubpackages(modulePackage))
                .should().resideInAPackage(moduleSubpackage(modulePackage, "repository"))
                .allowEmptyShould(true);
    }

    public static ArchRule repositoriesShouldNotDeclareTransactions(String modulePackage) {
        return noClasses().that()
                .resideInAPackage(moduleSubpackage(modulePackage, "repository"))
                .should().beAnnotatedWith(Transactional.class)
                .orShould().beMetaAnnotatedWith(Transactional.class)
                .allowEmptyShould(true);
    }

    public static ArchRule repositoryMethodsShouldNotDeclareTransactions(String modulePackage) {
        return noMethods().that()
                .areDeclaredInClassesThat().resideInAPackage(moduleSubpackage(modulePackage, "repository"))
                .should().beAnnotatedWith(Transactional.class)
                .orShould().beMetaAnnotatedWith(Transactional.class)
                .allowEmptyShould(true);
    }

    public static ArchRule entitiesShouldNotReferenceOtherModuleEntities(String rootPackage, String modulePackage) {
        return fields().that()
                .areDeclaredInClassesThat().resideInAPackage(moduleSubpackage(modulePackage, "domain"))
                .should(notReferenceAnotherModuleEntity(rootPackage, modulePackage))
                .allowEmptyShould(true);
    }

    public static ArchRule moduleReferencesShouldUseUuid(
            String modulePackage, Collection<String> knownModuleNames) {
        return fields().that()
                .areDeclaredInClassesThat().resideInAPackage(moduleSubpackage(modulePackage, "domain"))
                .should(useUuidForModuleReference(modulePackage, knownModuleNames))
                .allowEmptyShould(true);
    }

    public static ArchRule transactionalClassesShouldResideInServicePackage(String rootPackage) {
        return classes().that()
                .areAnnotatedWith(Transactional.class)
                .and().resideInAPackage(rootPackage + "..")
                .should(resideInServicePackage())
                .allowEmptyShould(true);
    }

    public static ArchRule transactionalMethodsShouldResideInServicePackage(String rootPackage) {
        return methods().that()
                .areAnnotatedWith(Transactional.class)
                .should(beDeclaredInServicePackage(rootPackage))
                .allowEmptyShould(true);
    }

    public static ArchRule noEntityManagerOutsideCustomRepositoryImplementation(String rootPackage) {
        return classes().that()
                .resideInAPackage(rootPackage + "..")
                .should(notDependOnEntityManagerOutsideCustomRepository())
                .allowEmptyShould(true);
    }

    public static ArchRule noFieldInjection(String rootPackage) {
        return noFields().that()
                .areAnnotatedWith(Autowired.class)
                .should().beDeclaredInClassesThat().resideInAPackage(rootPackage + "..")
                .allowEmptyShould(true);
    }

    public static ArchRule restControllersShouldBeStateless() {
        return fields().that()
                .areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .should(beImmutableControllerField())
                .allowEmptyShould(true);
    }

    public static ArchRule restControllersShouldNotDependOnRepositoriesOrEntityManagers() {
        return classes().that()
                .areAnnotatedWith(RestController.class)
                .should(notDependOnRepositoriesOrEntityManagers())
                .allowEmptyShould(true);
    }

    public static ArchRule restControllerMethodsShouldNotExposeEntities(String rootPackage) {
        return methods().that()
                .areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .should(notExposeEntityInSignature(rootPackage))
                .allowEmptyShould(true);
    }

    public static ArchRule restControllersShouldResideInWebPackage(String modulePackage) {
        return classes().that()
                .areAnnotatedWith(RestController.class)
                .and().resideInAPackage(moduleWithSubpackages(modulePackage))
                .should().resideInAPackage(moduleSubpackage(modulePackage, "web"))
                .allowEmptyShould(true);
    }

    public static ArchRule webShouldNotAccessOwnRepository(String modulePackage) {
        return noClasses().that()
                .resideInAPackage(moduleSubpackage(modulePackage, "web"))
                .should().dependOnClassesThat().resideInAPackage(moduleSubpackage(modulePackage, "repository"))
                .allowEmptyShould(true);
    }

    public static ArchRule webShouldNotExposeOwnDomain(String modulePackage) {
        return noClasses().that()
                .resideInAPackage(moduleSubpackage(modulePackage, "web"))
                .should().dependOnClassesThat().resideInAPackage(moduleSubpackage(modulePackage, "domain"))
                .allowEmptyShould(true);
    }

    public static String moduleSubpackage(String modulePackage, String subpackage) {
        return modulePackage + "." + subpackage + "..";
    }

    public static String moduleWithSubpackages(String modulePackage) {
        return modulePackage + "..";
    }

    private static boolean isIgnored(JavaClass target, Set<Class<?>> ignoredClasses) {
        return ignoredClasses.stream().anyMatch(ignored -> ignored.getName().equals(target.getName()));
    }

    private static ArchCondition<JavaField> notReferenceAnotherModuleEntity(
            String rootPackage, String modulePackage) {
        return new ArchCondition<>("not map JPA associations or entity fields to another module") {

            @Override
            public void check(JavaField field, ConditionEvents events) {
                Set<JavaClass> invalidTypes = new LinkedHashSet<>();
                for (JavaClass involvedType : field.getAllInvolvedRawTypes()) {
                    if (isOtherModuleDomainType(involvedType, rootPackage, modulePackage)) {
                        invalidTypes.add(involvedType);
                    }
                }

                if (invalidTypes.isEmpty()) {
                    return;
                }

                for (JavaClass invalidType : invalidTypes) {
                    String detail = hasJpaReferenceAnnotation(field)
                            ? "JPA association"
                            : "direct entity/domain reference";
                    events.add(SimpleConditionEvent.violated(field,
                            "%s uses %s to %s across module boundary %s; store UUID and use the module API"
                                    .formatted(field.getFullName(), detail, invalidType.getName(), modulePackage)));
                }
            }
        };
    }

    private static ArchCondition<JavaField> useUuidForModuleReference(
            String modulePackage, Collection<String> knownModuleNames) {
        return new ArchCondition<>("use UUID for @ModuleReference fields") {

            @Override
            public void check(JavaField field, ConditionEvents events) {
                field.tryGetAnnotationOfType(ModuleReference.class).ifPresent(reference -> {
                    if (!field.getRawType().isEquivalentTo(UUID.class)) {
                        events.add(SimpleConditionEvent.violated(field,
                                "%s is @ModuleReference(\"%s\") but does not use java.util.UUID"
                                        .formatted(field.getFullName(), reference.value())));
                    }

                    String currentModule = moduleName(modulePackage);
                    if (reference.value().isBlank()) {
                        events.add(SimpleConditionEvent.violated(field,
                                "%s declares an empty @ModuleReference target".formatted(field.getFullName())));
                    } else if (!knownModuleNames.contains(reference.value())) {
                        events.add(SimpleConditionEvent.violated(field,
                                "%s references unknown module %s".formatted(field.getFullName(), reference.value())));
                    } else if (currentModule.equals(reference.value())) {
                        events.add(SimpleConditionEvent.violated(field,
                                "%s points @ModuleReference back to its own module".formatted(field.getFullName())));
                    }
                });
            }
        };
    }

    private static ArchCondition<JavaClass> resideInServicePackage() {
        return new ArchCondition<>("reside in a service package") {

            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (!isServicePackage(item.getPackageName())) {
                    events.add(SimpleConditionEvent.violated(item,
                            "%s declares @Transactional outside a service package".formatted(item.getName())));
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> beDeclaredInServicePackage(String rootPackage) {
        return new ArchCondition<>("be declared in a service package") {

            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                if (!NitrogenModules.isInsidePackage(method.getOwner().getPackageName(), rootPackage)) {
                    return;
                }
                if (!isServicePackage(method.getOwner().getPackageName())) {
                    events.add(SimpleConditionEvent.violated(method,
                            "%s declares @Transactional outside a service package".formatted(method.getFullName())));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notDependOnEntityManagerOutsideCustomRepository() {
        return new ArchCondition<>("not use EntityManager outside custom repository implementations") {

            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean usesEntityManager = javaClass.getDirectDependenciesFromSelf().stream()
                        .map(Dependency::getTargetClass)
                        .anyMatch(target -> target.isAssignableTo(EntityManager.class));

                if (usesEntityManager && !isCustomRepositoryImplementation(javaClass)) {
                    events.add(SimpleConditionEvent.violated(javaClass,
                            "%s uses EntityManager outside a custom repository implementation"
                                    .formatted(javaClass.getName())));
                }
            }
        };
    }

    private static ArchCondition<JavaField> beImmutableControllerField() {
        return new ArchCondition<>("be private final instance state or static final constant") {

            @Override
            public void check(JavaField field, ConditionEvents events) {
                Set<JavaModifier> modifiers = field.getModifiers();
                boolean staticFinal = modifiers.contains(JavaModifier.STATIC) && modifiers.contains(JavaModifier.FINAL);
                if (staticFinal) {
                    return;
                }

                if (!modifiers.contains(JavaModifier.FINAL)) {
                    events.add(SimpleConditionEvent.violated(field,
                            "%s is mutable instance state in a @RestController".formatted(field.getFullName())));
                    return;
                }

                if (!modifiers.contains(JavaModifier.PRIVATE)) {
                    events.add(SimpleConditionEvent.violated(field,
                            "%s must be private final in a @RestController".formatted(field.getFullName())));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notDependOnRepositoriesOrEntityManagers() {
        return new ArchCondition<>("not depend on Repository or EntityManager") {

            @Override
            public void check(JavaClass controller, ConditionEvents events) {
                controller.getDirectDependenciesFromSelf().stream()
                        .map(Dependency::getTargetClass)
                        .filter(target -> target.isAssignableTo(Repository.class)
                                || target.isAssignableTo(EntityManager.class))
                        .forEach(target -> events.add(SimpleConditionEvent.violated(controller,
                                "%s depends on %s; REST controllers must use service/API, not Repository or EntityManager"
                                        .formatted(controller.getName(), target.getName()))));
            }
        };
    }

    private static ArchCondition<JavaMethod> notExposeEntityInSignature(String rootPackage) {
        return new ArchCondition<>("not expose JPA entities in REST method signatures") {

            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Set<JavaClass> involvedTypes = new LinkedHashSet<>(method.getReturnType().getAllInvolvedRawTypes());
                method.getParameterTypes()
                        .forEach(parameterType -> involvedTypes.addAll(parameterType.getAllInvolvedRawTypes()));

                involvedTypes.stream()
                        .filter(type -> isEntityOrDomainType(type, rootPackage))
                        .forEach(type -> events.add(SimpleConditionEvent.violated(method,
                                "%s exposes JPA/domain type %s in a REST signature; use DTOs only"
                                        .formatted(method.getFullName(), type.getName()))));
            }
        };
    }

    private static boolean hasJpaReferenceAnnotation(JavaField field) {
        return field.getAnnotations().stream()
                .map(annotation -> annotation.getRawType().getName())
                .anyMatch(JPA_REFERENCE_ANNOTATIONS::contains);
    }

    private static boolean isOtherModuleDomainType(JavaClass target, String rootPackage, String modulePackage) {
        String targetPackage = target.getPackageName();
        return NitrogenModules.isInsidePackage(targetPackage, rootPackage)
                && !NitrogenModules.isInsidePackage(targetPackage, modulePackage)
                && !NitrogenModules.isInsidePackage(targetPackage, rootPackage + ".common")
                && !NitrogenModules.isInsidePackage(targetPackage, rootPackage + ".platform")
                && isEntityOrDomainType(target, rootPackage);
    }

    private static boolean isEntityOrDomainType(JavaClass target, String rootPackage) {
        return target.isAnnotatedWith(Entity.class)
                || (NitrogenModules.isInsidePackage(target.getPackageName(), rootPackage)
                        && target.getPackageName().contains(".domain"));
    }

    private static boolean isCustomRepositoryImplementation(JavaClass javaClass) {
        return javaClass.getPackageName().contains(".repository")
                && javaClass.getSimpleName().endsWith("RepositoryImpl");
    }

    private static boolean isServicePackage(String packageName) {
        return packageName.endsWith(".service") || packageName.contains(".service.");
    }

    private static String moduleName(String modulePackage) {
        return modulePackage.substring(modulePackage.lastIndexOf('.') + 1);
    }
}
