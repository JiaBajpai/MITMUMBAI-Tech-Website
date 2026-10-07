package com.mittechkernel.backend;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

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
        ArchRule rule = noClasses()
            .that().resideInAPackage("..modules..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..modules.auth..",
                "..modules.user..",
                "..modules.member..",
                "..modules.domain..",
                "..modules.session..",
                "..modules.registration..",
                "..modules.foundation..",
                "..modules.leaderboard.."
            )
            .because("Business modules must not depend on each other directly. " +
                     "Use query service interfaces for cross-module reads.")
            .allowEmptyShould(true);
        
        rule.check(CLASSES);
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
        ArchRule rule = noClasses()
            .that().resideInAPackage("..modules..")
            .should().dependOnClassesThat()
            .resideInAPackage("..modules..Repository")
            .andShould().notBeAssignableTo(org.springframework.data.repository.Repository.class)
            .because("Modules must not access other modules' repositories directly.")
            .allowEmptyShould(true);
        
        rule.check(CLASSES);
    }

    @Test
    void modulesShouldNotAccessOtherModulesEntities() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..modules..")
            .should().dependOnClassesThat()
            .resideInAPackage("..modules..entity..")
            .because("Modules must not access other modules' entities directly. Use DTOs via query services.")
            .allowEmptyShould(true);
        
        rule.check(CLASSES);
    }

    @Test
    void controllersShouldStayInTheirModule() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..modules..")
            .and().haveSimpleNameEndingWith("Controller")
            .should().resideInAPackage("..modules..")
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
            .resideInAnyPackage("..modules.auth..", "..modules.user..", "..common..", "..config..")
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