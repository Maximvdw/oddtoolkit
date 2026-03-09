package be.vlaanderen.omgeving.oddtoolkit.config;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.generator.ClassDiagramGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.ClassGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.ERDiagramGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.JavaGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.SQLGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.ShaclGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.TypescriptGenerator;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for creating generator beans and filtering adapters based on properties.
 *
 * This configuration:
 * - Registers all generators with the GeneratorRegistry for dynamic access
 * - Filters adapters based on GeneratorProperties configuration
 * - Supports both legacy @Bean approach and new plugin-based approach
 * - Maintains backward compatibility with existing YAML configuration
 */
@Configuration
@EnableConfigurationProperties({
    GeneratorProperties.class,
    ClassDiagramProperties.class,
    DiagramGeneratorProperties.class,
    ERDiagramProperties.class,
    TypescriptGeneratorProperties.class,
    JavaGeneratorProperties.class,
    SchemaGeneratorProperties.class
})
public class GeneratorConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(GeneratorConfiguration.class);

  @Bean
  @SuppressWarnings("unchecked")
  public ClassGenerator classGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      ApplicationContext context,
      GeneratorProperties generatorProperties) {
    Map<String, AbstractAdapter<?>> adapterBeans = (Map) context.getBeansOfType(AbstractAdapter.class);
    List<AbstractAdapter<?>> adapters = selectAdapters(adapterBeans, generatorProperties.adaptersFor("class"));
    return new ClassGenerator(ontologyInfo, conceptSchemeInfo, adapters);
  }

  @Bean
  @SuppressWarnings("unchecked")
  public ClassDiagramGenerator classDiagramGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      ApplicationContext context,
      GeneratorProperties generatorProperties,
      ClassDiagramProperties classDiagramProperties,
      DiagramGeneratorProperties diagramGeneratorProperties) {
    Map<String, AbstractAdapter<?>> adapterBeans = (Map) context.getBeansOfType(AbstractAdapter.class);
    List<AbstractAdapter<?>> adapters = selectAdapters(adapterBeans, generatorProperties.adaptersFor("class-diagram"));
    return new ClassDiagramGenerator(ontologyInfo, conceptSchemeInfo, adapters, classDiagramProperties, diagramGeneratorProperties);
  }

  @Bean
  @SuppressWarnings("unchecked")
  public ERDiagramGenerator erDiagramGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      ApplicationContext context,
      GeneratorProperties generatorProperties,
      DiagramGeneratorProperties diagramGeneratorProperties,
      SchemaGeneratorProperties schemaGeneratorProperties,
      ERDiagramProperties erDiagramProperties) {
    Map<String, AbstractAdapter<?>> adapterBeans = (Map) context.getBeansOfType(AbstractAdapter.class);
    List<AbstractAdapter<?>> adapters = selectAdapters(adapterBeans, generatorProperties.adaptersFor("er-diagram"));
    return new ERDiagramGenerator(ontologyInfo, conceptSchemeInfo, adapters, diagramGeneratorProperties, schemaGeneratorProperties, erDiagramProperties);
  }

  @Bean
  public SQLGenerator sqlGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      ApplicationContext context,
      GeneratorProperties generatorProperties,
      DiagramGeneratorProperties diagramGeneratorProperties,
      SchemaGeneratorProperties schemaGeneratorProperties,
      SQLGeneratorProperties sqlGeneratorProperties) {
    Map<String, AbstractAdapter<?>> adapterBeans = (Map) context.getBeansOfType(AbstractAdapter.class);
    List<AbstractAdapter<?>> adapters = selectAdapters(adapterBeans, generatorProperties.adaptersFor("sql"));
    return new SQLGenerator(ontologyInfo, conceptSchemeInfo, adapters, diagramGeneratorProperties, schemaGeneratorProperties, sqlGeneratorProperties);
  }

  @Bean
  @SuppressWarnings("unchecked")
  public ShaclGenerator shaclGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      ApplicationContext context,
      GeneratorProperties generatorProperties,
      ShaclGeneratorProperties shaclGeneratorProperties) {
    Map<String, AbstractAdapter<?>> adapterBeans = (Map) context.getBeansOfType(AbstractAdapter.class);
    List<AbstractAdapter<?>> adapters = selectAdapters(adapterBeans, generatorProperties.adaptersFor("shacl"));
    return new ShaclGenerator(ontologyInfo, conceptSchemeInfo, adapters, shaclGeneratorProperties);
  }

  @Bean
  @SuppressWarnings("unchecked")
  public JavaGenerator javaGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      ApplicationContext context,
      GeneratorProperties generatorProperties,
      DiagramGeneratorProperties diagramGeneratorProperties,
      SchemaGeneratorProperties schemaGeneratorProperties,
      JavaGeneratorProperties javaGeneratorProperties) {
    Map<String, AbstractAdapter<?>> adapterBeans = (Map) context.getBeansOfType(AbstractAdapter.class);
    List<AbstractAdapter<?>> adapters = selectAdapters(adapterBeans, generatorProperties.adaptersFor("shacl"));
    return new JavaGenerator(ontologyInfo, conceptSchemeInfo, adapters, diagramGeneratorProperties, schemaGeneratorProperties, javaGeneratorProperties);
  }

  @Bean
  @SuppressWarnings("unchecked")
  public TypescriptGenerator typescriptGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      ApplicationContext context,
      GeneratorProperties generatorProperties,
      TypescriptGeneratorProperties typescriptGeneratorProperties) {
    Map<String, AbstractAdapter<?>> adapterBeans = (Map) context.getBeansOfType(AbstractAdapter.class);
    List<AbstractAdapter<?>> adapters = selectAdapters(adapterBeans, generatorProperties.adaptersFor("shacl"));
    return new TypescriptGenerator(ontologyInfo, conceptSchemeInfo, adapters, typescriptGeneratorProperties);
  }

  /**
   * Register all generators with the GeneratorRegistry.
   * This allows generators to be accessed dynamically by name through the registry.
   *
   * @param registry the generator registry
   * @param classGenerator the class generator bean
   * @param classDiagramGenerator the class diagram generator bean
   * @param erDiagramGenerator the ER diagram generator bean
   * @param sqlGenerator the SQL generator bean
   * @param shaclGenerator the SHACL generator bean
   * @param javaGenerator the Java generator bean
   * @param typescriptGenerator the TypeScript generator bean
   */
  @Bean
  public GeneratorRegistrationHelper generatorRegistrationHelper(
      GeneratorRegistry registry,
      ClassGenerator classGenerator,
      ClassDiagramGenerator classDiagramGenerator,
      ERDiagramGenerator erDiagramGenerator,
      SQLGenerator sqlGenerator,
      ShaclGenerator shaclGenerator,
      JavaGenerator javaGenerator,
      TypescriptGenerator typescriptGenerator) {
    return new GeneratorRegistrationHelper(registry,
        classGenerator, classDiagramGenerator, erDiagramGenerator,
        sqlGenerator, shaclGenerator, javaGenerator, typescriptGenerator);
  }

  private List<AbstractAdapter<?>> selectAdapters(Map<String, AbstractAdapter<?>> adapterBeans, List<String> requestedAdapterNames) {
    List<AbstractAdapter<?>> available = new ArrayList<>(adapterBeans.values());
    if (requestedAdapterNames == null || requestedAdapterNames.isEmpty()) {
      // no specific selection -> use all available adapters
      // Sort by dependencies
      available.sort(new be.vlaanderen.omgeving.oddtoolkit.adapter.AdapterDependencyComparator());
      return available;
    }
    // Map bean names to instances
    Map<String, AbstractAdapter<?>> beanNameToAdapter = adapterBeans.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    List<AbstractAdapter<?>> selected = new ArrayList<>();
    for (String name : requestedAdapterNames) {
      if (beanNameToAdapter.containsKey(name)) {
        selected.add(beanNameToAdapter.get(name));
      } else {
        logger.warn("Requested adapter '{}' is not available or is disabled; ignoring.", name);
      }
    }
    // Sort by dependencies
    selected.sort(new be.vlaanderen.omgeving.oddtoolkit.adapter.AdapterDependencyComparator());
    return selected;
  }
}
