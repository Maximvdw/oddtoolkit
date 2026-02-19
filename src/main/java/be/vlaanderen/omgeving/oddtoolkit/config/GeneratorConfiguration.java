package be.vlaanderen.omgeving.oddtoolkit.config;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.generator.ClassDiagramGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.ClassGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.ERDiagramGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.ShaclGenerator;
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
 */
@Configuration
@EnableConfigurationProperties({GeneratorProperties.class, ClassDiagramProperties.class, DiagramGeneratorProperties.class, ERDiagramProperties.class})
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
      ERDiagramProperties erDiagramProperties) {
    Map<String, AbstractAdapter<?>> adapterBeans = (Map) context.getBeansOfType(AbstractAdapter.class);
    List<AbstractAdapter<?>> adapters = selectAdapters(adapterBeans, generatorProperties.adaptersFor("er-diagram"));
    return new ERDiagramGenerator(ontologyInfo, conceptSchemeInfo, adapters, erDiagramProperties);
  }

  @Bean
  @SuppressWarnings("unchecked")
  public ShaclGenerator shaclGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      ApplicationContext context,
      GeneratorProperties generatorProperties) {
    Map<String, AbstractAdapter<?>> adapterBeans = (Map) context.getBeansOfType(AbstractAdapter.class);
    List<AbstractAdapter<?>> adapters = selectAdapters(adapterBeans, generatorProperties.adaptersFor("shacl"));
    return new ShaclGenerator(ontologyInfo, conceptSchemeInfo, adapters);
  }

  private List<AbstractAdapter<?>> selectAdapters(Map<String, AbstractAdapter<?>> adapterBeans, List<String> requestedAdapterNames) {
    List<AbstractAdapter<?>> available = new ArrayList<>(adapterBeans.values());
    if (requestedAdapterNames == null || requestedAdapterNames.isEmpty()) {
      // no specific selection -> use all available adapters
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
    return selected;
  }
}
