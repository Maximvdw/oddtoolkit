# Ontology Metadata & OWL Support

ODDToolkit works with RDF/OWL ontologies and leverages semantic metadata to drive code generation. This guide explains the ontology features, supported metadata vocabularies, and how to structure your ontology for optimal generation.

## Overview

ODDToolkit reads from Turtle (TTL) RDF files and extracts class, property, and constraint information to generate:
- Class diagrams (Mermaid)
- Database schemas (SQL)
- Java POJOs and records
- TypeScript interfaces
- SHACL validation shapes

The toolkit respects OWL semantics and integrates with standard semantic web vocabularies for richer metadata.

## Supported Vocabularies

### Core OWL (Web Ontology Language)

ODDToolkit supports OWL 2 constructs for class definition:

- **Classes** (`owl:Class`) — Define entities and types
- **Properties** (`owl:ObjectProperty`, `owl:DatatypeProperty`) — Relationships and attributes
- **Restrictions** (`owl:Restriction`) — Cardinality, value constraints, etc.
- **Class axioms** — Subclass relationships, equivalence, disjointness
- **Data types** — Standard XSD types and custom ranges

Example OWL class definition:

```turtle
@prefix ex: <http://example.org/model/> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .

ex:Person a owl:Class ;
  rdfs:label "Person"@en ;
  rdfs:comment "A human being." ;
  rdfs:subClassOf ex:Agent .

ex:name a owl:DatatypeProperty ;
  rdfs:domain ex:Person ;
  rdfs:range xsd:string ;
  rdfs:label "Name"@en .

ex:age a owl:DatatypeProperty ;
  rdfs:domain ex:Person ;
  rdfs:range xsd:integer ;
  rdfs:label "Age"@en .
```

### Hydra Vocabulary

**Hydra** (`https://www.w3.org/ns/hydra/core#`) provides API documentation and RDF-LD semantics. ODDToolkit extracts useful metadata from Hydra annotations:

| Hydra Property | Use | Example |
|---|---|---|
| `hydra:title` | Human-readable class/property name | `hydra:title "User Profile"` |
| `hydra:description` | Documentation string | `hydra:description "Represents a user..."` |
| `hydra:required` | Property is mandatory | `hydra:required true` |
| `hydra:readOnly` | Property is read-only | `hydra:readOnly false` |
| `hydra:writeOnly` | Property is write-only | `hydra:writeOnly false` |
| `hydra:memberAssertion` | Linked data collection semantics | (Advanced) |

Example Hydra-enriched ontology:

```turtle
@prefix hydra: <https://www.w3.org/ns/hydra/core#> .
@prefix ex: <http://example.org/model/> .

ex:User a owl:Class ;
  hydra:title "User" ;
  hydra:description "A registered user account." .

ex:email a owl:DatatypeProperty ;
  rdfs:domain ex:User ;
  rdfs:range xsd:string ;
  hydra:title "Email Address" ;
  hydra:description "Primary contact email." ;
  hydra:required true .

ex:website a owl:DatatypeProperty ;
  rdfs:domain ex:User ;
  rdfs:range xsd:anyURI ;
  hydra:title "Website" ;
  hydra:required false ;
  hydra:readOnly false .
```

### Dublin Core (dc/dcterms)

Standard metadata for documenting resources:

- `dc:title` — Title (maps to class/property names)
- `dc:description` — Description/documentation
- `dc:creator` — Author/creator
- `dc:issued` — Publication/creation date
- `dcterms:created` — Creation timestamp (useful for temporal tracking)
- `dcterms:modified` — Last modification timestamp

Example:

```turtle
@prefix dc: <http://purl.org/dc/elements/1.1/> .
@prefix dcterms: <http://purl.org/dc/terms/> .

ex:Article a owl:Class ;
  dc:title "Article" ;
  dc:description "A published article or blog post." ;
  dc:creator "Content Team" .

ex:publishedDate a owl:DatatypeProperty ;
  rdfs:domain ex:Article ;
  rdfs:range xsd:dateTime ;
  dc:description "Publication timestamp." ;
  dcterms:created "2025-06-01T00:00:00Z" .
```

### SKOS (Simple Knowledge Organization System)

