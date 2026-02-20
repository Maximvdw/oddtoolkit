package be.vlaanderen.omgeving.oddtoolkit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "generators.sql-generator")
public class SQLGeneratorProperties {
  private String outputFile;
}
