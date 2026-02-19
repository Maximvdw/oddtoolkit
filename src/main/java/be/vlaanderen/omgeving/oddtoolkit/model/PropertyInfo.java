package be.vlaanderen.omgeving.oddtoolkit.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

@Getter
@Setter
public class PropertyInfo extends AbstractInfo {
  private String inverseOf;
  private boolean isIdentifier;
  private Cardinality cardinalityTo;
  private Cardinality cardinalityFrom;
  private List<String> range;

  public PropertyInfo(Scope scope, Resource resource, Resource parent) {
    super(scope, resource);
    cardinalityTo = new Cardinality();
    cardinalityFrom = new Cardinality();
    range = new ArrayList<>();
    if (parent != null) {
      initializeProperty(parent);
    }
  }

  public PropertyInfo(Scope scope, Resource resource) {
    this(scope, resource, null);
  }

  protected void initializeProperty(Resource resource) {
    if (resource.hasProperty(RDF.type, RDF.Property) || resource.hasProperty(RDF.type,
        OWL2.ObjectProperty) || resource.hasProperty(RDF.type, OWL2.DatatypeProperty)) {
      setUri(resource.getURI());
      setName(resource.getLocalName());
      if (resource.hasProperty(RDFS.range)) {
        resource.listProperties(RDFS.range).forEachRemaining(stmt -> {
          if (stmt.getObject().isResource()) {
            range.add(stmt.getObject().asResource().getURI());
          }
        });
      }
    }

    // Determine the cardinality of the property based on the presence of certain RDF properties
    if (resource.hasProperty(OWL2.maxCardinality)) {
      cardinalityTo.max = resource.getProperty(OWL2.maxCardinality).getInt();
    }

    if (resource.hasProperty(OWL2.minCardinality)) {
      cardinalityTo.min = resource.getProperty(OWL2.minCardinality).getInt();
    }

    if (resource.hasProperty(OWL2.cardinality)) {
      int card = resource.getProperty(OWL2.cardinality).getInt();
      cardinalityTo.min = card;
      cardinalityTo.max = card;
    }

    if (resource.hasProperty(OWL2.someValuesFrom)) {
      // Set the range types
      resource.listProperties(OWL2.someValuesFrom).forEachRemaining(stmt -> {
        if (stmt.getObject().isResource()) {
          range.add(stmt.getObject().asResource().getURI());
        }
      });
    } else if (resource.hasProperty(OWL2.allValuesFrom)) {
      resource.listProperties(OWL2.allValuesFrom).forEachRemaining(stmt -> {
        if (stmt.getObject().isResource()) {
          range.add(stmt.getObject().asResource().getURI());
        }
      });
    }

    // Ensure no properties are allowed with null uri
    if (getUri() == null) {
      throw new IllegalArgumentException(
          "Property URI cannot be null for resource: \n" + this);
    }
  }

  @Getter
  @Setter
  public static class Cardinality {

    private Integer min;
    private Integer max;
  }
}