SKOS provides vocabulary management and concept definitions:

- `skos:prefLabel` — Preferred label for a concept
- `skos:altLabel` — Alternative label
- `skos:definition` — Formal definition
- `skos:example` — Example usage
- `skos:broader` / `skos:narrower` — Hierarchical relationships

Example SKOS concept:

```turtle
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .

ex:Status a skos:ConceptScheme ;
  skos:prefLabel "Status Codes"@en ;
  skos:definition "Valid states for order processing."@en .

ex:StatusActive a skos:Concept ;
  skos:inScheme ex:Status ;
  skos:prefLabel "Active"@en ;
  skos:definition "The entity is currently active."@en ;
  skos:example "An active user account."@en .
```

### SHACL (Shapes Constraint Language)

SHACL defines validation constraints on RDF data. ODDToolkit can generate SHACL shapes from ontology definitions:

- `sh:class` — RDF type constraint
- `sh:datatype` — Data type constraint
- `sh:minCount` / `sh:maxCount` — Cardinality
- `sh:minLength` / `sh:maxLength` — String length
- `sh:minInclusive` / `sh:maxInclusive` — Numeric range
- `sh:in` — Enumeration constraint
- `sh:pattern` — Regular expression pattern

Example SHACL shape:

```turtle
@prefix sh: <http://www.w3.org/ns/shacl#> .

ex:UserShape a sh:NodeShape ;
  sh:targetClass ex:User ;
  sh:property [
    sh:path ex:email ;
    sh:datatype xsd:string ;
    sh:minCount 1 ;
    sh:maxCount 1 ;
    sh:pattern "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
  ] ;
  sh:property [
    sh:path ex:age ;
    sh:datatype xsd:integer ;
    sh:minInclusive 0 ;
    sh:maxInclusive 150
  ] .
```

## Special Metadata Properties

### Temporal Properties

Some properties track temporal information (created, modified, deleted dates):

```yaml
ontology:
  temporal-properties:
    - "http://purl.org/dc/terms/created"
    - "http://purl.org/dc/terms/modified"
    - "http://example.org/deletedAt"
```

These properties are often excluded from generated schemas (treated as metadata, not domain properties).

### Enum Classes

Mark certain classes as enumerations/code lists for special handling:

```yaml
ontology:
  enum-classes:
    - "http://www.w3.org/ns/adms#Status"
    - "http://example.org/OrderStatus"
```

Enum classes generate as Java enums or TypeScript literal unions instead of regular classes.

### Extra Properties

Add project-specific metadata not in standard vocabularies:

```yaml
ontology:
  extra-properties:
    - "http://example.org/internalId"
    - "http://example.org/department"
    - "http://example.org/costCenter"
```

These properties are included in generation even if not linked through standard RDF relationships.

### Override Properties

Override or supplement ontology definitions for generation purposes:

```yaml
ontology:
  override-properties:
    ex:Person:
      label: "Human Individual"
      description: "A registered human being in the system."
      customAttribute: "customValue"
```

## Ontology Structure Best Practices

### 1. Use Consistent Naming

- Use URIs for all identifiers (classes, properties)
- Use readable local names (`ex:Person`, not `ex:P001`)
- Use PascalCase for classes, camelCase for properties

```turtle
@prefix ex: <http://example.org/model/> .

ex:Customer a owl:Class ;          # PascalCase for class
  rdfs:label "Customer" .

ex:firstName a owl:DatatypeProperty ;  # camelCase for property
  rdfs:label "First Name" .
```

### 2. Use Comprehensive Labels and Comments

```turtle
ex:Order a owl:Class ;
  rdfs:label "Order"@en ;
  rdfs:label "Commande"@fr ;
  rdfs:comment "A purchase order placed by a customer."@en ;
  hydra:description "Complete order details including items and payment." .

ex:totalAmount a owl:DatatypeProperty ;
  rdfs:label "Total Amount"@en ;
  rdfs:comment "Sum of all items after discounts and tax."@en ;
  rdfs:range xsd:decimal .
```

### 3. Define Domains and Ranges

