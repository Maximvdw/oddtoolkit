package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.DiagramGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration;
import be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration.ExtraProperty;
import be.vlaanderen.omgeving.oddtoolkit.config.SchemaGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class SchemaGenerator extends DiagramGenerator {

  private final List<Table> tables = new ArrayList<>();
  private final OntologyConfiguration ontologyConfiguration;
  private final SchemaGeneratorProperties generatorProperties;

  public SchemaGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo, List<AbstractAdapter<?>> adapters,
      DiagramGeneratorProperties diagramGeneratorProperties,
      SchemaGeneratorProperties generatorProperties) {
    super(ontologyInfo, conceptSchemeInfo, adapters, diagramGeneratorProperties);
    this.ontologyConfiguration = ontologyInfo.getConfig();
    this.generatorProperties = generatorProperties;
  }

  @Override
  public void run() {
    super.run();
    extractTables();
    extractTableRelations();
  }

  private String getIdentifierColumnUri() {
    return this.ontologyConfiguration.getExtraProperties()
        .stream()
        .filter(ExtraProperty::isIdentifier)
        .findFirst()
        .map(ExtraProperty::getUri)
        .orElse("http://www.w3.org/1999/02/22-rdf-syntax-ns#id");
  }

  protected void extractTableRelations() {
    tables.forEach(this::extractColumnRelations);

    // Extract inheritance relations and tables
    tables.forEach(this::extractInheritance);

    // Create join tables for many-to-many relations
    new ArrayList<>(tables).forEach(this::extractManyToManyRelations);
  }

  private void extractStyles(Table table) {
    // Get style for the table
    String style = getStyleForClass(table.getClassInfo());
    table.setDiagramStyle(style);
  }

  private void extractTables() {
    // Extract tables from concrete classes
    this.classes
        .forEach(concreteClass -> {
          tables.add(new Table(concreteClass));
        });
    // Extract tables from interfaces
    this.interfaces
        .forEach(interfaceClass -> {
          tables.add(new Table(interfaceClass));
        });
    // Now extract columns
    tables.forEach(this::extractStyles);
  }

  private String getJoinTableName(Relation relation) {
    return toSnakeCase(relation.getFrom().getName() + "_" + relation.getTo().getName());
  }

  private Table createJoinTable(Relation relation) {
    Table joinTable = new Table();
    joinTable.setDiagramStyle(relation.getFrom().getDiagramStyle());
    joinTable.setClassInfo(relation.getFrom().getClassInfo());
    joinTable.setName(getJoinTableName(relation));
    // Add all primary key columns from both tables to the join table
    List<Column> joinColumns = new ArrayList<>();
    List<Column> leftColumns = relation.getFrom().getColumns().stream()
        .filter(Column::isPrimaryKey)
        .map(Column::copy)
        .peek(c -> c.setName(relation.getFrom().getName() + "_" + c.getName()))
        .toList();
    List<Column> rightColumns = relation.getTo().getColumns().stream()
        .filter(Column::isPrimaryKey)
        .map(Column::copy)
        .peek(c -> c.setName(relation.getTo().getName() + "_" + c.getName()))
        // If the left column has the same name as the right column, rename the right column to avoid conflicts
        .peek(c -> {
          if (leftColumns.stream().anyMatch(lc -> lc.getName().equals(c.getName()))) {
            c.setName(relation.getTo().getName() + "_" + c.getName());
          }
        })
        .toList();
    joinColumns.addAll(leftColumns);
    joinColumns.addAll(rightColumns);
    // Set all columns as FK in the join table
    joinColumns.forEach(c -> c.setForeignKey(true));
    joinTable.setColumns(joinColumns);
    tables.add(joinTable);
    // Create relations from original tables to join table
    createJoinTableRelation(relation.getName(), relation.getFrom(), joinTable,
        relation.getFromColumn());
    createJoinTableRelation(relation.getName(), relation.getTo(), joinTable,
        relation.getToColumn());
    return joinTable;
  }

  private void cleanRelation(Relation relation) {
    Relation inverseRelation = relation.getTo().getRelations().stream()
        .filter(r -> r.getTo().equals(relation.getFrom()) && r.getToColumn().equals(
            relation.getFromColumn()) && r.getFromColumn().equals(relation.getToColumn()))
        .findFirst()
        .orElse(null);
    // Remove columns
    relation.getFrom().getColumns()
        .stream()
        .filter(c -> c.equals(relation.getFromColumn()) || c.equals(relation.getToColumn()))
        .forEach(c -> relation.getFrom().removeColumn(c));
    relation.getTo().getColumns()
        .stream()
        .filter(c -> c.equals(relation.getFromColumn()) || c.equals(relation.getToColumn()))
        .forEach(c -> relation.getTo().removeColumn(c));
    // Remove the original relation
    relation.getFrom().getRelations().remove(relation);
    relation.getTo().getRelations().remove(inverseRelation);
  }

  private void extractManyToManyRelations(Table table) {
    // First determine if there are relations that need to be merged
    if (generatorProperties.getMergeJoinTables().isEnabled()) {
      // Find all relations that are many-to-many to the same target
      List<Relation> relations = new ArrayList<>(table.getRelations());
      relations
          .stream()
          .filter(r -> r.getCardinality() == Cardinality.MANY_TO_MANY)
          .map(Relation::getTo)
          .distinct()
          .forEach(targetTable -> {
            List<Relation> relationsToTarget = table.getRelations().stream()
                .filter(r -> r.getCardinality() == Cardinality.MANY_TO_MANY && r.getTo().equals(
                    targetTable))
                .toList();
            if (relationsToTarget.size() > 1) {
              // Create new enum type
              Enum enumType = new Enum();
              enumType.setName(
                  toSnakeCase(table.getName() + "_" + targetTable.getName() + "_merge_type"));
              enumType.setValues(relationsToTarget.stream().map(r -> {
                String relationName = r.getName() != null ? r.getName() : "relation";
                EnumValue enumValue = new EnumValue();
                enumValue.setName(toSnakeCase(relationName));
                return enumValue;
              }).toList());
              enums.add(enumType);
              Table joinTable = createJoinTable(relationsToTarget.getFirst());
              // Add merge type column
              Column mergeTypeColumn = new Column();
              mergeTypeColumn.setName(generatorProperties.getMergeJoinTables().getAttributeName());
              mergeTypeColumn.setDataType(new DataType(enumType.getName(), enumType.getUri()));
              mergeTypeColumn.setForeignKey(false);
              mergeTypeColumn.setNullable(false);
              mergeTypeColumn.setPrimaryKey(true);
              joinTable.addColumn(mergeTypeColumn);
              // Remove columns of the original relation(s)
              relationsToTarget.forEach(this::cleanRelation);
            }
          });
    }
    // Extract many-to-many relations
    List<Relation> relations = new ArrayList<>(table.getRelations());
    relations.forEach(relation -> {
      if (relation.getCardinality() == Cardinality.MANY_TO_MANY) {
        Relation inverseRelation = relation.getTo().getRelations().stream()
            .filter(r -> r.getTo().equals(relation.getFrom()) && r.getToColumn().equals(
                relation.getFromColumn()) && r.getFromColumn().equals(relation.getToColumn()))
            .findFirst()
            .orElse(null);
        createJoinTable(relation);
        cleanRelation(relation);
      }
    });
  }

  private void createJoinTableRelation(String name, Table targetTable, Table joinTable,
      Column toColumn) {
    Relation toRelation = new Relation();
    toRelation.setFrom(joinTable);
    toRelation.setTo(targetTable);
    toRelation.setFromColumn(toColumn);
    toRelation.setToColumn(targetTable.getColumnByUri(getIdentifierColumnUri()));
    toRelation.setCardinality(Cardinality.MANY_TO_ONE);
    toRelation.setName(name);
    joinTable.getRelations().add(toRelation);
  }

  private void extractInheritance(Table table) {
    table.getClassInfo().getSuperClasses().forEach(superClass -> {
      Table parentTable = getTableByClazz(getClass(superClass));
      if (parentTable == null) {
        return; // Skip if the parent table is not found
      }
      Column targetColumn = parentTable.getColumnByUri(getIdentifierColumnUri());
      // Add column
      Column column = new Column();
      column.setName(toSnakeCase(table.getName()) + "_" + targetColumn.getName());
      column.setForeignKey(true);
      column.setPrimaryKey(true);
      column.setDataType(targetColumn.getDataType());
      table.addColumn(column);

      // Add relation
      Relation relation = new Relation();
      relation.setFrom(table);
      relation.setFromColumn(column);
      relation.setTo(parentTable);
      relation.setToColumn(targetColumn);
      relation.setCardinality(Cardinality.ONE_TO_MANY);
      table.getRelations().add(relation);
    });
  }

  private void extractColumnRelations(Table table) {
    // Extract relations based on properties that reference other classes
    for (Attribute attribute : table.getAttributes()) {
      // Relations may not be to the direct range of the property
      // use nearest class to find the target table
      if (attribute.getRange() != null) {
        // Get a target table
        Clazz nearestClass = attribute.getRange();
        Table targetTable = getTableByClazz(nearestClass);
        if (targetTable == null) {
          continue; // Skip if the target table is not found
        }
        // Update the data type of the column to match the identifier column of the target table
        Column column = table.getColumnByAttribute(attribute);
        Column targetColumn = targetTable.getColumnByUri(getIdentifierColumnUri());
        if (column != null && targetColumn != null) {
          column.setDataType(targetColumn.getDataType());
          column.setName(column.getName() + "_" + targetColumn.getName());
          column.setForeignKey(true);
        }

        // Determine the PK of the related table to use as FK
        String relationName = attribute.getName();
        Relation relation = new Relation();
        relation.setName(toSnakeCase(relationName));
        relation.setFrom(table);
        relation.setTo(targetTable);
        relation.setFromColumn(table.getColumnByAttribute(attribute));
        relation.setToColumn(table.getColumnByUri(getIdentifierColumnUri()));
        relation.setCardinality(attribute.getCardinality());
        table.getRelations().add(relation);
      }
    }
  }

  protected Table getTableByClazz(Clazz clazz) {
    if (clazz == null) {
      return null;
    }
    return tables.stream()
        .filter(t -> t.getClassInfo().equals(clazz.getClassInfo()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Utility to convert XSD data types to SQL data types.
   *
   * @param type The XSD data type URI (e.g., "http://www.w3.org/2001/XMLSchema#string")
   * @return The corresponding SQL data type (e.g., "VARCHAR")
   */
  protected String xsdToSQL(String type) {
    if (type == null) {
      return "VARCHAR"; // Default to VARCHAR if type is unknown
    }
    return switch (type) {
      case "http://www.w3.org/2001/XMLSchema#string" -> "VARCHAR";
      case "http://www.w3.org/2001/XMLSchema#integer" -> "INT";
      case "http://www.w3.org/2001/XMLSchema#decimal" -> "DECIMAL";
      case "http://www.w3.org/2001/XMLSchema#boolean" -> "BOOLEAN";
      case "http://www.w3.org/2001/XMLSchema#date" -> "DATE";
      case "http://www.w3.org/2001/XMLSchema#dateTime" -> "DATETIME";
      default -> "VARCHAR"; // Fallback for unrecognized types
    };
  }

  @Getter
  @Setter
  protected static class Table extends Clazz {

    private List<Relation> relations = new ArrayList<>();
    private String diagramStyle;

    public Table() {
    }

    public Table(Clazz clazz) {
      setClassInfo(clazz.getClassInfo());
      setName(toSnakeCase(clazz.getName()));
      List<Column> columns = clazz.getAttributes()
          .stream()
          .map(Column::new)
          .toList();
      addAllColumns(columns);
    }

    public List<Column> getColumns() {
      return super.getAttributes().stream()
          .filter(a -> a instanceof Column)
          .map(a -> (Column) a)
          .toList();
    }

    public void addColumn(Column column) {
      getAttributes().add(column);
    }

    public void addAllColumns(List<Column> columns) {
      getAttributes().addAll(columns);
    }

    public void removeColumn(Column column) {
      getAttributes().remove(column);
    }

    public void setColumns(List<Column> columns) {
      // Clear existing columns
      getAttributes().removeIf(a -> a instanceof Column);
      // Add new columns
      getAttributes().addAll(columns);
    }

    public Column getColumnByAttribute(Attribute attribute) {
      return getColumns().stream()
          .filter(c -> c.getPropertyInfo() != null && c.getPropertyInfo()
              .equals(attribute.getPropertyInfo()))
          .findFirst()
          .orElse(null);
    }

    public Column getColumnByUri(String propertyUri) {
      return getColumns().stream()
          .filter(
              c -> c.getPropertyInfo() != null && c.getPropertyInfo().getUri().equals(propertyUri))
          .findFirst()
          .orElse(null);
    }

    public String toString() {
      return "Table{name='" + getName() + "', columns=" + getColumns() + ", relations=" + relations
          + "}";
    }
  }

  @Getter
  @Setter
  protected static class Column extends Attribute {

    private boolean foreignKey;

    public Column() {
    }

    public Column(Attribute attribute) {
      setPropertyInfo(attribute.getPropertyInfo());
      setDataType(attribute.getDataType());
      setName(attribute.getName());
      setRange(attribute.getRange());
      setPrimaryKey(attribute.isPrimaryKey());
      setNullable(attribute.isNullable());
      setCardinality(attribute.getCardinality());
    }

    public String toString() {
      return "Column{name='" + getName() + "', dataType='" + getDataType() + "', primaryKey="
          + isPrimaryKey()
          + ", nullable=" + isNullable() + "}";
    }

    public Column copy() {
      Column column = new Column(this);
      column.foreignKey = foreignKey;
      return column;
    }
  }

  @Getter
  @Setter
  protected static class Relation {

    private String name;
    private Table from;
    private Table to;
    private Column fromColumn;
    private Column toColumn;
    private Cardinality cardinality;

    public String toString() {
      return "Relation{name='" + name + "', from=" + from.getName() + ", to=" + to.getName()
          + ", cardinality=" + cardinality + "}";
    }
  }
}
