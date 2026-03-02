package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.TypescriptGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.jena.atlas.lib.Pair;
import org.jspecify.annotations.Nullable;

public class TypescriptGenerator extends ClassGenerator {

  private final Map<Clazz, String> fileNames = new HashMap<>();

  private final TypescriptGeneratorProperties typescriptGeneratorProperties;

  public TypescriptGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters,
      TypescriptGeneratorProperties typescriptGeneratorProperties) {
    super(ontologyInfo, conceptSchemeInfo, adapters);
    this.typescriptGeneratorProperties = typescriptGeneratorProperties;
  }

  @Override
  public void run() {
    super.run();
    prepareFileNames();
    generateFile(getClasses(), null);
    generateFile(getInterfaces(), "interface");
    generateFile(getEnums(), "enum");
  }

  private String getBasePath() {
    return typescriptGeneratorProperties.getOutputDirectory();
  }

  protected void prepareFileNames() {
    // Prepare file names for all classes, interfaces and enums
    getClasses().forEach(clazz -> fileNames.put(clazz, clazz.getName() + ".ts"));
    getInterfaces().forEach(clazz -> fileNames.put(clazz, clazz.getName() + ".interface.ts"));
    getEnums().forEach(clazz -> fileNames.put(clazz, clazz.getName() + ".enum.ts"));
  }

  protected void generateFile(List<? extends Clazz> classes, @Nullable String type) {
    classes.forEach(clazz -> {
      String typeDeclaration = "class";
      switch (type) {
        case "interface" -> {
          clazz.setName("I" + clazz.getName());
          typeDeclaration = "interface";
        }
        case "enum" -> {
          typeDeclaration = "enum";
        }
        case null -> {
        }
        default -> throw new IllegalStateException("Unexpected value: " + type);
      }

      String fileName = clazz.getName() + ".java";
      StringBuilder builder = new StringBuilder();

      boolean isInterface = clazz instanceof Interface;
      boolean isEnum = clazz instanceof Enum;

      // Begin with imports
      getDependencies(clazz)
          // Create a pair with left the file name and right the class name
          .stream()
          .map(dep -> {
            String depFileName = fileNames.entrySet().stream()
                .filter(entry -> entry.getKey().getName().equals(dep))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Dependency not found: " + dep));
            return new Pair<>(fileName, depFileName);
          })
          .forEach(
              dep -> builder.append("import { ").append(dep.getRight()).append(" } from \"").append(dep.getLeft())
                  .append("\";\n"));
      // Add default imports
      if (!isInterface) {
        // If no properties, we can skip JSON annotations and Lombok imports
        boolean hasProperties = !clazz.getAttributes().isEmpty();
        if (hasProperties) {
          // JSON import
        }
      }
      builder.append("\n");

      // Add comments
      builder.append("/**\n");
      builder.append(" * ").append(clazz.getName()).append("\n");
      builder.append(" * <a href=\"").append(clazz.getUri()).append("\">")
          .append(clazz.getClassInfo().getName()).append("</a>\n");
      builder.append(" **/\n");

      // Determine if it extends or implements other classes/interfaces
      String extendsClause = "";
      if (clazz.getExtendsClass() != null) {
        extendsClause = " extends " + clazz.getExtendsClass().getName();
      }
      String implementsClause = "";
      if (!clazz.getInterfaces().isEmpty()) {
        implementsClause = " implements " + clazz.getInterfaces().stream()
            .map(Clazz::getName)
            .filter(Objects::nonNull)
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
      }

      builder.append("public ").append(typeDeclaration).append(" ").append(clazz.getName())
          .append(extendsClause).append(implementsClause)
          .append(" {\n");
      if (clazz instanceof Enum enumClazz) {
        // For enums, we can add the enum values as constants
        enumClazz.getValues().forEach(value -> {
          // Add comment
          builder.append("\t// ").append(value.getUri()).append("\n");
          builder.append("\t").append(value.getName()).append(",\n");
        });
      }
      clazz.getAttributes().forEach(prop -> {
        // Add comments for the property
        builder.append("\t// ").append("<a href=\"").append(prop.getUri()).append("\">")
            .append(prop.getPropertyInfo().getName()).append("</a>\n");
        // Add JSON annotations
        boolean isArray = prop.getCardinality().isToMany();
        String dataType = getTypescriptType(prop.getDataType()) + (isArray ? "[]" : "");
        builder.append("\t@JsonProperty(\"").append(prop.getPropertyInfo().getName())
            .append("\")\n");

        builder.append("\t").append("private ")
            .append(dataType)
            .append(" ").append(prop.getName()).append(";\n");
      });
      builder.append("}\n");
      // Save to file
      saveToFile(fileName, builder.toString());
    });
  }

  protected List<String> getDependencies(Clazz clazz) {
    // For Java, we can determine dependencies based on the data types of the attributes
    Set<String> dependencies = new HashSet<>(clazz.getAttributes().stream()
        .map(attr -> getTypescriptType(attr.getDataType()))
        .toList());
    // Add extend and implement dependencies
    if (clazz.getExtendsClass() != null) {
      dependencies.add(clazz.getExtendsClass().getName());
    }
    clazz.getInterfaces()
        .forEach(i -> dependencies.add(i.getName()));
    return dependencies.stream().toList();
  }

  protected void saveToFile(String fileName, String content) {
    try {
      Path outputPath = Paths.get(getBasePath(), fileName);
      Files.createDirectories(outputPath.getParent());
      Files.writeString(outputPath, content);
    } catch (IOException e) {
      throw new RuntimeException("Failed to save file: " + fileName, e);
    }
  }

  protected String getTypescriptType(DataType dataType) {
    // First check if it's a known primitive type
    return switch (dataType.getUri()) {
      case "http://www.w3.org/2001/XMLSchema#string" -> "String";
      case "http://www.w3.org/2001/XMLSchema#decimal", "http://www.w3.org/2001/XMLSchema#double",
           "http://www.w3.org/2001/XMLSchema#float", "http://www.w3.org/2001/XMLSchema#integer" ->
          "number";
      case "http://www.w3.org/2001/XMLSchema#boolean" -> "boolean";
      case "http://www.w3.org/2001/XMLSchema#date" -> "Date";
      case "http://www.w3.org/2001/XMLSchema#dateTime" -> "DateTime";
      default -> dataType.getName();
    };
  }
}
