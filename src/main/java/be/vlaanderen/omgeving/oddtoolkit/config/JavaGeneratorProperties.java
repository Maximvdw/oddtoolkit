package be.vlaanderen.omgeving.oddtoolkit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "generators.java-generator")
public class JavaGeneratorProperties {
  private String outputDirectory;
  private String packageName = "be.vlaanderen.omgeving.oddtoolkit.generated";
}
