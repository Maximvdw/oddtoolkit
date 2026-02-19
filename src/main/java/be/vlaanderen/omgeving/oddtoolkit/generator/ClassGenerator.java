package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.Scope;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class ClassGenerator extends BaseGenerator {

  protected List<ClassInfo> concreteClasses;
  protected List<ClassInfo> interfaces;
  protected Map<ClassInfo, List<ClassInfo>> enums;

  public ClassGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo, List<AbstractAdapter<?>> adapters) {
    super(ontologyInfo, conceptSchemeInfo, adapters);
  }

  @Override
  public void run() {
    super.run();
    this.concreteClasses = getOntologyClasses();
    this.interfaces = getAllClasses()
        .stream()
        .filter(c -> c.getScope() == Scope.EXTERNAL)
        .toList();
    this.enums = getAllClasses()
        .stream()
        .filter(c -> getOntologyConfiguration().getEnumClasses().contains(c.getUri()))
        .collect(Collectors.toMap(c -> c, c -> List.of()));
    filterInterfaces();
    filterEnums();
    filterSuperClasses();
    getConcreteClasses().forEach(this::filterInverseProperties);
    getInterfaces().forEach(this::filterInverseProperties);
  }

  protected void filterInverseProperties(ClassInfo classInfo) {
    // Filter inverse properties and copy the cardinality
    // Then we can remove the inverse properties from the class and only keep the original properties
    classInfo.getProperties()
        .stream()
        .filter(p -> p.getInverseOf() != null)
        .forEach(p -> {
          getAllClasses().stream()
              .flatMap(c -> c.getProperties().stream())
              .filter(ip -> ip.getUri().equals(p.getInverseOf()))
              .findFirst()
              .ifPresent(inverseProperty -> {
                p.setCardinalityFrom(inverseProperty.getCardinalityTo());
                inverseProperty.setCardinalityFrom(p.getCardinalityTo());
                // Check that one of the properties has a comment, if not we can keep either one of them
                if ((p.getComment() == null || p.getComment().isEmpty()) && (
                    inverseProperty.getComment() == null || inverseProperty.getComment()
                        .isEmpty())) {
                  // Keep the property with the lower URI (arbitrary choice to keep one of them)
                  if (p.getUri().compareTo(inverseProperty.getUri()) < 0) {
                    p.setComment("Inverse property of " + inverseProperty.getUri());
                  } else {
                    inverseProperty.setComment("Inverse property of " + p.getUri());
                  }
                }
              });
        });
    // If the class is an interface then remove all inverse properties
    if (isInterface(classInfo)) {
      classInfo.setProperties(classInfo.getProperties().stream()
          .filter(p -> p.getInverseOf() == null)
          .toList());
      return;
    }
    // Remove the property that is an inverse property
    // We will keep the property that has a comment and remove the one without a comment
    classInfo.setProperties(classInfo.getProperties().stream()
        .filter(
            p -> p.getInverseOf() == null || (p.getComment() != null && !p.getComment().isEmpty()))
        .toList());
  }

  protected void filterInterfaces() {
    // Only keep interfaces that are directly used by concrete classes
    this.interfaces = interfaces
        .stream()
        .filter(i -> concreteClasses.stream()
            .flatMap(c -> c.getProperties().stream())
            .anyMatch(p -> p.getRange() != null && p.getRange().contains(i.getUri())))
        .toList();
    // Also filter interfaces that are only used as a superclass of only one concrete class
    this.interfaces = interfaces
        .stream()
        .filter(i -> concreteClasses.stream()
            .filter(c -> c.getSuperClasses() != null && c.getSuperClasses().contains(i))
            .count() > 1)
        .toList();
  }

  protected void filterSuperClasses() {
    // Filter the properties of the interfaces to only include properties
    // used by ALL concrete classes that implement the interface
    this.interfaces = interfaces
        .stream()
        .map(i -> {
          List<ClassInfo> implementingClasses = concreteClasses.stream()
              .filter(c -> c.getSuperClasses() != null && c.getSuperClasses().contains(i))
              .toList();
          if (implementingClasses.isEmpty()) {
            return i;
          }
          List<PropertyInfo> filteredProperties = i.getProperties().stream()
              .filter(p -> implementingClasses.stream()
                  .allMatch(c -> c.getProperties().stream()
                      .anyMatch(cp -> cp.getUri().equals(p.getUri()))))
              .toList();
          i.setProperties(filteredProperties);
          return i;
        })
        .toList();
    // Filter the properties of all classes that extend another class (not interface)
    // to remove properties that are already defined in the super class
    // The properties will be removed from the subclass
    this.concreteClasses = concreteClasses
        .stream()
        .map(c -> {
          // Check the super classes and see which properties are defined
          List<ClassInfo> superClasses = c.getSuperClasses();
          if (superClasses == null || superClasses.isEmpty()) {
            return c;
          }
          // Get all inherited properties from the super classes (except interfaces)
          // Recursive call to super classes to get all inherited properties
          List<PropertyInfo> inheritedProperties = superClasses.stream()
              .filter(sc -> !isInterface(sc) && isConcreteClass(sc))
              .flatMap(sc -> sc.getProperties().stream())
              .toList();
          List<PropertyInfo> filteredProperties = c.getProperties().stream()
              .filter(p -> inheritedProperties.stream()
                  .noneMatch(ip -> ip.getUri().equals(p.getUri())))
              .toList();
          c.setProperties(filteredProperties);
          return c;
        })
        .toList();
    // Remove super classes that have a super class themselves
    // e.g. if A is a subclass of B + C and B is a subclass of C
    // then we can remove C as a super class of A
    this.concreteClasses = concreteClasses
        .stream()
        .map(c -> {
          List<ClassInfo> superClasses = c.getSuperClasses();
          if (superClasses == null) {
            return c;
          }
          // For every super class, check if there are other super classes that are subclasses of it
          // if so, we can remove it from the list of super classes
          List<ClassInfo> filteredSuperClasses = superClasses.stream()
              .filter(sc -> superClasses.stream()
                  // Get the class from (getClasses())
                  .map(other -> getAllClasses().stream()
                      .filter(cc -> cc.getUri().equals(other.getUri()))
                      .findFirst()
                      .orElse(null)).filter(Objects::nonNull)
                  .filter(other -> !other.equals(sc))
                  .noneMatch(other -> other.isSubClassOf(sc.getUri())))
              .toList();
          c.setSuperClasses(filteredSuperClasses);
          return c;
        })
        .toList();
  }

  protected void filterEnums() {
    // Remove enums from interfaces
    this.interfaces = interfaces
        .stream()
        .filter(i -> !enums.containsKey(i))
        .toList();
    // Remove enums from concrete classes
    this.concreteClasses = concreteClasses
        .stream()
        .filter(c -> !enums.containsKey(c))
        .toList();
    this.enums.forEach((enumInfo, enumList) -> {
      // Get a list of all classes that are subclasses of the enum class
      List<ClassInfo> subclasses = getSubClasses(enumInfo)
          .stream()
          // Assume the class does not have any properties; otherwise it would not be an enum subclass
          // Ignore extra attributes that are added by default to all classes
          .filter(c -> c.getProperties().isEmpty() || c.getProperties().stream()
              .allMatch(p -> p.isIdentifier() || getOntologyConfiguration().getExtraProperties()
                  .stream()
                  .anyMatch(ep -> ep.getUri().equals(p.getUri()))))
          .toList();
      // Get a list of all "classes" that are individuals of the enum class
      // TODO
      // Add the subclasses to the enum map
      enums.put(enumInfo, subclasses);
      // Remove the subclasses from the concrete classes and interfaces
      this.concreteClasses = concreteClasses
          .stream()
          .filter(c -> !subclasses.contains(c))
          .toList();
      this.interfaces = interfaces
          .stream()
          .filter(i -> !subclasses.contains(i))
          .toList();
    });
  }

  public ClassInfo getNearestClass(String uri) {
    if (uri == null) {
      return null;
    }
    ClassInfo classInfo = getAllClasses().stream()
        .filter(c -> c.getUri().equals(uri))
        .findFirst()
        .orElse(null);
    if (classInfo == null) {
      return null;
    }
    return getNearestClass(classInfo);
  }

  /**
   * Get the nearest class or interface for a given class.
   *
   * @param classInfo the class for which to find the nearest class or interface
   * @return the nearest class or interface, or null if none found
   */
  public ClassInfo getNearestClass(ClassInfo classInfo) {
    // First check if the class itself is a concrete class or interface
    if (isConcreteClass(classInfo) || isInterface(classInfo) || isEnum(classInfo)) {
      return classInfo;
    }
    // Loop through the interfaces and concrete classes to see if its a super class
    for (ClassInfo interfaceInfo : interfaces) {
      if (interfaceInfo.isSubClassOf(classInfo.getUri())) {
        return interfaceInfo;
      }
    }
    for (ClassInfo enumInfo : enums.keySet()) {
      if (enumInfo.isSubClassOf(classInfo.getUri())) {
        return enumInfo;
      }
    }
    for (ClassInfo concreteClassInfo : concreteClasses) {
      if (concreteClassInfo.isSubClassOf(classInfo.getUri())) {
        return concreteClassInfo;
      }
    }
    return null;
  }

  public boolean isConcreteClass(ClassInfo classInfo) {
    return concreteClasses.stream().map(ClassInfo::getUri)
        .anyMatch(uri -> uri.equals(classInfo.getUri()));
  }

  public boolean isInterface(ClassInfo classInfo) {
    return interfaces.stream().map(ClassInfo::getUri)
        .anyMatch(uri -> uri.equals(classInfo.getUri()));
  }

  public boolean isEnum(ClassInfo classInfo) {
    return enums.keySet().stream().map(ClassInfo::getUri)
        .anyMatch(uri -> uri.equals(classInfo.getUri()));
  }

  public List<ClassInfo> getSubClasses(ClassInfo classInfo) {
    return getAllClasses().stream()
        .filter(c -> c.isSubClassOf(classInfo.getUri()))
        .toList();
  }

  protected String getReadableDataType(String dataTypeName) {
    if (dataTypeName == null) {
      return "String";
    }
    // Capital camel case
    return dataTypeName.substring(0, 1).toUpperCase() + dataTypeName.substring(1);
  }

  public enum ClassType {
    CLASS(null),
    INTERFACE("interface"),
    ENUM("enumerable");

    @Getter
    private final String value;

    ClassType(String value) {
      this.value = value;
    }
  }
}
