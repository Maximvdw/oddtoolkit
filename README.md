# ODDToolkit

ODDToolkit (Ontology Driven Design Toolkit) is a small toolkit to help generate code and documentation from ontology-driven models. It includes generators and utilities to produce class diagrams, ER diagrams, and Java code from RDF/Turtle sources and ontology metadata.

## Key Features

- Generate class diagrams and ER diagrams from ontology sources
- Produce Java classes and artifacts based on ontology models

## Quickstart

Build the project (from repository root)

```bash
# build the Java project and create the JAR (skip tests for a faster build)
mvn -DskipTests package

# run the produced JAR (example)
java -jar target/oddtoolkit-0.0.1-SNAPSHOT.jar
```

If you prefer to run tests during build, omit `-DskipTests`.

## Project layout

- `src/main/java` - main Java sources
- `src/test/java` - tests
- `docs/` - documentation site

## Contributing

Contributions, issues and feature requests are welcome. Please open issues on the repository and follow the standard forking workflow. Keep changes focused and add tests where appropriate.

## License

This project is licensed under the terms in the `LICENSE` file.

## Contact / Maintainers

Maintained by the project authors. See the repository for contributor details.
