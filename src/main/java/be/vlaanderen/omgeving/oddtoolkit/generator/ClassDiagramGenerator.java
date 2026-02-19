package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.ClassDiagramProperties;
import be.vlaanderen.omgeving.oddtoolkit.config.DiagramGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassConceptInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import java.util.List;
import java.util.Map;
import org.apache.jena.atlas.lib.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassDiagramGenerator extends DiagramGenerator {
  private static final Logger logger = LoggerFactory.getLogger(
      ClassDiagramGenerator.class);

  private final ClassDiagramProperties generatorProperties;

  public ClassDiagramGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters,
      ClassDiagramProperties generatorProperties,
      DiagramGeneratorProperties diagramGeneratorProperties) {
    super(ontologyInfo, conceptSchemeInfo, adapters, diagramGeneratorProperties);
    this.generatorProperties = generatorProperties;
  }

  @Override
  public void run() {
    super.run();
    String classDiagram = generate("classDiagram");
    // Save to file
    if (getOutputFile() != null) {
      logger.info("Writing class diagram to {}", getOutputFile());
      saveToFile(getOutputFile(), classDiagram);
    } else {
      System.out.println(classDiagram);
    }
  }

  @Override
  protected String getOutputFile() {
    return generatorProperties.getOutputFile();
  }

  @Override
  protected void renderContent(StringBuilder builder, String type) {
    // Generate classes and interfaces
    for (ClassInfo classInfo : getConcreteClasses()) {
      generateClass(builder, classInfo, ClassType.CLASS);
    }
    for (ClassInfo interfaceInfo : getInterfaces()) {
      generateClass(builder, interfaceInfo, ClassType.INTERFACE);
    }
    for (ClassInfo enumInfo : getEnums().keySet()) {
      generateClass(builder, enumInfo, ClassType.ENUM);
    }

    // Emit class diagram style definitions (classDef ..) local to this generator
    emitStyleDefinitions(builder);
  }

  // Keep the existing helper methods but make them protected so DiagramGenerator can call them
  protected void generateClass(StringBuilder builder, ClassInfo classInfo, ClassType type) {
    // Get the concept class
    ClassConceptInfo classConceptInfo = getClassConceptForClass(classInfo.getUri());
    String className = classConceptInfo != null ? classConceptInfo.getName() : classInfo.getName();
    // Add documentation as comment
    builder.append("%% ").append(classInfo.getUri()).append("\n");
    // apply style if configured
    String style = getStyleForClass(classInfo);
    builder.append("class ").append(className);
    if (style != null) {
      builder.append(":::").append(style);
    }
    builder.append(" {\n");
    if (type != ClassType.CLASS) {
      builder.append("  <<").append(type.getValue()).append(">>\n");
    }
    if (type == ClassType.ENUM) {
      List<ClassInfo> enumValues = getEnums().get(classInfo);
      for (ClassInfo enumValue : enumValues) {
        // Format the enum name
        String enumValueName = enumValue.getName()
            .toUpperCase().replace(classInfo.getName().toUpperCase(), "");
        builder.append("  ").append(enumValueName).append("\n");
      }
    } else {
      for (PropertyInfo propertyInfo : classInfo.getProperties()) {
        generateProperty(builder, propertyInfo);
      }
    }
    builder.append("}\n");
    // Generate relations
    generateRelations(builder, classInfo);
  }

  protected void generateRelations(StringBuilder builder, ClassInfo classInfo) {
    Pair<String, String> classNameAndLabel = getClassNameAndLabel(classInfo);
    for (PropertyInfo propertyInfo : classInfo.getProperties()) {
      if (propertyInfo.getRange() != null) {
        for (String domainUri : propertyInfo.getRange()) {
          ClassInfo domainClass = getNearestClass(domainUri);
          if (domainClass != null) {
            // Get the concept class for the domain class
            Pair<String, String> domainClassNameAndLabel = getClassNameAndLabel(domainClass);
            // Get the concept property for the property
            Pair<String, String> propertyNameAndLabel = getPropertyNameAndLabel(propertyInfo);
            String propertyLabel = propertyNameAndLabel.getRight();
            builder.append("%% ").append(propertyInfo.getUri()).append("\n");
            builder.append(classNameAndLabel.getLeft()).append(" --> ")
                .append(domainClassNameAndLabel.getLeft())
                .append(" : ").append(propertyLabel).append("\n");
          }
        }
      }
    }
    // Generate subclass relations for classes that are in concrete classes or interfaces, but not for enums
    for (ClassInfo superClass : classInfo.getSuperClasses()) {
      if (getConcreteClasses().contains(superClass) || getInterfaces().contains(superClass)) {
        // Get the concept class for the superclass
        Pair<String, String> superClassNameAndLabel = getClassNameAndLabel(superClass);
        builder.append(superClassNameAndLabel.getLeft()).append(" <|-- ")
            .append(classNameAndLabel.getLeft()).append("\n");
      }
    }
  }

  protected void generateProperty(StringBuilder builder, PropertyInfo propertyInfo) {
    // Get the data type of the property
    String dataType = propertyInfo.getRange() != null && !propertyInfo.getRange().isEmpty()
        ? propertyInfo.getRange().getFirst() : null;
    ClassInfo dataTypeClass = getNearestClass(dataType);
    if (dataTypeClass == null && dataType != null) {
      dataTypeClass = getXsdType(dataType);
      if (dataTypeClass != null) {
        dataTypeClass.setName(this.getReadableDataType(dataTypeClass.getName()));
      }
    }
    ClassConceptInfo dataTypeClassInfo =
        dataTypeClass != null ? getClassConceptForClass(dataTypeClass.getUri()) : null;

    String dataTypeName =
        dataTypeClassInfo != null ? dataTypeClassInfo.getName()
            : (dataTypeClass != null ? dataTypeClass.getName() : "String");
    // Determine if it is an array
    if (propertyInfo.getCardinalityTo().getMax() == null
        || propertyInfo.getCardinalityTo().getMax() > 1) {
      dataTypeName += "[]";
    }

    // Determine if its an identifier
    if (propertyInfo.isIdentifier()) {
      dataTypeName = "+" + dataTypeName;
    }

    // Get the concept property
    String propertyName = getPropertyNameAndLabel(propertyInfo).getLeft();
    builder.append("  ").append(dataTypeName).append(" ").append(propertyName).append("\n");
  }

  // Emit class diagram style definitions (classDef ...) from DiagramGenerator properties
  private void emitStyleDefinitions(StringBuilder builder) {
    List<DiagramStyle> styles = getStyleEntries();
    if (styles == null) return;
    for (DiagramStyle style : styles) {
      if (style != null && style.uri != null && style.name != null && style.props != null) {
        builder.append("classDef ").append(style.name).append(" ");
        int i = 0;
        for (Map.Entry<String, Object> e : style.props.entrySet()) {
          if (i++ > 0) builder.append(',');
          builder.append(e.getKey()).append(":").append(e.getValue());
        }
        builder.append("\n");
      }
    }
  }
}
