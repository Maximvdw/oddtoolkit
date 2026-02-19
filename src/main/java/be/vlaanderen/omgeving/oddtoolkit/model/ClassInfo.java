package be.vlaanderen.omgeving.oddtoolkit.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.rdf.model.Resource;

@Getter
@Setter
public class ClassInfo extends AbstractInfo {
  private UriTemplate uriTemplate;
  private List<ClassInfo> superClasses = new ArrayList<>();
  private List<PropertyInfo> properties = new ArrayList<>();

  public ClassInfo(Scope scope, Resource resource) {
    super(scope, resource);
  }

  public PropertyInfo getPropertyByUri(String propertyUri) {
    for (PropertyInfo property : properties) {
      if (property.getUri().equals(propertyUri)) {
        return property;
      }
    }
    return null;
  }

  public boolean isSubClassOf(String classUri) {
    for (ClassInfo superClass : superClasses) {
      if (superClass.getUri().equals(classUri) || superClass.isSubClassOf(classUri)) {
        return true;
      }
    }
    return false;
  }
}