```turtle
ex:email a owl:DatatypeProperty ;
  rdfs:domain ex:User ;      # Applies to User
  rdfs:range xsd:string .    # Must be string

ex:authored a owl:ObjectProperty ;
  rdfs:domain ex:Person ;     # Applies to Person
  rdfs:range ex:Document .    # References Document
```

### 4. Use Appropriate Data Types

```turtle
ex:birthDate a owl:DatatypeProperty ;
  rdfs:range xsd:date .      # Date only, no time

ex:createdAt a owl:DatatypeProperty ;
  rdfs:range xsd:dateTime .  # Full timestamp

ex:salary a owl:DatatypeProperty ;
  rdfs:range xsd:decimal .   # Precise decimal for money

ex:count a owl:DatatypeProperty ;
  rdfs:range xsd:integer .   # Whole number
```

### 5. Document Cardinality with Restrictions

```turtle
ex:Person a owl:Class ;
  rdfs:subClassOf [
    a owl:Restriction ;
    owl:onProperty ex:primaryEmail ;
    owl:cardinality 1          # Exactly one
  ] ;
  rdfs:subClassOf [
    a owl:Restriction ;
    owl:onProperty ex:nicknames ;
    owl:minCardinality 0 ;     # Zero or more
    owl:maxCardinality 5       # At most 5
  ] .
```

### 6. Organize with Hierarchies

```turtle
ex:Agent a owl:Class ;
  rdfs:label "Agent"@en .

ex:Person a owl:Class ;
  rdfs:subClassOf ex:Agent ;  # Person is an Agent
  rdfs:label "Person"@en .

ex:Organization a owl:Class ;
  rdfs:subClassOf ex:Agent ;  # Organization is an Agent
  rdfs:label "Organization"@en .
```

## Complete Example: E-Commerce Ontology

```turtle
@prefix ex: <http://example.org/ecommerce/> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix dc: <http://purl.org/dc/elements/1.1/> .
@prefix dcterms: <http://purl.org/dc/terms/> .
@prefix hydra: <https://www.w3.org/ns/hydra/core#> .

# Classes

ex:Customer a owl:Class ;
  rdfs:label "Customer"@en ;
  dc:description "A registered customer account." ;
  hydra:title "Customer Account" .

ex:Order a owl:Class ;
  rdfs:label "Order"@en ;
  dc:description "A purchase order with items and payment." ;
  hydra:title "Purchase Order" .

ex:Product a owl:Class ;
  rdfs:label "Product"@en ;
  dc:description "A product available for purchase." ;
  hydra:title "Product Listing" .

ex:OrderStatus a owl:Class ;
  rdfs:label "Order Status"@en ;
  dc:description "Status enumeration for orders." .

# Data Properties

ex:customerId a owl:DatatypeProperty ;
  rdfs:domain ex:Customer ;
  rdfs:range xsd:string ;
  rdfs:label "Customer ID"@en ;
  hydra:required true ;
  hydra:readOnly true .

ex:customerName a owl:DatatypeProperty ;
  rdfs:domain ex:Customer ;
  rdfs:range xsd:string ;
  rdfs:label "Full Name"@en ;
  hydra:required true .

ex:email a owl:DatatypeProperty ;
  rdfs:domain ex:Customer ;
  rdfs:range xsd:string ;
  rdfs:label "Email Address"@en ;
  hydra:required true ;
  hydra:readOnly false .

ex:orderDate a owl:DatatypeProperty ;
  rdfs:domain ex:Order ;
  rdfs:range xsd:dateTime ;
  rdfs:label "Order Date"@en ;
  dcterms:created "2025-01-01T00:00:00Z" ;
  hydra:required true ;
  hydra:readOnly true .

ex:totalAmount a owl:DatatypeProperty ;
  rdfs:domain ex:Order ;
  rdfs:range xsd:decimal ;
  rdfs:label "Total Amount"@en ;
  dc:description "Sum of all items, discounts, and tax."@en ;
  hydra:required true ;
  hydra:readOnly false .

ex:productName a owl:DatatypeProperty ;
  rdfs:domain ex:Product ;
  rdfs:range xsd:string ;
  rdfs:label "Product Name"@en ;
  hydra:required true .

ex:productPrice a owl:DatatypeProperty ;
  rdfs:domain ex:Product ;
  rdfs:range xsd:decimal ;
  rdfs:label "Price"@en ;
  dc:description "Retail price in USD."@en ;
  hydra:required true .

# Object Properties

ex:placedBy a owl:ObjectProperty ;
  rdfs:domain ex:Order ;
  rdfs:range ex:Customer ;
  rdfs:label "Placed By"@en ;
  dc:description "The customer who placed this order." ;
  hydra:required true ;
  hydra:readOnly true .

ex:contains a owl:ObjectProperty ;
  rdfs:domain ex:Order ;
  rdfs:range ex:Product ;
  rdfs:label "Contains"@en ;
  dc:description "Products included in this order."@en ;
  hydra:required true .

ex:hasStatus a owl:ObjectProperty ;
  rdfs:domain ex:Order ;
  rdfs:range ex:OrderStatus ;
  rdfs:label "Status"@en ;
  hydra:required true .
```

