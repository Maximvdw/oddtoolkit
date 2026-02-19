package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.ERDiagramProperties;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import java.util.List;

public class ERDiagramGenerator extends SchemaGenerator {

  private final ERDiagramProperties generatorProperties;

  public ERDiagramGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters,
      ERDiagramProperties generatorProperties) {
    super(ontologyInfo, conceptSchemeInfo, adapters);
    this.generatorProperties = generatorProperties;
  }

  @Override
  public void run() {
    super.run();
    String diagram = generate("erDiagram");
    if (getOutputFile() != null) {
      saveToFile(getOutputFile(), diagram);
    } else {
      System.out.println(diagram);
    }
  }

  @Override
  protected String getOutputFile() {
    return generatorProperties != null ? generatorProperties.getOutputFile() : null;
  }

  @Override
  protected void renderContent(StringBuilder builder, String type) {
    generateTables(builder);
  }

  private void generateTables(StringBuilder builder) {
    getTables().forEach(table -> {
      builder.append("%% ").append(table.getClassInfo().getUri()).append("\n");
      builder.append(table.getName()).append(" {\n");
      table.getColumns().forEach(column -> {
        builder.append("  ").append(column.getName()).append(" ").append(column.getDataType());
        builder.append(" ");
        boolean hasFlag = false;
        if (column.isPrimaryKey()) {
          builder.append("PK");
          hasFlag = true;
        }
        if (column.isForeignKey()) {
          if (hasFlag) {
            builder.append(",");
          }
          builder.append("FK");
        }
        builder.append("\n");
      });
      builder.append("}\n\n");
      generateRelations(builder, table);
    });
  }

  private void generateRelations(StringBuilder builder, Table table) {
    table.getRelations().forEach(relation -> {
      // Add a comment
      PropertyInfo relationProperty = relation.getFromColumn().getPropertyInfo();
      Cardinality cardinality = relation.getCardinality();
      if (relationProperty != null) {
        builder.append("%% ").append(relation.getFromColumn().getPropertyInfo().getUri())
            .append("\n");
      }
      // Get the enum value (MANY_TO_ONE, ONE_TO_MANY, etc.) as a string (key of the enum)
      String cardinalityString = cardinality.name();
      String cardinalityFromString = cardinalityString.split("_TO_")[0];
      String cardinalityToString = cardinalityString.split("_TO_")[1];
      builder.append(table.getName()).append(" ").append(cardinalityFromString).append(" to ")
          .append(cardinalityToString).append(" ").append(relation.getTo().getName());
      builder.append(" : ").append(relation.getName() != null ? relation.getName() : "\"\"")
          .append("\n\n");
    });
  }
}
