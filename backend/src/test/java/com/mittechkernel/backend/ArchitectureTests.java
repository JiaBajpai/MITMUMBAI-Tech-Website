package com.mittechkernel.backend;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture tests to enforce module boundaries and dependency rules.
 * 
 * Run with: mvn test -Dtest=ArchitectureTests
 */
class ArchitectureTests {

    private static final JavaClasses CLASSES = new ClassFileImporter()
        .importPackages("com.mittechkernel.backend");

    @Test
    void modulesShouldNotDependOnEachOther() {
        ArchRule authMustNotDependOnUser = noClasses()
            .that().resideInAPackage("..modules.auth..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..modules.user..")
            .because("Auth module must not directly depend on the user module.")
            .allowEmptyShould(true);

        ArchRule userMustNotDependOnAuth = noClasses()
            .that().resideInAPackage("..modules.user..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..modules.auth..")
            .because("User module must not directly depend on the auth module.")
            .allowEmptyShould(true);

        authMustNotDependOnUser.check(CLASSES);
        userMustNotDependOnAuth.check(CLASSES);
    }

    @Test
    void commonShouldNotDependOnModules() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..common..")
            .should().dependOnClassesThat()
            .resideInAPackage("..modules..")
            .because("Shared infrastructure must not depend on business modules.")
            .allowEmptyShould(true);
        
        rule.check(CLASSES);
    }

    @Test
    void modulesShouldNotAccessOtherModulesRepositories() {
        ArchRule authMustNotUseUserRepositories = noClasses()
            .that().resideInAPackage("..modules.auth..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..modules.user..repository..")
            .because("Auth should use its own repository abstractions and not the user module repository directly.")
            .allowEmptyShould(true);

        ArchRule userMustNotUseAuthRepositories = noClasses()
            .that().resideInAPackage("..modules.user..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..modules.auth..repository..")
            .because("User should not reach into the auth repository layer.")
            .allowEmptyShould(true);

        authMustNotUseUserRepositories.check(CLASSES);
        userMustNotUseAuthRepositories.check(CLASSES);
    }

    @Test
    void modulesShouldNotAccessOtherModulesEntities() {
        ArchRule authMustNotUseUserEntities = noClasses()
            .that().resideInAPackage("..modules.auth..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..modules.user..entity..")
            .because("Auth must not access user entities directly.")
            .allowEmptyShould(true);

        ArchRule userMustNotUseAuthEntities = noClasses()
            .that().resideInAPackage("..modules.user..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..modules.auth..entity..")
            .because("User must not access auth entities directly.")
            .allowEmptyShould(true);

        authMustNotUseUserEntities.check(CLASSES);
        userMustNotUseAuthEntities.check(CLASSES);
    }

    @Test
    void controllersShouldStayInTheirModule() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Controller")
            .should().resideInAnyPackage("..modules.auth..", "..modules.admin..", "..modules.user..", "..modules.domain..", "..modules.resource..", "..modules.task..", "..modules.session..", "..modules.project..", "..modules.gamification..")
            .because("Controllers must stay in their module package.")
            .allowEmptyShould(true);
        
        rule.check(CLASSES);
    }

    @Test
    void servicesShouldNotDependOnControllers() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..modules..service..")
            .should().dependOnClassesThat()
            .resideInAPackage("..modules..controller..")
            .because("Services must not depend on controllers.")
            .allowEmptyShould(true);
        
        rule.check(CLASSES);
    }

    @Test
    void configShouldNotDependOnModules() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..config..")
            .should().dependOnClassesThat()
            .resideInAPackage("..modules..")
            .because("Shared configuration must not depend on business modules.")
            .allowEmptyShould(true);
        
        rule.check(CLASSES);
    }

    @Test
    void securityShouldOnlyDependOnAuthAndUserModules() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..security..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..modules.session..",
                "..modules.domain..",
                "..modules.member..",
                "..modules.registration..",
                "..modules.foundation..",
                "..modules.leaderboard.."
            )
            .because("Security configuration should only depend on auth/user modules and shared infrastructure.")
            .allowEmptyShould(true);
        
        rule.check(CLASSES);
    }

    @Test
    void queryServiceInterfacesShouldBeInModuleRootOrContractPackage() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameEndingWith("QueryService")
            .and().areInterfaces()
            .should().resideInAnyPackage("..modules..", "..modules..contract..", "..modules..dto..")
            .because("Query service interfaces define module contracts and should be in module root or contract/dto package.")
            .allowEmptyShould(true);
        
        rule.check(CLASSES);
    }
}
