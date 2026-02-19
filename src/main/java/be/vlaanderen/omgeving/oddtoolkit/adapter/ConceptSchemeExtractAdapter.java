package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.model.ClassConceptInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyConceptInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.Scope;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@AdapterDependency({
    ConceptSchemeLoadAdapter.class
})
@ConditionalOnProperty(prefix = "adapters", name = "concept-scheme-extract.enabled", havingValue = "true", matchIfMissing = true)
@Component("concept-scheme-extract")
public class ConceptSchemeExtractAdapter extends AbstractAdapter<ConceptSchemeInfo> {

  public ConceptSchemeExtractAdapter() {
    super(ConceptSchemeInfo.class);
  }

  @Override
  public ConceptSchemeInfo adapt(ConceptSchemeInfo info) {
    // Extract the concepts
    ResIterator conceptIterator = info.getModel().listResourcesWithProperty(
        RDF.type,
        SKOS.Concept
    );
    List<ClassConceptInfo> classConcepts = new ArrayList<>();
    List<PropertyConceptInfo> propertyConcepts = new ArrayList<>();
    conceptIterator.forEachRemaining(concept -> {
      if (concept.hasProperty(OWL2.equivalentClass)) {
        classConcepts.add(new ClassConceptInfo(Scope.CONCEPTS, concept));
      } else if (concept.hasProperty(OWL2.equivalentProperty)) {
        propertyConcepts.add(new PropertyConceptInfo(Scope.CONCEPTS, concept));
      }
    });

    info.setClassConcepts(classConcepts);
    info.setPropertyConcepts(propertyConcepts);
    return null;
  }
}
