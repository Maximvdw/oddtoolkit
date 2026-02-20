package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.adapter.AdapterDependencyComparator;
import be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassConceptInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyConceptInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.Scope;
import java.util.List;
import lombok.Getter;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Getter
@Component
public class BaseGenerator {

  private static final Logger logger = LoggerFactory.getLogger(
      BaseGenerator.class);

  private final OntologyConfiguration ontologyConfiguration;
  private final OntologyInfo ontologyInfo;
  private final ConceptSchemeInfo conceptSchemeInfo;
  private final List<AbstractAdapter<?>> adapters;

  public BaseGenerator(OntologyInfo ontologyInfo, ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters) {
    this.ontologyInfo = ontologyInfo;
    this.ontologyConfiguration = ontologyInfo.getConfig();
    this.conceptSchemeInfo = conceptSchemeInfo;
    this.adapters = adapters;

    initialize();
  }

  private void initialize() {
    // Sort the adapters based on their order to ensure they are applied in the correct sequence
    // Use the @AdapterDependency annotation to determine the order of the adapters
    adapters.sort(new AdapterDependencyComparator());
    // Initialize the generator by adapting the ontology and concept scheme information
    for (AbstractAdapter<?> adapter : adapters) {
      if (adapter.canAdapt(ontologyInfo)) {
        adapter.setInfo(ontologyInfo);
      }
      if (adapter.canAdapt(conceptSchemeInfo)) {
        adapter.setInfo(conceptSchemeInfo);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public void run() {
    for (AbstractAdapter<?> adapter : adapters) {
      logger.info("Running adapter: {}", adapter.getClass().getSimpleName());
      if (adapter.canAdapt(ontologyInfo)) {
        ((AbstractAdapter<OntologyInfo>) adapter).adapt(ontologyInfo);
      }
      if (adapter.canAdapt(conceptSchemeInfo)) {
        ((AbstractAdapter<ConceptSchemeInfo>) adapter).adapt(conceptSchemeInfo);
      }
    }
  }

  /**
   * Get all classes defined in the ontology
   *
   * @return a list of ClassInfo objects representing the classes
   */
  public List<ClassInfo> getOntologyClasses() {
    // Get all classes defined in the ontology and filter them based on the provided scope
    return ontologyInfo.getClasses().stream()
        .filter(c -> c.getScope() == Scope.ONTOLOGY)
        .toList();
  }

  /**
   * Get all classes
   *
   * @return a list of ClassInfo objects representing all classes
   */
  public List<ClassInfo> getAllClasses() {
    return ontologyInfo.getClasses();
  }

  /**
   * Get all concepts defined in the concept scheme
   *
   * @return a list of ConceptInfo objects representing the class concepts in the concept scheme
   */
  public List<ClassConceptInfo> getOntologyClassConcepts() {
    return conceptSchemeInfo.getClassConcepts();
  }

  /**
   * Get the property concept for a given property URI
   *
   * @param propertyUri the URI of the property
   * @return the PropertyConceptInfo object representing the property concept, or null if not found
   */
  public PropertyConceptInfo getPropertyConceptForProperty(String propertyUri) {
    return conceptSchemeInfo.getPropertyConcepts().stream()
        .filter(pc -> pc.getEquivalents().contains(propertyUri))
        .findFirst()
        .orElse(null);
  }

  /**
   * Get the class concept for a given class URI
   *
   * @param classUri the URI of the class
   * @return the ClassConceptInfo object representing the class concept, or null if not found
   */
  public ClassConceptInfo getClassConceptForClass(String classUri) {
    return conceptSchemeInfo.getClassConcepts().stream()
        .filter(cc -> cc.getEquivalents().contains(classUri))
        .findFirst()
        .orElse(null);
  }

  public ClassInfo getXsdType(String uri) {
    // Get the XSD Property for a given URI, if it exists
    if (uri.startsWith(XSD.getURI())) {
      String localName = uri.substring(XSD.getURI().length());
      ClassInfo info = new ClassInfo(Scope.EXTERNAL, null);
      info.setUri(uri);
      info.setName(localName);
      return info;
    }
    return null;
  }

  /**
   * Utility to convert a string to snake_case, suitable for identifiers in ER diagrams.
   *
   * @param input The input string to convert
   * @return The snake_case version of the input string
   */
  protected static String toSnakeCase(String input) {
    if (input == null) {
      return null;
    }
    String s = input.trim();
    // Replace camelCase boundaries with underscore
    s = s.replaceAll("([a-z0-9])([A-Z])", "$1_$2");
    // Replace non-alphanumeric chars with underscore
    s = s.replaceAll("[^A-Za-z0-9]+", "_");
    s = s.replaceAll("_+", "_");
    s = s.replaceAll("^_+|_+$", "");
    return s.toLowerCase();
  }

  public Pair<String, String> getClassNameAndLabel(ClassInfo classInfo) {
    ClassConceptInfo classConceptInfo = getClassConceptForClass(classInfo.getUri());
    return getStringStringPair(classConceptInfo != null,
        classConceptInfo != null ? classConceptInfo.getName() : null,
        classInfo.getName(), classConceptInfo != null ? classConceptInfo.getLabel() : null);
  }

  public Pair<String, String> getPropertyNameAndLabel(PropertyInfo propertyInfo) {
    PropertyConceptInfo propertyConceptInfo = getPropertyConceptForProperty(propertyInfo.getUri());
    return getStringStringPair(propertyConceptInfo != null,
        propertyConceptInfo != null ? propertyConceptInfo.getName() : null,
        propertyInfo.getName(), propertyConceptInfo != null ? propertyConceptInfo.getLabel() : null);
  }

  private Pair<String, String> getStringStringPair(boolean b, String name, String name2,
      String label) {
    String propertyName =
        b ? name : name2;
    String propertyConceptLabel =
        b ? (label != null
            ? label : name) : null;
    String propertyLabel =
        propertyConceptLabel != null ? propertyConceptLabel : name2;
    return new Pair<>(propertyName, propertyLabel);
  }

  protected enum Cardinality {
    ONE_TO_ONE("1..1"),
    ONE_TO_MANY("1..*"),
    MANY_TO_ONE("*..1"),
    MANY_TO_MANY("*..*");

    private final String label;

    Cardinality(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return label;
    }

    public boolean isFromMany() {
      return this == MANY_TO_ONE || this == MANY_TO_MANY;
    }

    public boolean isToMany() {
      return this == ONE_TO_MANY || this == MANY_TO_MANY;
    }
  }
}
