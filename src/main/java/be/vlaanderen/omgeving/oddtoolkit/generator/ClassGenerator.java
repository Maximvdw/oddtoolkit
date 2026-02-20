package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.model.AbstractInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.Scope;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.XSD;
import org.jspecify.annotations.Nullable;

@Getter
public class ClassGenerator extends BaseGenerator {

  protected List<Clazz> classes = new ArrayList<>();
  protected List<Interface> interfaces = new ArrayList<>();
  protected List<Enum> enums = new ArrayList<>();
  protected List<Filter<ClassGenerator>> filters = new ArrayList<>();

  public ClassGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo, List<AbstractAdapter<?>> adapters) {
    super(ontologyInfo, conceptSchemeInfo, adapters);

    filters.add(new Filter<>("interfaces") {
      @Override
      void filter(ClassGenerator generator) {
        generator.filterInterfaces();
        generator.filterInterfaceProperties();
      }
    });
    filters.add(new Filter<>("enums") {
      @Override
      void filter(ClassGenerator generator) {
        generator.filterEnums();
      }
    });
    filters.add(new Filter<>("inheritedProperties") {
      @Override
      void filter(ClassGenerator generator) {
        generator.filterInheritedProperties();
      }
    });
    filters.add(new Filter<>("superclasses") {
      @Override
      void filter(ClassGenerator generator) {
        generator.filterSuperClasses();
      }
    });
    filters.add(new Filter<>("inverseProperties") {
      @Override
      void filter(ClassGenerator generator) {
        generator.getClasses().forEach(generator::filterInverseProperties);
        generator.getInterfaces().forEach(generator::filterInverseProperties);
      }
    });
  }

  @Override
  public void run() {
    super.run();
    extractClasses();
    extractInterfaces();
    extractEnums();
    extractRelations();
    // Apply the filters in order
    filters.forEach(filter -> filter.filter(this));
    updateRanges();
    extractDataTypes();
  }

  private void updateRanges() {
    // Loop through all attributes of all classes and interfaces and update the range to the nearest class or interface
    List<Clazz> completeList = new ArrayList<>();
    completeList.addAll(classes);
    completeList.addAll(interfaces);
    completeList.addAll(enums);
    completeList.forEach(clazz -> clazz.getAttributes().forEach(attribute -> {
      if (attribute.getRange() != null) {
        ClassInfo nearestClass = getNearestClass(attribute.getRange().getClassInfo());
        if (nearestClass != null) {
          attribute.setRange(findNeareast(nearestClass));
          attribute.setDataType(new DataType(attribute.getRange().getName(), attribute.getRange().getUri()));
        }
      }
    }));
  }

  private <T extends Clazz> T createClass(ClassInfo c, Class<T> classType)
      throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    Pair<String, String> nameAndLabel = getClassNameAndLabel(c);
    T clazz = classType.getConstructor().newInstance();
    clazz.setClassInfo(c);
    clazz.setName(nameAndLabel.getLeft());
    clazz.setAttributes(c.getProperties()
        .stream()
        .map(p -> {
          Pair<String, String> propertyNameAndLabel = getPropertyNameAndLabel(p);
          Attribute attribute = new Attribute();
          attribute.setPropertyInfo(p);
          attribute.setName(propertyNameAndLabel.getLeft());
          attribute.setCardinality(getCardinality(p));
          attribute.setDomain(clazz);
          attribute.setPrimaryKey(p.isIdentifier());
          // Check if the property is a relation to another class
          if (p.getRange() != null && p.getRange().stream()
              .noneMatch(uri -> getOntologyClasses().stream()
                  .anyMatch(otherClass -> otherClass.getUri().equals(uri)))) {
            // Determine a data type based on XSD type or default to VARCHAR
            String dataType = p.getRange() != null && !p.getRange().isEmpty() ?
                p.getRange().getFirst() : XSD.xstring.getURI();
            attribute.setDataType(new DataType(null, dataType));
          }
          return attribute;
        })
        .toList());
    return clazz;
  }

  protected void extractDataTypes() {
    // Loop through all attributes of all classes and interfaces and set the data type based on the range of the property
    List<Clazz> completeList = new ArrayList<>();
    completeList.addAll(classes);
    completeList.addAll(interfaces);
    completeList.forEach(clazz -> clazz.getAttributes().forEach(attribute -> {
      if (attribute.getDataType().getName() == null) {
        attribute.getDataType().setName(getReadableDataType(attribute.getDataType().getUri()));
      }
    }));
  }

  protected void extractRelations() {
    // Loop through the classes and interfaces and set the range of the properties to the nearest class or interface
    for (Clazz clazz : classes) {
      for (Attribute attribute : clazz.getAttributes()) {
        if (attribute.getPropertyInfo() != null
            && attribute.getPropertyInfo() instanceof PropertyInfo propertyInfo) {
          if (propertyInfo.getRange() != null && !propertyInfo
              .getRange()
              .isEmpty()) {
            String rangeUri = propertyInfo.getRange().getFirst();
            ClassInfo rangeClass = getNearestClass(rangeUri);
            if (rangeClass != null) {
              // Find the clazz or interface for the range class
              attribute.setRange(findNeareast(rangeClass));
              attribute.setDataType(new DataType(attribute.getRange().getName(), attribute.getRange().getUri()));
            }
          }
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends Clazz> T findNeareast(ClassInfo classInfo) {
    for (Clazz clazz : classes) {
      if (clazz.getUri().equals(classInfo.getUri())) {
        return (T) clazz;
      }
    }
    for (Interface interfaceInfo : interfaces) {
      if (interfaceInfo.getUri().equals(classInfo.getUri())) {
        return (T) interfaceInfo;
      }
    }
    for (Enum enumInfo : enums) {
      if (enumInfo.getUri().equals(classInfo.getUri())) {
        return (T) enumInfo;
      }
    }
    return null;
  }

  protected void extractClasses() {
    this.classes.addAll(getOntologyClasses()
        .stream()
        .map(c -> {
          try {
            return createClass(c, Clazz.class);
          } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                   IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        })
        .toList());
  }

  protected void extractInterfaces() {
    this.interfaces.addAll(getAllClasses()
        .stream()
        .filter(c -> c.getScope() == Scope.EXTERNAL)
        .map(c -> {
          try {
            return createClass(c, Interface.class);
          } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                   IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        })
        .toList());
    // Update the classes to set the interfaces they implement based on the super classes that are interfaces
    this.classes.forEach(clazz -> {
      List<Interface> implementedInterfaces = interfaces.stream()
          .filter(i -> clazz.getClassInfo().getSuperClasses() != null && clazz.getClassInfo()
              .getSuperClasses().stream()
              .anyMatch(ci -> ci.getUri().equals(i.getUri())))
          .toList();
      clazz.setInterfaces(implementedInterfaces);
    });
  }

  protected void extractEnums() {
    this.enums.addAll(getAllClasses()
        .stream()
        .filter(c -> getOntologyConfiguration().getEnumClasses().contains(c.getUri()))
        .map(c -> {
          try {
            return createClass(c, Enum.class);
          } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                   IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        })
        .toList());
  }

  protected void filterInverseProperties(Clazz clazz) {
    // Filter inverse properties and copy the cardinality
    // Then we can remove the inverse properties from the class and only keep the original properties
    clazz.getAttributes()
        .stream()
        .filter(a -> a.getPropertyInfo() != null
            && a.getPropertyInfo() instanceof PropertyInfo propertyInfo
            && propertyInfo.getInverseOf() != null)
        .forEach(a -> {
          PropertyInfo propertyInfo = (PropertyInfo) a.getPropertyInfo();
          getAllClasses().stream()
              .flatMap(c -> c.getProperties().stream())
              .filter(ip -> ip.getUri().equals(propertyInfo.getInverseOf()))
              .findFirst()
              .ifPresent(inverseProperty -> {
                propertyInfo.setCardinalityFrom(inverseProperty.getCardinalityTo());
                inverseProperty.setCardinalityFrom(propertyInfo.getCardinalityTo());
                // Check that one of the properties has a comment, if not we can keep either one of them
                if ((propertyInfo.getComment() == null || propertyInfo.getComment().isEmpty()) && (
                    inverseProperty.getComment() == null || inverseProperty.getComment()
                        .isEmpty())) {
                  // Keep the property with the lower URI (arbitrary choice to keep one of them)
                  if (propertyInfo.getUri().compareTo(inverseProperty.getUri()) < 0) {
                    propertyInfo.setComment("Inverse property of " + inverseProperty.getUri());
                  } else {
                    inverseProperty.setComment("Inverse property of " + propertyInfo.getUri());
                  }
                }
              });
        });
    // If the class is an interface then remove all inverse properties
    if (isInterface(clazz.getClassInfo())) {
      clazz.setAttributes(clazz.getAttributes().stream()
          .filter(a -> a.getPropertyInfo() == null || (
              a.getPropertyInfo() instanceof PropertyInfo propertyInfo
                  && propertyInfo.getInverseOf() == null))
          .toList());
      return;
    }
    // Remove inverse properties if they don't have a comment
    List<Attribute> remainingProperties = clazz.getAttributes()
        .stream()
        .filter(
            p -> {
              PropertyInfo propertyInfo = (PropertyInfo) p.getPropertyInfo();
              return propertyInfo.getInverseOf() == null || (propertyInfo.getComment() != null && !propertyInfo.getComment().isEmpty());
            })
        .toList();
    clazz.setAttributes(remainingProperties);
  }

  protected void filterInterfaces() {
    // Only keep interfaces that are directly used by concrete classes
    this.interfaces = interfaces
        .stream()
        .filter(i -> classes.stream()
            .flatMap(c -> c.getAttributes().stream())
            .anyMatch(p -> p.getRange() != null && p.getRange().getUri().equals(i.getUri())))
        .toList();
    // Also filter interfaces that are only used as a superclass of only one concrete class
    this.interfaces = interfaces
        .stream()
        .filter(i -> classes.stream()
            .filter(c -> c.getInterfaces() != null && c.getInterfaces().stream()
                .anyMatch(ci -> ci.getUri().equals(i.getUri())))
            .count() > 1)
        .toList();
    // Filter the used filters in classes to only include the interfaces that are still kept
    this.classes = classes
        .stream()
        .peek(c -> {
          List<Interface> filteredInterfaces = c.getInterfaces().stream()
              .filter(i -> interfaces.stream()
                  .anyMatch(keptInterface -> keptInterface.getUri().equals(i.getUri())))
              .toList();
          c.setInterfaces(filteredInterfaces);
        })
        .toList();
  }

  protected void filterInterfaceProperties() {
    // Filter the properties of the interfaces to only include properties
    // used by ALL concrete classes that implement the interface
    this.interfaces = interfaces
        .stream()
        .map(i -> {
          List<Clazz> implementingClasses = classes.stream()
              .filter(c -> c.getInterfaces() != null && c.getInterfaces().stream()
                  .anyMatch(ci -> ci.getUri().equals(i.getUri())))
              .toList();
          if (implementingClasses.isEmpty()) {
            return i;
          }
          List<Attribute> filteredProperties = i.getAttributes().stream()
              .filter(p -> implementingClasses.stream()
                  .allMatch(c -> c.getAttributes().stream()
                      .anyMatch(cp -> cp.getPropertyInfo().getUri()
                          .equals(p.getPropertyInfo().getUri()))))
              .toList();
          i.setAttributes(filteredProperties);
          return i;
        })
        .toList();
  }

  protected void filterInheritedProperties() {
    // Filter the properties of all classes that extend another class (not interface)
    // to remove properties that are already defined in the super class
    // The properties will be removed from the subclass
    this.classes = classes
        .stream()
        .map(c -> {
          // Check the super classes and see which properties are defined
          List<ClassInfo> superClasses = c.getClassInfo().getSuperClasses()
              .stream().filter(this::isConcreteClass)
              .toList();
          if (superClasses.isEmpty()) {
            return c;
          }
          List<PropertyInfo> inheritedProperties = superClasses.stream()
              .flatMap(sc -> getAllClasses().stream()
                  .filter(other -> other.getUri().equals(sc.getUri()))
                  .flatMap(other -> other.getProperties().stream()))
              .toList();
          List<Attribute> filteredProperties = c.getAttributes().stream()
              .filter(p -> inheritedProperties.stream()
                  .noneMatch(ip -> ip.getUri().equals(p.getPropertyInfo().getUri())))
              .toList();
          c.setAttributes(filteredProperties);
          return c;
        })
        .toList();
  }

  protected void filterSuperClasses() {
    // Remove super classes that have a super class themselves
    // e.g. if A is a subclass of B + C and B is a subclass of C
    // then we can remove C as a super class of A
    this.classes = classes
        .stream()
        .map(c -> {
          List<ClassInfo> superClasses = c.getClassInfo().getSuperClasses();
          if (superClasses == null) {
            return c;
          }
          // For every super class, check if there are other super classes that are subclasses of it
          // if so, we can remove it from the list of super classes
          Clazz superClass = superClasses.stream()
              .filter(sc -> superClasses.stream()
                  // Get the class from (getClasses())
                  .map(other -> getAllClasses().stream()
                      .filter(cc -> cc.getUri().equals(other.getUri()))
                      .findFirst()
                      .orElse(null)).filter(Objects::nonNull)
                  .filter(other -> !other.equals(sc))
                  .noneMatch(other -> other.isSubClassOf(sc.getUri())))
              // Super class should be a concrete class
              .filter(sc -> classes.stream()
                  .anyMatch(cc -> cc.getUri().equals(sc.getUri())))
              .map(this::getClass)
              .findFirst()
              .orElse(null);
          c.setExtendsClass(superClass);
          return c;
        })
        .toList();
  }


  protected void filterEnums() {
    // Remove enums from interfaces
    this.interfaces = interfaces
        .stream()
        .filter(i -> !isEnum(i.getClassInfo()))
        .toList();
    // Remove enums from concrete classes
    this.classes = classes
        .stream()
        .filter(c -> !isEnum(c.getClassInfo()))
        .toList();
    this.enums.forEach(enumInfo -> {
      // Clear all properties
      enumInfo.setAttributes(new ArrayList<>());
      // Get a list of all classes that are subclasses of the enum class
      List<ClassInfo> subclasses = new ArrayList<>();
      getSubClasses(enumInfo.getClassInfo())
          .stream()
          // Assume the class does not have any properties; otherwise it would not be an enum subclass
          // Ignore extra attributes that are added by default to all classes
          .filter(c -> c.getProperties().isEmpty() || c.getProperties().stream()
              .allMatch(p -> p.isIdentifier() || getOntologyConfiguration().getExtraProperties()
                  .stream()
                  .anyMatch(ep -> ep.getUri().equals(p.getUri()))))
          .forEach(subclasses::add);
      // Get a list of all "classes" that are individuals of the enum class
      enumInfo.getClassInfo().getIndividuals()
          .stream()
          .map(i -> new ClassInfo(enumInfo.getClassInfo().getScope(), i))
          .forEach(subclasses::add);
      enumInfo.setValues(subclasses
          .stream().map(clazz -> {
            EnumValue enumValue = new EnumValue();
            enumValue.setPropertyInfo(clazz);
            // Set to UPPER_SNAKE_CASE
            enumValue.setName(toEnumValueName(clazz.getName()));
            return enumValue;
          })
          .toList());
      // Remove the subclasses from the concrete classes and interfaces
      this.classes = classes
          .stream()
          .filter(c -> !subclasses.contains(c.getClassInfo()))
          .toList();
      this.interfaces = interfaces
          .stream()
          .filter(i -> !subclasses.contains(i.getClassInfo()))
          .toList();
    });
  }

  public ClassInfo getNearestClass(String classUri) {
    if (classUri == null) {
      return null;
    }
    // Loop through all classes, the nearest class is the
    ClassInfo classInfo = getAllClasses()
        .stream()
        .filter(c -> c.getUri().equals(classUri))
        .findFirst()
        .orElse(null);
    if (classInfo == null) {
      return null;
    }
    return getNearestClass(classInfo);
  }

  protected String toEnumValueName(String name) {
    return toSnakeCase(name).toUpperCase();
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
    for (Interface interfaceInfo : interfaces) {
      if (interfaceInfo.getClassInfo().isSubClassOf(classInfo.getUri())) {
        return interfaceInfo.getClassInfo();
      }
    }
    for (Enum enumInfo : enums) {
      if (enumInfo.getClassInfo().isSubClassOf(classInfo.getUri())) {
        return enumInfo.getClassInfo();
      }
    }
    for (Clazz concreteClassInfo : classes) {
      if (concreteClassInfo.getClassInfo().isSubClassOf(classInfo.getUri())) {
        return concreteClassInfo.getClassInfo();
      }
    }
    return null;
  }

  public boolean isConcreteClass(ClassInfo classInfo) {
    return classes
        .stream().map(Clazz::getClassInfo)
        .anyMatch(uri -> uri.getUri().equals(classInfo.getUri()));
  }

  public boolean isInterface(ClassInfo classInfo) {
    return interfaces.stream().map(Interface::getClassInfo)
        .anyMatch(uri -> uri.getUri().equals(classInfo.getUri()));
  }

  public boolean isEnum(ClassInfo classInfo) {
    return enums.stream().map(Enum::getClassInfo)
        .anyMatch(uri -> uri.getUri().equals(classInfo.getUri()));
  }

  public List<ClassInfo> getSubClasses(ClassInfo classInfo) {
    return getAllClasses().stream()
        .filter(c -> c.isSubClassOf(classInfo.getUri()))
        .toList();
  }

  protected String getReadableDataType(String dataTypeUri) {
    // Try to find a class, interface, enum first
    Clazz clazz = getClass(getNearestClass(dataTypeUri));
    if (clazz != null) {
      return clazz.getName();
    }
    // Extract the local name from the URI
    Resource resource = getOntologyInfo().getModel().createResource(dataTypeUri);
    // Convert to a readable name (e.g. "string" -> "String")
    String localName = resource.getLocalName();
    if (localName == null) {
      localName = dataTypeUri;
    }
    return localName.substring(0, 1).toUpperCase() + localName.substring(1);
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

  protected Interface getInterface(ClassInfo classInfo) {
    return interfaces.stream()
        .filter(i -> i.getUri().equals(classInfo.getUri()))
        .findFirst()
        .orElse(null);
  }

  protected Clazz getClass(ClassInfo classInfo) {
    if (classInfo == null) {
      return null;
    }
    // Search in classes, interfaces and enums
    List<Clazz> completeList = new ArrayList<>();
    completeList.addAll(classes);
    completeList.addAll(interfaces);
    completeList.addAll(enums);
    return completeList.stream()
        .filter(c -> c.getUri().equals(classInfo.getUri()))
        .findFirst()
        .orElse(null);
  }

  protected Enum getEnum(ClassInfo classInfo) {
    return enums.stream()
        .filter(e -> e.getUri().equals(classInfo.getUri()))
        .findFirst()
        .orElse(null);
  }

  protected Cardinality getCardinality(PropertyInfo property) {
    Cardinality cardinality;
    switch (property.getCardinalityFrom().getMax() == null
        || property.getCardinalityFrom().getMax() > 1) {
      case false -> {
        switch (property.getCardinalityTo().getMax() == null
            || property.getCardinalityTo().getMax() > 1) {
          case false -> cardinality = Cardinality.ONE_TO_ONE;
          case true -> cardinality = Cardinality.ONE_TO_MANY;
        }
      }
      case true -> {
        switch (property.getCardinalityTo().getMax() == null
            || property.getCardinalityTo().getMax() > 1) {
          case false -> cardinality = Cardinality.MANY_TO_ONE;
          case true -> cardinality = Cardinality.MANY_TO_MANY;
        }
      }
    }
    return cardinality;
  }

  @Getter
  protected abstract static class Filter<T extends ClassGenerator> {

    private final String name;

    public Filter(String name) {
      this.name = name;
    }

    public Filter() {
      this(null);
    }

    abstract void filter(T generator);
  }

  @Getter
  @Setter
  protected static class Clazz {

    private List<Interface> interfaces = new ArrayList<>();
    private @Nullable Clazz extendsClass;
    private ClassInfo classInfo;
    private String name;
    private List<Attribute> attributes = new ArrayList<>();

    public Clazz() {}

    public String getUri() {
      return classInfo != null ? classInfo.getUri() : null;
    }

    public String toString() {
      return name;
    }
  }

  @Getter
  @Setter
  public static class Interface extends Clazz {

  }

  @Getter
  @Setter
  public static class Enum extends Clazz {

    private List<EnumValue> values = new ArrayList<>();
  }

  @Getter
  @Setter
  public static class EnumValue extends Attribute {

  }

  @Getter
  @Setter
  protected static class Attribute {

    private AbstractInfo propertyInfo;
    private String name;
    private Cardinality cardinality;
    private Clazz domain;
    private Clazz range;
    private DataType dataType;
    private boolean primaryKey;
    private boolean nullable;

    public String toString() {
      return name;
    }
  }

  @Getter
  @Setter
  protected static class DataType {
      private String name;
      private String uri;

      public DataType(String name, String uri) {
        this.name = name;
        this.uri = uri;
      }

      public String toString() {
        return name != null ? name : uri;
      }
  }
}
