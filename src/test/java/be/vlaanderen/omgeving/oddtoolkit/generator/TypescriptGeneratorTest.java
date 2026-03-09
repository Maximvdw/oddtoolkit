package be.vlaanderen.omgeving.oddtoolkit.generator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test for TypescriptGenerator.
 * Generates TypeScript classes, interfaces and enums from the ontology.
 */
@SpringBootTest
public class TypescriptGeneratorTest {
  @Qualifier("typescriptGenerator")
  @Autowired
  TypescriptGenerator generator;

  @Test
  void testGenerator() {
    generator.run();
  }
}

