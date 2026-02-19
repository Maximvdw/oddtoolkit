package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ClassDiagramGeneratorTest {
  @Qualifier("classDiagramGenerator")
  @Autowired
  ClassDiagramGenerator generator;

  @Test
  void testGetConcreteClasses() throws Exception {
    generator.run();
    String output = generator.getOutputFile();
    if (output != null) {
      Path p = Path.of(output);
      assertThat(Files.exists(p)).isTrue();
      String content = Files.readString(p);
      assertThat(content).contains("class");
    }
  }
}
