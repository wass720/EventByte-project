package fr.soft_labs.eventbyte;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * US-19 : vérifie automatiquement les règles de l'architecture hexagonale.
 *
 * Si un test échoue, ArchUnit indique la classe et la dépendance interdite.
 * Les dépendances doivent pointer vers le centre :
 * infrastructure -> application -> domain.
 */
@AnalyzeClasses(packages = "fr.soft_labs.eventbyte", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** Le domaine ne dépend de rien : ni Spring, ni JPA, ni JSON. */
    @ArchTest
    static final ArchRule domain_does_not_depend_on_frameworks = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "org.hibernate..",
                    "tools.jackson..",
                    "com.fasterxml.jackson..",
                    "com.meilisearch..")
            .because("le domaine ne doit dépendre ni de Spring ni de JPA (US-19)");

    /** Le domaine ne connaît ni les use cases ni les adapters. */
    @ArchTest
    static final ArchRule domain_does_not_depend_on_outer_layers = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..")
            .because("les dépendances pointent vers le centre (architecture hexagonale)");

    /**
     * Les use cases passent par des ports, jamais directement par un adapter
     * (JPA, Redis, RabbitMQ, contrôleur...).
     */
    @ArchTest
    static final ArchRule application_does_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .because("un use case dépend de ports, pas d'adapters (architecture hexagonale)");
}
