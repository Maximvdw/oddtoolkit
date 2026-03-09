# Extension Guide

This guide explains how to extend the ODD Toolkit with custom generators and adapters, making it truly adaptable to your specific needs.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Creating Custom Generators](#creating-custom-generators)
3. [Creating Custom Adapters](#creating-custom-adapters)
4. [Registering Components](#registering-components)
5. [Complete Example: PlantUML Generator](#complete-example-plantuml-generator)
6. [Testing Custom Components](#testing-custom-components)
7. [Best Practices](#best-practices)

## Architecture Overview

The ODD Toolkit is built on an extensible architecture with three main extension points:

### 1. **Generator Registry Pattern**

```
GeneratorRegistry (Interface)
    ↓
DefaultGeneratorRegistry (Implementation)
    ↓
Specific Generators (Your Custom Generators)
```

### 2. **Component Model**

```
OntologyInfo + ConceptSchemeInfo (Input Data)
    ↓
Adapters (Transform & Extract Data)
    ↓
Generators (Generate Output)
    ↓
Generated Output Files
```

### 3. **Configuration System**

```
application.yml / JSON File (Base Config)
    ↓
CLI Arguments (Overrides)
    ↓
Environment Variables (Fallback)
    ↓
Final Configuration
```

## Creating Custom Generators

### Step 1: Extend BaseGenerator

All generators should extend `BaseGenerator`, which provides common functionality and configuration handling.

```java
package com.example.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.generator.BaseGenerator;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.util.List;
import java.util.Map;

public class CustomGenerator extends BaseGenerator {

  public CustomGenerator(
      OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters,
      Map<String, Object> config) {
    super(ontologyInfo, conceptSchemeInfo, adapters, config);
  }

  @Override
  public String getName() {
    return "custom-generator";
  }

  @Override
  public String getDescription() {
    return "Generates custom output from ontology data";
  }

  @Override
  public void generate() throws Exception {
    // Your generation logic here
    validate();
    
    // Example: process ontology classes
    System.out.println("Generating custom output...");
    // TODO: Implement your generator logic
  }

  @Override
  public void validate() throws IllegalStateException {
    super.validate();
    // Add custom validation here if needed
  }
}
```

### Step 2: Register as Spring Bean

Create a Spring configuration class to register your generator:

```java
package com.example.oddtoolkit.config;

import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import com.example.oddtoolkit.generator.CustomGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomGeneratorConfiguration {

  @Bean
  public CustomGenerator customGenerator(
      OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo) {
    
    // Adapters can be injected if needed
    return new CustomGenerator(
        ontologyInfo,
        conceptSchemeInfo,
        List.of(), // or inject specific adapters
        Map.of()   // configuration
    );
  }
}
```

### Step 3: Update GeneratorConfiguration (If Needed)

If integrating with the existing configuration system, update `GeneratorConfiguration` to include your new generator in the registry:

```java
// In GeneratorConfiguration.java

@Bean
public GeneratorRegistrationHelper generatorRegistrationHelper(
    GeneratorRegistry registry,
    // ... existing generators ...
    CustomGenerator customGenerator) {
  
  return new GeneratorRegistrationHelper(
      registry,
      // ... pass existing generators ...
      customGenerator  // add your generator
  );
}
```

Then update `GeneratorRegistrationHelper.registerGenerators()`:

```java
@PostConstruct
public void registerGenerators() {
  logger.info("Registering generators with GeneratorRegistry");
  registry.register("custom-generator", customGenerator);
  // ... existing registrations ...
}
```

## Creating Custom Adapters

### Adapter Purpose

Adapters transform and extract data from the ontology before it's used by generators. Common uses:

- Load and process ontology files
- Extract specific classes or properties
- Transform ontology data into generator-specific formats
- Filter or override ontology definitions

### Step 1: Extend AbstractAdapter

```java
package com.example.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.adapter.AdapterDependency;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.ontology.OntologyClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomDataTransformerAdapter extends AbstractAdapter<OntologyClass> {

  private static final Logger logger = LoggerFactory.getLogger(CustomDataTransformerAdapter.class);

  @Override
  public String getName() {
    return "customDataTransformerAdapter";
  }

  @Override
  public List<AdapterDependency> getDependencies() {
    return new ArrayList<>(); // No dependencies if this is a first adapter
  }

  @Override
  public void execute(OntologyInfo ontologyInfo) throws Exception {
    logger.info("Executing custom data transformation");
    
    // Example: Process all ontology classes
    ontologyInfo.getOntologyClasses().forEach(ontClass -> {
      logger.debug("Processing class: {}", ontClass.getURI());
      // Your transformation logic here
    });
  }

  @Override
  public List<OntologyClass> getData() {
    // Return transformed data
    return new ArrayList<>();
  }
}
```

### Step 2: Register as Spring Component

```java
package com.example.oddtoolkit.adapter;

import org.springframework.stereotype.Component;

@Component
public class CustomDataTransformerAdapter extends AbstractAdapter<OntologyClass> {
  // ... implementation ...
}
```

## Registering Components

### Method 1: Spring Component Annotation (Recommended)

```java
@Component
public class MyCustomAdapter extends AbstractAdapter<Object> {
  // Auto-registered via component scanning
}

@Configuration
public class MyGeneratorConfig {
  
  @Bean
  public MyCustomGenerator myCustomGenerator(
      OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo) {
    return new MyCustomGenerator(ontologyInfo, conceptSchemeInfo, List.of(), Map.of());
  }
}
```

### Method 2: Explicit Bean Registration

```java
@Configuration
public class PluginConfiguration {

  @Bean
  public MyCustomAdapter myCustomAdapter() {
    return new MyCustomAdapter();
  }

  @Bean
  public MyCustomGenerator myCustomGenerator(
      OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      MyCustomAdapter adapter) {
    return new MyCustomGenerator(ontologyInfo, conceptSchemeInfo, List.of(adapter), Map.of());
  }
}
```

## Complete Example: PlantUML Generator

Here's a complete, runnable example of creating a custom PlantUML diagram generator.

### File: `src/main/java/com/example/oddtoolkit/generator/PlantUmlGenerator.java`

```java
package com.example.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.generator.BaseGenerator;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates PlantUML class diagrams from ontology.
 * PlantUML can be converted to multiple formats (PNG, SVG, PDF) using PlantUML tools.
 */
public class PlantUmlGenerator extends BaseGenerator {

  private static final Logger logger = LoggerFactory.getLogger(PlantUmlGenerator.class);

  public PlantUmlGenerator(
      OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters,
      Map<String, Object> config) {
    super(ontologyInfo, conceptSchemeInfo, adapters, config);
  }

  @Override
  public String getName() {
    return "plantuml";
  }

  @Override
  public String getDescription() {
    return "Generates PlantUML class diagrams from ontology";
  }

  @Override
  public void generate() throws Exception {
    validate();
    
    String outputPath = getConfigString("output-path");
    if (outputPath == null) {
      outputPath = "target/plantuml-diagram.puml";
    }

    File outputFile = new File(outputPath);
    outputFile.getParentFile().mkdirs();

    StringBuilder diagram = new StringBuilder();
    diagram.append("@startuml\n");
    diagram.append("skinparam backgroundColor #FEFEFE\n");
    diagram.append("skinparam class {\n");
    diagram.append("  BackgroundColor #F3F3FF\n");
    diagram.append("  ArrowColor #000000\n");
    diagram.append("  BorderColor #666666\n");
    diagram.append("}\n\n");

    // Generate classes from ontology
    // This is a simplified example - adapt to your ontology structure
    ontologyInfo.getOntologyClasses().forEach(ontClass -> {
      String className = ontClass.getLocalName();
      diagram.append("class ").append(className).append(" {\n");
      
      // Add properties
      ontClass.listDeclaredProperties().forEachRemaining(prop -> {
        String propName = prop.getLocalName();
        diagram.append("  ").append(propName).append(": Object\n");
      });
      
      diagram.append("}\n\n");
    });

    diagram.append("@enduml\n");

    // Write to file
    try (FileWriter writer = new FileWriter(outputFile)) {
      writer.write(diagram.toString());
      logger.info("PlantUML diagram generated: {}", outputFile.getAbsolutePath());
    }
  }
}
```

### File: `src/main/java/com/example/oddtoolkit/config/PlantUmlGeneratorConfig.java`

```java
package com.example.oddtoolkit.config;

import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import com.example.oddtoolkit.generator.PlantUmlGenerator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlantUmlGeneratorConfig {

  @Bean
  public PlantUmlGenerator plantUmlGenerator(
      OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo) {
    
    Map<String, Object> config = new HashMap<>();
    config.put("output-path", "target/plantuml-diagram.puml");
    
    return new PlantUmlGenerator(
        ontologyInfo,
        conceptSchemeInfo,
        List.of(),
        config
    );
  }
}
```

### Usage

```bash
# Build the project
mvn clean package

# Run with PlantUML generator
java -jar oddtoolkit.jar --generator=plantuml

# Generate the diagram image (requires PlantUML installed)
plantuml target/plantuml-diagram.puml -png
```

## Testing Custom Components

### Unit Testing a Generator

```java
package com.example.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class PlantUmlGeneratorTest {

  private PlantUmlGenerator generator;
  private OntologyInfo ontologyInfo;
  private ConceptSchemeInfo conceptSchemeInfo;

  @BeforeEach
  void setUp() {
    // Mock or create test instances
    // ontologyInfo = createTestOntology();
    // conceptSchemeInfo = createTestConceptScheme();
    // generator = new PlantUmlGenerator(ontologyInfo, conceptSchemeInfo, List.of(), Map.of());
  }

  @Test
  void testGenerationSucceeds() throws Exception {
    // Arrange
    // generator.getConfig().put("output-path", "target/test-output.puml");

    // Act
    // generator.generate();

    // Assert
    // assertTrue(new File("target/test-output.puml").exists());
  }

  @Test
  void testValidationFails() {
    // Arrange
    // PlantUmlGenerator invalidGenerator = new PlantUmlGenerator(null, null, List.of(), Map.of());

    // Act & Assert
    // assertThrows(IllegalStateException.class, invalidGenerator::validate);
  }
}
```

## Best Practices

### 1. **Extend BaseGenerator for Consistency**

Always extend `BaseGenerator` to ensure your generator:
- Has consistent configuration handling
- Implements required methods
- Integrates with the registry system

### 2. **Use Dependency Injection**

```java
@Configuration
public class MyGeneratorConfig {

  @Bean
  public MyAdapter myAdapter() {
    return new MyAdapter();
  }

  @Bean
  public MyGenerator myGenerator(
      OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      MyAdapter adapter) {  // Injected dependency
    return new MyGenerator(ontologyInfo, conceptSchemeInfo, List.of(adapter), Map.of());
  }
}
```

### 3. **Implement Proper Error Handling**

```java
@Override
public void generate() throws Exception {
  try {
    validate();
    // Generation logic
  } catch (IOException e) {
    logger.error("Failed to write output file", e);
    throw new GenerationException("Generation failed", e);
  }
}
```

### 4. **Use Configuration Methods**

```java
String outputPath = getConfigString("output-path");
boolean verbose = getConfigBoolean("verbose", false);
int maxDepth = Integer.parseInt(getConfigString("max-depth", "5"));
```

### 5. **Log Important Events**

```java
@Override
public void generate() throws Exception {
  logger.info("Starting generation with generator: {}", getName());
  logger.debug("Configuration: {}", config);
  
  // Generation logic
  
  logger.info("Generation completed successfully");
}
```

### 6. **Document Your Generator**

```java
/**
 * Generates PlantUML class diagrams from ontology data.
 *
 * Configuration:
 * - output-path: Path to output file (default: target/plantuml-diagram.puml)
 * - style: PlantUML style preset (default: default)
 *
 * Usage:
 * java -jar oddtoolkit.jar --generator=plantuml --output=/tmp/diagram.puml
 */
public class PlantUmlGenerator extends BaseGenerator {
  // ...
}
```

## Next Steps

- Review existing generators in `src/main/java/be/vlaanderen/omgeving/oddtoolkit/generator/`
- Study adapter implementations in `src/main/java/be/vlaanderen/omgeving/oddtoolkit/adapter/`
- Check test examples in `src/test/java/be/vlaanderen/omgeving/oddtoolkit/`
- Read [CLI Guide](cli-guide.md) for usage instructions

## Support

For questions or issues:
- Check existing issues in the repository
- Create a new issue with:
  - Clear title describing the problem
  - Code snippet showing your extension
  - Expected vs actual behavior
  - Java and Maven versions

