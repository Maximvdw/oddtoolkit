package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ERDiagramGeneratorTest {

  @Qualifier("erDiagramGenerator")
  @Autowired
  ERDiagramGenerator generator;

  @Test
  void testRunGeneratesDiagram() throws Exception {
    // run generator
    generator.run();
    // no exception means it executed; if outputFile configured in test application.yml it should be written
    String outputFile = generator.getOutputFile();
    if (outputFile != null) {
      Path p = Path.of(outputFile);
      assertThat(Files.exists(p)).isTrue();
      String content = Files.readString(p);
      assertThat(content).contains("erDiagram");
      // ensure at least one entity definition (curly braces) exists
      assertThat(content).contains("{");
    }
  }
}

