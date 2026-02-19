package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import be.vlaanderen.omgeving.oddtoolkit.model.ClassConceptInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class BaseGeneratorTest {
  @Qualifier("baseGenerator")
  @Autowired
  BaseGenerator generator;

  @Test
  void testGetOntologyClasses() {
    generator.run();
    List<ClassInfo> classes = generator.getOntologyClasses();
    assertThat(classes).isNotNull();
  }

  @Test
  void testGetOntologyClassConcepts() {
    generator.run();
    List<ClassConceptInfo> concepts = generator.getOntologyClassConcepts();
    assertThat(concepts).isNotNull();
  }

  @Test
  void testGetAllClasses() {
    generator.run();
    List<ClassInfo> allClasses = generator.getAllClasses();
    assertThat(allClasses).isNotNull();
  }
}
