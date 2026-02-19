package be.vlaanderen.omgeving.oddtoolkit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration for the er-diagram generator.
 * Binds to: generators.er-diagram
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "generators.er-diagram")
public class ERDiagramProperties {
  private String outputFile;
}
