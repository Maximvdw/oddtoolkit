package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration;
import be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration.ExtraProperty;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.atlas.lib.Pair;

@Getter
public abstract class SchemaGenerator extends DiagramGenerator {

  private final List<Table> tables = new ArrayList<>();
  private final OntologyConfiguration ontologyConfiguration;

  public SchemaGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo, List<AbstractAdapter<?>> adapters) {
    super(ontologyInfo, conceptSchemeInfo, adapters);
    this.ontologyConfiguration = ontologyInfo.getConfig();
  }

  @Override
  public void run() {
    super.run();
    extractTables();
  }

  private String getIdentifierColumnUri() {
    return this.ontologyConfiguration.getExtraProperties()
        .stream()
        .filter(ExtraProperty::isIdentifier)
        .findFirst()
        .map(ExtraProperty::getUri)
        .orElse("http://www.w3.org/1999/02/22-rdf-syntax-ns#id");
  }

  private void extractTables() {
    // Extract tables from concrete classes
    this.concreteClasses
        .forEach(concreteClass -> {
          Pair<String, String> classNameAndLabel = getClassNameAndLabel(concreteClass);
          Table table = new Table();
          table.setClassInfo(concreteClass);
          table.setName(toSnakeCase(classNameAndLabel.getLeft()));
          tables.add(table);
        });
    // Extract tables from interfaces
    this.interfaces
        .forEach(interfaceClass -> {
          Pair<String, String> classNameAndLabel = getClassNameAndLabel(interfaceClass);
          Table table = new Table();
          table.setClassInfo(interfaceClass);
          table.setName(toSnakeCase(classNameAndLabel.getLeft()));
          tables.add(table);
        });
    // Now extract columns
    tables.forEach(this::extractColumns);
    tables.forEach(this::extractColumnRelations);

    // Extract inheritance relations and tables
    tables.forEach(this::extractInheritance);

    // Create join tables for many-to-many relations
    new ArrayList<>(tables).forEach(this::extractManyToManyRelations);
  }

  private void extractManyToManyRelations(Table table) {
    // Extract many to many relations
    List<Relation> newRelations = new ArrayList<>();
    table.getRelations().forEach(relation -> {
      if (relation.getCardinality() == Cardinality.MANY_TO_MANY) {
        // Create join table
        Table joinTable = new Table();
        joinTable.setClassInfo(table.getClassInfo());
        joinTable.setName(
            toSnakeCase(relation.getFrom().getName() + "_" + relation.getTo().getName()));
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
            .toList();
        joinColumns.addAll(leftColumns);
        joinColumns.addAll(rightColumns);
        // Set all columns as FK in the join table
        joinColumns.forEach(c -> c.setForeignKey(true));
        joinTable.setColumns(joinColumns);
        tables.add(joinTable);
        // Create relations from original tables to join table
        createJoinTableRelation(relation.getFrom(), joinTable, relation.getFromColumn());
        createJoinTableRelation(relation.getTo(), joinTable, relation.getToColumn());
        // Remove columns
        table.getColumns()
            .removeIf(c -> c.equals(relation.getFromColumn()) || c.equals(relation.getToColumn()));
      } else {
        newRelations.add(relation);
      }
    });
    table.setRelations(newRelations);
  }

  private void createJoinTableRelation(Table targetTable, Table joinTable, Column toColumn) {
    Relation toRelation = new Relation();
    toRelation.setFrom(joinTable);
    toRelation.setTo(targetTable);
    toRelation.setFromColumn(toColumn);
    toRelation.setToColumn(targetTable.getColumnByUri(getIdentifierColumnUri()));
    toRelation.setCardinality(Cardinality.MANY_TO_ONE);
    joinTable.getRelations().add(toRelation);
  }

  private void extractInheritance(Table table) {
    table.getClassInfo().getSuperClasses().forEach(superClass -> {
      Pair<String, String> classNameAndLabel = getClassNameAndLabel(superClass);
      Table parentTable = getTableByClassInfo(superClass);
      if (parentTable == null) {
        return; // Skip if the parent table is not found
      }
      Column targetColumn = parentTable.getColumnByUri(getIdentifierColumnUri());
      // Add column
      Column column = new Column();
      column.setName(toSnakeCase(classNameAndLabel.getLeft()) + "_" + targetColumn.name);
      column.setForeignKey(true);
      column.setDataType(targetColumn.getDataType());
      table.getColumns().add(column);

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
    for (PropertyInfo property : table.getClassInfo().getProperties()) {
      // Relations may not be to the direct range of the property
      // use nearest class to find the target table
      if (property.getRange() != null && !property.getRange().isEmpty()
          && getNearestClass(property.getRange().getFirst()) != null) {
        // Get target table
        ClassInfo nearestClass = getNearestClass(property.getRange().getFirst());
        Table targetTable = getTableByClassInfo(nearestClass);
        if (targetTable == null) {
          continue; // Skip if the target table is not found
        }
        // Update the data type of the column to match the identifier column of the target table
        Column column = table.getColumnByPropertyInfo(property);
        Column targetColumn = targetTable.getColumnByUri(getIdentifierColumnUri());
        if (column != null && targetColumn != null) {
          column.setDataType(targetColumn.getDataType());
          column.setName(column.getName() + "_" + targetColumn.getName());
          column.setForeignKey(true);
        }

        // Determine the PK of the related table to use as FK
        Pair<String, String> propertyNameAndLabel = getPropertyNameAndLabel(property);
        String relationName = propertyNameAndLabel.getLeft();
        Relation relation = new Relation();
        relation.setName(toSnakeCase(relationName));
        relation.setFrom(table);
        relation.setTo(targetTable);
        relation.setFromColumn(table.getColumnByPropertyInfo(property));
        relation.setToColumn(table.getColumnByUri(getIdentifierColumnUri()));
        relation.setCardinality(getCardinality(property));
        table.getRelations().add(relation);
      }
    }
  }

  private Cardinality getCardinality(PropertyInfo property) {
    Cardinality cardinality;
    switch (property.getCardinalityFrom().getMax() == null
        || property.getCardinalityFrom().getMax() > 1) {
      case false -> {
        switch (property.getCardinalityTo().getMax() == null
            || property.getCardinalityTo().getMax() > 1) {
          case false -> cardinality = Cardinality.ONE_TO_ONE;
          case true -> cardinality = Cardinality.ONE_TO_MANY;
        }
      }
      case true -> {
        switch (property.getCardinalityTo().getMax() == null
            || property.getCardinalityTo().getMax() > 1) {
          case false -> cardinality = Cardinality.MANY_TO_ONE;
          case true -> cardinality = Cardinality.MANY_TO_MANY;
        }
      }
    }
    return cardinality;
  }

  private void extractColumns(Table table) {
    // Extract columns from properties of the class
    List<Column> columns = new ArrayList<>();
    for (PropertyInfo property : table.getClassInfo().getProperties()) {
      Pair<String, String> propertyNameAndLabel = getPropertyNameAndLabel(property);
      Column column = new Column();
      column.setPropertyInfo(property);
      column.setPrimaryKey(property.isIdentifier());
      column.setName(toSnakeCase(propertyNameAndLabel.getLeft()));
      column.setNullable(
          property.getCardinalityTo().getMin() == null
              || property.getCardinalityTo().getMin() == 0);
      // Check if the property is a relation to another class
      if (property.getRange() != null && property.getRange().stream()
          .noneMatch(uri -> getOntologyClasses().stream().anyMatch(c -> c.getUri().equals(uri)))) {
        // Determine data type based on XSD type or default to VARCHAR
        String dataType = property.getRange() != null && !property.getRange().isEmpty() ? xsdToSQL(
            property.getRange().getFirst()) : "VARCHAR";
        column.setDataType(dataType);
      }
      columns.add(column);
    }
    table.setColumns(columns);
  }

  @Override
  protected List<DiagramStyle> getStyleEntries() {
    // No default styles for schema generator; subclasses may override
    return Collections.emptyList();
  }

  protected Table getTableByClassInfo(ClassInfo classInfo) {
    return tables.stream()
        .filter(t -> t.getClassInfo().equals(classInfo))
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

  /**
   * Utility to convert a string to snake_case, suitable for identifiers in ER diagrams.
   *
   * @param input The input string to convert
   * @return The snake_case version of the input string
   */
  protected String toSnakeCase(String input) {
    if (input == null) {
      return null;
    }
    String s = input.trim();
    // Replace camelCase boundaries with underscore
    s = s.replaceAll("([a-z0-9])([A-Z])", "$1_$2");
    // Replace non-alphanumeric chars with underscore
    s = s.replaceAll("[^A-Za-z0-9]+", "_");
    s = s.replaceAll("_+", "_");
    s = s.replaceAll("^_+|_+$", "");
    return s.toLowerCase();
  }

  @Getter
  @Setter
  protected static class Table {

    private ClassInfo classInfo;
    private String name;
    private List<Column> columns = new ArrayList<>();
    private List<Relation> relations = new ArrayList<>();

    public Column getColumnByPropertyInfo(PropertyInfo propertyInfo) {
      return columns.stream()
          .filter(c -> c.getPropertyInfo().equals(propertyInfo))
          .findFirst()
          .orElse(null);
    }

    public Column getColumnByUri(String propertyUri) {
      return columns.stream()
          .filter(c -> c.getPropertyInfo() != null && c.getPropertyInfo().getUri().equals(propertyUri))
          .findFirst()
          .orElse(null);
    }

    public String toString() {
      return "Table{name='" + name + "', columns=" + columns + ", relations=" + relations + "}";
    }
  }

  @Getter
  @Setter
  protected static class Column {

    private PropertyInfo propertyInfo;
    private String dataType;
    private String name;
    private boolean primaryKey;
    private boolean foreignKey;
    private boolean nullable;

    public String toString() {
      return "Column{name='" + name + "', dataType='" + dataType + "', primaryKey=" + primaryKey
          + ", nullable=" + nullable + "}";
    }

    public Column copy() {
      Column column = new Column();
      column.propertyInfo = propertyInfo;
      column.dataType = dataType;
      column.name = name;
      column.primaryKey = primaryKey;
      column.foreignKey = foreignKey;
      column.nullable = nullable;
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
