package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.apache.commons.lang3.StringUtils.capitalize;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaGenerator extends ClassGenerator {
  private final Map<Clazz, String> fileNames = new HashMap<>();

  public JavaGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters) {
    super(ontologyInfo, conceptSchemeInfo, adapters);
  }

  @Override
  public void run() {
    super.run();
  }

  protected void generateInterfaces() {
    this.interfaces.forEach(inf -> {
      String fileName = inf.getName() + ".interface.java";
      fileNames.put(inf, fileName);
      StringBuilder builder = new StringBuilder();
      builder.append("@Getter\n").append("@Setter\n");
      builder.append("public interface ").append(inf.getName()).append(" {\n");
      inf.getAttributes().forEach(prop -> {

      });
      builder.append("}\n");
    });
  }
}
