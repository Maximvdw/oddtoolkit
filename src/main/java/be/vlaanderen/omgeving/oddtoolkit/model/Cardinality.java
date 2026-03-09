package be.vlaanderen.omgeving.oddtoolkit.model;

/**
 * Enum representing the cardinality of relationships between entities.
 */
public enum Cardinality {
  /**
   * One-to-one relationship.
   */
  ONE_TO_ONE,

  /**
   * One-to-many relationship.
   */
  ONE_TO_MANY,

  /**
   * Many-to-one relationship.
   */
  MANY_TO_ONE,

  /**
   * Many-to-many relationship.
   */
  MANY_TO_MANY;

  /**
   * Checks if this cardinality represents a to-many relationship.
   *
   * @return true if the cardinality is ONE_TO_MANY or MANY_TO_MANY, false otherwise
   */
  public boolean isToMany() {
    return this == ONE_TO_MANY || this == MANY_TO_MANY;
  }
}

