package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.DiagramGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.config.SQLGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.config.SchemaGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.util.List;
import lombok.Getter;

@Getter
public class SQLGenerator extends SchemaGenerator {

  private final SQLGeneratorProperties sqlGeneratorProperties;

  public SQLGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters,
      DiagramGeneratorProperties diagramGeneratorProperties,
      SchemaGeneratorProperties schemaGeneratorProperties,
      SQLGeneratorProperties generatorProperties) {
    super(ontologyInfo, conceptSchemeInfo, adapters, diagramGeneratorProperties,
        schemaGeneratorProperties);
    this.sqlGeneratorProperties = generatorProperties;
  }

  @Override
  public void run() {
    super.run();
    String generated = generate();
    if (getOutputFile() != null) {
      saveToFile(getOutputFile(), generated);
    } else {
      System.out.println(generated);
    }
  }

  private String generate() {
    StringBuilder sb = new StringBuilder();
    sb.append("-- Auto-generated SQL schema from ODDToolkit\n");
    sb.append("-- Ontology: ").append(getOntologyInfo().getUri()).append("\n");
    sb.append("-- Generated: ").append(java.time.ZonedDateTime.now()).append("\n\n");
    generateEnumTypes(sb);
    generateTables(sb);
    return sb.toString();
  }

  private void generateTables(StringBuilder sb) {
    getTables().forEach(table -> {
      sb.append("-- ").append(table.getUri()).append("\n");
      sb.append("CREATE TABLE ").append(table.getName()).append(" (\n");
      int i = 0;
      for (Column column : table.getColumns()) {
        if (i++ > 0) {
          sb.append(",\n");
        }
        sb.append("  ").append(column.getName()).append(" ").append(column.getDataType());
      }
      // Create constraints for primary keys and foreign keys
      List<Column> primaryKeys = table.getColumns().stream().filter(Column::isPrimaryKey).toList();
      if (!primaryKeys.isEmpty()) {
        sb.append(",\n  PRIMARY KEY (");
        int j = 0;
        for (Column pk : primaryKeys) {
          if (j++ > 0) {
            sb.append(", ");
          }
          sb.append(pk.getName());
        }
        sb.append(")");
      }
      List<Column> foreignKeys = table.getColumns().stream().filter(Column::isForeignKey).toList();
      for (Column fk : foreignKeys) {
        table.getRelations()
            .stream().filter(r -> r.getFromColumn().equals(fk)).findFirst().ifPresent(
                relation -> sb.append(",\n  FOREIGN KEY (").append(fk.getName()).append(") REFERENCES ")
                    .append(relation.getTo().getName()).append("(")
                    .append(relation.getToColumn().getName()).append(")"));
      }
      sb.append("\n);\n\n");

      // Create comments ON TABLE and ON COLUMN for documentation
      sb.append("COMMENT ON TABLE ").append(table.getName()).append(" IS '")
          .append(table.getUri()).append("';\n");
      for (Column column : table.getColumns()) {
        if (column.getPropertyInfo() != null) {
          sb.append("COMMENT ON COLUMN ").append(table.getName()).append(".").append(column.getName())
              .append(" IS '").append(column.getPropertyInfo().getUri()).append("';\n");
        }
      }
      sb.append("\n");
      sb.append("----------------------------------------------------------------------\n\n");
    });
  }

  private void generateEnumTypes(StringBuilder sb) {
    getEnums()
        .forEach(type -> {
          if (type.getClassInfo() != null) {
            sb.append("-- ").append(type.getUri()).append("\n");
          }
          sb.append("CREATE TYPE ").append(type.getName()).append(" AS ENUM (\n");
          int i = 0;
          for (EnumValue value : type.getValues()) {
            if (i++ > 0) {
              sb.append(",\n");
            }
            sb.append("  '").append(value).append("'");
          }
          sb.append("\n);\n\n");
        });
  }

  @Override
  protected String getOutputFile() {
    return sqlGeneratorProperties != null ? sqlGeneratorProperties.getOutputFile() : null;
  }
}
