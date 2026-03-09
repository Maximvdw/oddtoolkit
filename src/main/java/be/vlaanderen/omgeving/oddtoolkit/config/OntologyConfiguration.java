package be.vlaanderen.omgeving.oddtoolkit.config;

import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("ontology")
public class OntologyConfiguration {
  // Path to the main ontology file
  private String ontologyFilePath;

  private String conceptsFilePath;

  // List of class URIs that should be treated as enumerations (configuration key: ontology.enum-classes)
  private List<String> enumClasses = new ArrayList<>();

  // Extra fixed properties you want to inject into generated classes
  // YAML key: ontology.extra-properties
  private List<ExtraProperty> extraProperties = new ArrayList<>();

  private List<OverrideProperty> overrideProperties = new ArrayList<>();

  private List<String> temporalProperties = new ArrayList<>();

  private MetadataClasses metadataClasses = new MetadataClasses();

  @Getter
  @Setter
  public static class MetadataClasses {
    private String suffix = "Metadata";
    private String key;
    private String value;
    private List<String> classes = new ArrayList<>();
  }

  @Getter
  @Setter
  public static class OverrideProperty {
    private String uri;
    private PropertyInfo.Cardinality cardinality;
    private String datatype;
  }

  @Getter
  @Setter
  public static class ExtraProperty {
    private String name;
    private String uri;
    private String comment;
    private String range;
    private boolean identifier = false;
    private PropertyInfo.Cardinality cardinality;
  }
}
