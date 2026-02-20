package be.vlaanderen.omgeving.oddtoolkit.config;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration for general diagram generators.
 * Binds to: generators.diagram-generator
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "generators.diagram-generator")
public class DiagramGeneratorProperties {
  private String outputFile;
  private List<StyleEntry> styles;

  @Getter
  @Setter
  public static class StyleEntry {
    private String name;
    private String uri;
    private List<String> uris;
    private Map<String, Object> props;
  }
}
