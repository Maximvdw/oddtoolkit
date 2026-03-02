package be.vlaanderen.omgeving.oddtoolkit.generator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class JavaGeneratorTest {
  @Qualifier("javaGenerator")
  @Autowired
  JavaGenerator generator;

  @Test
  void testGenerator() {
    generator.run();
  }
}
