package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.ClassDiagramProperties;
import be.vlaanderen.omgeving.oddtoolkit.config.DiagramGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassConceptInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.util.List;
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
    for (Clazz classInfo : getClasses()) {
      generateClass(builder, classInfo, ClassType.CLASS);
    }
    for (Interface interfaceInfo : getInterfaces()) {
      generateClass(builder, interfaceInfo, ClassType.INTERFACE);
    }
    for (Enum enumInfo : getEnums()) {
      generateClass(builder, enumInfo, ClassType.ENUM);
    }

    // Emit class diagram style definitions (classDef ..) local to this generator
    emitStyleDefinitions(builder);
  }

  // Keep the existing helper methods but make them protected so DiagramGenerator can call them
  protected void generateClass(StringBuilder builder, Clazz classInfo, ClassType type) {
    // Get the concept class
    ClassConceptInfo classConceptInfo = getClassConceptForClass(classInfo.getUri());
    String className = classConceptInfo != null ? classConceptInfo.getName() : classInfo.getName();
    // Add documentation as comment
    builder.append("%% ").append(classInfo.getUri()).append("\n");
    // apply style if configured
    String style = getStyleForClass(classInfo.getClassInfo());
    builder.append("class ").append(className);
    if (style != null) {
      builder.append(":::").append(style);
    }
    builder.append(" {\n");
    if (type != ClassType.CLASS) {
      builder.append("  <<").append(type.getValue()).append(">>\n");
    }
    if (type == ClassType.ENUM) {
      List<EnumValue> enumValues = getEnum(classInfo.getClassInfo()).getValues();
      for (EnumValue enumValue : enumValues) {
        builder.append("  ").append(enumValue.getName()).append("\n");
      }
    } else {
      for (Attribute attribute : classInfo.getAttributes()) {
        generateProperty(builder, attribute);
      }
    }
    builder.append("}\n");
    // Generate relations
    generateRelations(builder, classInfo);
  }

  protected void generateRelations(StringBuilder builder, Clazz classInfo) {
    for (Attribute attribute : classInfo.getAttributes()) {
      if (attribute.getRange() != null) {
        Clazz domainClass = attribute.getRange();
        builder.append("%% ").append(attribute.getPropertyInfo().getUri()).append("\n");
        builder.append(classInfo.getName()).append(" --> ")
            .append(domainClass.getName())
            .append(" : ").append(attribute.getName()).append("\n");
      }
    }
    // Generate subclass relations for classes that are in concrete classes or interfaces, but not for enums
    for (Interface superInterface : classInfo.getInterfaces()) {
        builder.append(superInterface.getName()).append(" <|-- ")
            .append(classInfo.getName()).append("\n");
    }
    if (classInfo.getExtendsClass() != null) {
      builder.append(classInfo.getExtendsClass().getName()).append(" <|-- ")
          .append(classInfo.getName()).append("\n");
    }
  }

  protected void generateProperty(StringBuilder builder, Attribute propertyInfo) {
    // Get the data type of the property
    String dataTypeName = propertyInfo.getDataType().getName();
    // Determine if it is an array
    if (propertyInfo.getCardinality().isToMany()) {
      dataTypeName += "[]";
    }

    // Determine if its an identifier
    if (propertyInfo.isPrimaryKey()) {
      dataTypeName = "+" + dataTypeName;
    }

    builder.append("  ").append(dataTypeName).append(" ").append(propertyInfo.getName()).append("\n");
  }
}
