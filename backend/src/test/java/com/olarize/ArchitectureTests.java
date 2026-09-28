package com.olarize;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTests {
    @Test void moduleDependenciesRemainValid() {
        ApplicationModules.of(OlarizeApplication.class).verify();
    }
    @Test void domainStaysIndependentOfFrameworksAndAdapters() {
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..", "jakarta.persistence..", "jakarta.servlet..",
                "..adapter..", "..application..")
            .allowEmptyShould(true)
            .check(new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.olarize"));
    }
}
