package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ClassGeneratorTest {
  @Qualifier("classGenerator")
  @Autowired
  ClassGenerator generator;

  @Test
  void testGetConcreteClasses() {
    generator.run();
    List<ClassInfo> classes = generator.getConcreteClasses();
    assertThat(classes).isNotNull();
  }

  @Test
  void testGetInterfaces() {
    generator.run();
    List<ClassInfo> interfaces = generator.getInterfaces();
    assertThat(interfaces).isNotNull();
  }

  @Test
  void testGetEnumerations() {
    generator.run();
    Map<ClassInfo, List<ClassInfo>> enumerations = generator.getEnums();
    assertThat(enumerations).isNotNull();
  }
}
