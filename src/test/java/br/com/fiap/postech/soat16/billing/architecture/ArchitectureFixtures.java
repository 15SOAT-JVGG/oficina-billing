package br.com.fiap.postech.soat16.billing.architecture;

import java.nio.file.Path;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * As classes são importadas uma vez por JVM e compartilhadas por todos os testes de arquitetura.
 */
final class ArchitectureFixtures {

    static final String ROOT = "br.com.fiap.postech.soat16.billing";
    static final String SHARED = ROOT + ".shared..";
    static final String APP_EXCEPTION = ROOT + ".shared.domain.exception.AppException";
    static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));

    static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
            .importPackages(ROOT);

    static final JavaClasses TEST_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
            .importPackages(ROOT);

    private ArchitectureFixtures() {
    }
}
