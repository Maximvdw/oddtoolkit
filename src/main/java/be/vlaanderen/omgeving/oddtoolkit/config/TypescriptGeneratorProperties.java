package be.vlaanderen.omgeving.oddtoolkit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "generators.typescript-generator")
public class TypescriptGeneratorProperties {
  private String outputDirectory;
}
