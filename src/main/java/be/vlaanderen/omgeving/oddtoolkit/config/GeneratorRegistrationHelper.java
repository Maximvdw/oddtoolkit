package be.vlaanderen.omgeving.oddtoolkit.config;

import be.vlaanderen.omgeving.oddtoolkit.generator.ClassDiagramGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.ClassGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.ERDiagramGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.JavaGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.SQLGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.ShaclGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.TypescriptGenerator;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to register all generator beans with the GeneratorRegistry on application startup.
 * This enables dynamic access to generators by name through the registry.
 */
public record GeneratorRegistrationHelper(GeneratorRegistry registry, ClassGenerator classGenerator,
                                          ClassDiagramGenerator classDiagramGenerator,
                                          ERDiagramGenerator erDiagramGenerator,
                                          SQLGenerator sqlGenerator, ShaclGenerator shaclGenerator,
                                          JavaGenerator javaGenerator,
                                          TypescriptGenerator typescriptGenerator) {

  private static final Logger logger = LoggerFactory.getLogger(GeneratorRegistrationHelper.class);

  @PostConstruct
  public void registerGenerators() {
    logger.info("Registering generators with GeneratorRegistry");

    registry.register("class", classGenerator);
    registry.register("class-diagram", classDiagramGenerator);
    registry.register("er-diagram", erDiagramGenerator);
    registry.register("sql", sqlGenerator);
    registry.register("shacl", shaclGenerator);
    registry.register("java", javaGenerator);
    registry.register("typescript", typescriptGenerator);

    logger.info(
        "Available generators: class, class-diagram, er-diagram, sql, shacl, java, typescript");
  }
}

