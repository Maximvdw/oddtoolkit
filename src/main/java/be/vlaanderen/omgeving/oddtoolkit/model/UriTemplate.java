package be.vlaanderen.omgeving.oddtoolkit.model;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

@Getter
@Setter
public class UriTemplate extends AbstractInfo {

  private String template;
  private Map<String, String> variables;

  public UriTemplate(Scope scope, Resource resource) {
    super(scope, resource);
  }

  public void initializeFromResource(Resource resource) {
    super.initializeFromResource(resource);
    variables = new HashMap<>();
    // Extract the template string and variables from the resource
    Property hydraTemplate = resource.getModel()
        .getProperty("http://www.w3.org/ns/hydra/core#template");
    if (resource.hasProperty(hydraTemplate)) {
      this.template = resource.getProperty(hydraTemplate).getString();
    }
    // Extract variables and their corresponding properties
    Property hydraMapping = resource.getModel()
        .getProperty("http://www.w3.org/ns/hydra/core#mapping");
    if (resource.hasProperty(hydraMapping)) {
      // Loop through the mappings and extract variable names and their corresponding properties
      resource.listProperties(hydraMapping).forEachRemaining(stmt -> {
        if (stmt.getObject().isResource()) {
          Resource mappingResource = stmt.getObject().asResource();
          String variableName = mappingResource.getProperty(
                  resource.getModel().getProperty("http://www.w3.org/ns/hydra/core#variable"))
              .getString();
          String propertyUri = mappingResource.getProperty(
                  resource.getModel().getProperty("http://www.w3.org/ns/hydra/core#property"))
              .getObject().asResource().getURI();
          variables.put(variableName, propertyUri);
        }
      });
    }
  }
}