## Configuration for Ontology-Driven Generation

```yaml
ontology:
  ontology-file-path: "src/main/resources/ontology.ttl"
  concepts-file-path: "src/main/resources/concepts.ttl"
  
  # Mark these as enumerations in generated code
  enum-classes:
    - "http://example.org/ecommerce/OrderStatus"
    - "http://www.w3.org/ns/adms#Status"
  
  # These properties track temporal info, exclude from domain model
  temporal-properties:
    - "http://purl.org/dc/terms/created"
    - "http://purl.org/dc/terms/modified"
  
  # Include these properties even if not in main ontology
  extra-properties:
    - "http://example.org/internalId"
  
  # Override or supplement ontology definitions
  override-properties:
    "http://example.org/ecommerce/Customer":
      description: "Registered customer with full account details."

generators:
  class-diagram:
    output-file: "target/diagrams/ecommerce-classes.mmd"
  
  sql-generator:
    output-file: "target/generated/ecommerce-schema.sql"
  
  java-generator:
    output-directory: "target/generated-sources/java"
    package-name: "com.example.ecommerce.model"
  
  typescript-generator:
    output-directory: "target/generated-sources/typescript"
```

## Validation & SHACL

ODDToolkit can generate SHACL shapes for validation:

```bash
java -jar oddtoolkit.jar \
  --generator=shacl \
  --config-file=config.yml \
  --output-file=target/ecommerce-shapes.ttl
```

The generated SHACL can validate RDF data:

```bash
# Validate RDF data against generated shapes
robot validate --input data.ttl --shapes target/ecommerce-shapes.ttl
```

## Tips & Troubleshooting

### Tip: Use Consistent Prefixes

Define prefixes at the top of your ontology and reuse them consistently:

```turtle
@prefix ex: <http://example.org/ecommerce/> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix dc: <http://purl.org/dc/elements/1.1/> .
@prefix hydra: <https://www.w3.org/ns/hydra/core#> .
```

### Tip: Validate Your Ontology

Use Apache Jena or similar tools to validate Turtle syntax:

```bash
# With Apache Jena (rdf tools)
riot --validate ontology.ttl

# Or with ROBOT (OBO tools)
robot validate --input ontology.ttl
```

### Issue: Classes Not Appearing in Generated Code

**Possible causes:**
- Class URI is not declared with `a owl:Class`
- Class is not reachable (missing `rdf:type` or `owl:subClassOf` declarations)
- Class is marked as abstract/internal

**Solution:**
- Ensure explicit `a owl:Class` declarations
- Check cardinality and domain/range restrictions
- Review configuration for filtering

### Issue: Properties Missing from Generated Schemas

**Possible causes:**
- Property domain/range not aligned with used classes
- Property marked as temporary or metadata-only
- Property in `temporal-properties` list

**Solution:**
- Define explicit `rdfs:domain` and `rdfs:range`
- Review configuration overrides and filters
- Remove from exclusion lists if needed

## Further Reading

- **OWL 2 Specification:** https://www.w3.org/TR/owl2-overview/
- **Hydra Vocabulary:** https://www.hydra-cg.com/
- **SHACL Specification:** https://www.w3.org/TR/shacl/
- **Dublin Core Metadata:** https://dublincore.org/
- **SKOS:** https://www.w3.org/TR/skos-reference/

