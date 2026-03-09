package be.vlaanderen.omgeving.oddtoolkit.cli;

import be.vlaanderen.omgeving.oddtoolkit.config.CliConfiguration;
import be.vlaanderen.omgeving.oddtoolkit.config.ConfigurationSourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * CLI runner for executing generators from command line.
 *
 * Usage:
 *   java -jar oddtoolkit.jar --generator=class-diagram --output=/tmp/output
 *   java -jar oddtoolkit.jar --generator=sql --config-file=custom-config.yml
 *   java -jar oddtoolkit.jar --help
 *
 * Supports:
 * - Generator selection via --generator flag
 * - Configuration file loading (YAML/JSON)
 * - Custom property overrides via --key=value
 * - Environment variable interpolation via ODD_* prefixed variables
 *
 * Note: This is optional and for future CLI support. Current application
 * still works with Spring Boot configuration via application.yml.
 */
@Component
public class GeneratorCliRunner implements CommandLineRunner {

  private static final Logger logger = LoggerFactory.getLogger(GeneratorCliRunner.class);

  @Autowired
  private ApplicationContext applicationContext;

  @Override
  public void run(String... args) throws Exception {
    CliConfiguration cliConfig = CliConfiguration.fromArgs(args);

    if (cliConfig.isHelpRequested()) {
      printHelp();
      return;
    }

    // Only process CLI if a generator is explicitly specified
    if (cliConfig.getGeneratorName() != null && !cliConfig.getGeneratorName().isEmpty()) {
      logger.info("CLI Generator execution requested: {}", cliConfig);
      // Future implementation: execute selected generator
      // This is a placeholder for future CLI support
      logger.info("Current implementation uses Spring Boot configuration via application.yml");
      logger.info("CLI support for direct generator execution is in development");
    }
  }

  /**
   * Print CLI help message.
   */
  private void printHelp() {
    System.out.println("""
        ODD Toolkit - Ontology-Driven Development Generator
        
        Usage: java -jar oddtoolkit.jar [OPTIONS]
        
        Options:
          --generator=NAME              Name of the generator to execute
                                        Available: class, class-diagram, er-diagram, sql, shacl, java, typescript
          
          --config-file=PATH            Path to configuration file (YAML or JSON)
                                        Example: --config-file=config.yml
          
          --output=PATH                 Output directory for generated files
                                        Example: --output=/tmp/output
          
          --ontology-file=PATH          Path to ontology file (overrides config file)
                                        Example: --ontology-file=ontology.ttl
          
          --concepts-file=PATH          Path to concepts file (overrides config file)
                                        Example: --concepts-file=concepts.ttl
          
          --help, -h                    Show this help message
        
        Examples:
          # Generate class diagram with default configuration
          java -jar oddtoolkit.jar --generator=class-diagram
          
          # Generate SQL with custom configuration file
          java -jar oddtoolkit.jar --generator=sql --config-file=myconfig.yml
          
          # Generate with custom output directory
          java -jar oddtoolkit.jar --generator=class-diagram --output=/home/user/output
        
        Environment Variables:
          Configuration values can be set via ODD_* prefixed environment variables.
          Example: ODD_GENERATOR_NAME=sql
          
        Configuration Files:
          Supported formats: YAML (.yml, .yaml) and JSON (.json)
          Configuration precedence (highest to lowest):
            1. Command-line arguments (--key=value)
            2. Environment variables (ODD_KEY)
            3. Configuration file
            4. Default values
        """);
  }
}

