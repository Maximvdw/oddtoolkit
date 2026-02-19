package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.Scope;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@AdapterDependency({
    OntologyClassExtractAdapter.class,
    ConceptSchemeLoadAdapter.class
})
@ConditionalOnProperty(prefix = "adapters", name = "concept-class-extract.enabled", havingValue = "true", matchIfMissing = true)
@Component("concept-class-extract")
public class ConceptClassExtractAdapter extends AbstractAdapter<OntologyInfo> {

  private final ConceptSchemeInfo conceptSchemeInfo;

  public ConceptClassExtractAdapter(ConceptSchemeInfo conceptSchemeInfo) {
    super(OntologyInfo.class);
    this.conceptSchemeInfo = conceptSchemeInfo;
  }

  @Override
  public OntologyInfo adapt(OntologyInfo info) {
    // Add all concept classes that are not already present in the ontology info
    conceptSchemeInfo.getClassConcepts().forEach(concept -> {
      if (info.getClassByUri(concept.getUri()) == null) {
        ClassInfo classInfo = new ClassInfo(Scope.ONTOLOGY, concept.getResource());
        classInfo.setUri(concept.getEquivalents().get(0));
        extractProperties(info, classInfo);
        info.addClass(classInfo);
      }
    });
    return info;
  }

  private void extractProperties(OntologyInfo info, ClassInfo classInfo) {
    // Extract all properties from the inferred model that have as
    // a domain the class and add them to the class info (if not already present)

    // Create resource from classinfo URI
    Resource objectResource = info.getInferredModel().createResource(classInfo.getUri());
    info.getInferredModel().listStatements(null, RDFS.domain, objectResource)
        .forEachRemaining(statement -> {
          // Extract the property and add it to the class info
          Property property = statement.getSubject().as(Property.class);
          if (classInfo.getProperties().stream().noneMatch(p -> p.getUri().equals(property.getURI()))) {
            PropertyInfo propertyInfo = new PropertyInfo(classInfo.getScope(), property);
            propertyInfo.getCardinalityTo().setMax(1);
            propertyInfo.getCardinalityTo().setMin(0);
            classInfo.getProperties().add(propertyInfo);
          }
        });
    // Filter properties, only properties that have an equivalent concept property should be included
    classInfo.getProperties().removeIf(propertyInfo -> conceptSchemeInfo.getPropertyConcepts().stream()
        .noneMatch(conceptProperty -> conceptProperty.getEquivalents().contains(propertyInfo.getUri())));
  }
}
