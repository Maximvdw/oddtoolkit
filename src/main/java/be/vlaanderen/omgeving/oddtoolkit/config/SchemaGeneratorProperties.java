package be.vlaanderen.omgeving.oddtoolkit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "generators.schema-generator")
public class SchemaGeneratorProperties {
  private MergeJoinTables mergeJoinTables = new MergeJoinTables();

  @Getter
  @Setter
  public static class MergeJoinTables {

    private boolean enabled = true;
    private String attributeName = "relation_type";
  }
}

