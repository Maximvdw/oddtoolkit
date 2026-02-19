package be.vlaanderen.omgeving.oddtoolkit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration for the class-diagram generator.
 * Binds to: generators.class-diagram
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "generators.class-diagram")
public class ClassDiagramProperties {
  private String outputFile;
}
