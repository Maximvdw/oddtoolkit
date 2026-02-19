package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 * SHACL generator that builds shapes from the ontology's inferred model.
 * Configurable via `generators.shacl` in application.yml (list of adapter bean names).
 */
public class ShaclGenerator extends BaseGenerator {

  private static final String SH = "http://www.w3.org/ns/shacl#";

  public ShaclGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters) {
    super(ontologyInfo, conceptSchemeInfo, adapters);
  }

  /**
   * Generate SHACL shapes from the ontology's inferred model.
   * Uses inferredModel if present, otherwise falls back to the raw model.
   */
  public Model generate() {
    OntologyInfo info = getOntologyInfo();
    Model ontology = info.getInferredModel() != null ? info.getInferredModel() : info.getModel();
    if (ontology == null) {
      return ModelFactory.createDefaultModel();
    }

    Model shacl = ModelFactory.createDefaultModel();
    shacl.setNsPrefix("sh", SH);
    shacl.setNsPrefix("owl", OWL.NS);
    shacl.setNsPrefix("rdfs", RDFS.getURI());

    // iterate over all classes in the ontology and create node shapes
    Iterator<Resource> classes = ontology.listResourcesWithProperty(RDF.type, OWL.Class);
    while (classes.hasNext()) {
      Resource cls = classes.next();
      if (!cls.isURIResource()) continue;
      generateNodeShape(cls, ontology, shacl);
    }

    return shacl;
  }

  // Helper to create shacl property in a model
  private Property shaclProp(String local, Model m) {
    return m.createProperty(SH + local);
  }

  private boolean isDatatype(Resource res) {
    return res != null && res.isURIResource() && res.getURI().startsWith("http://www.w3.org/2001/XMLSchema#");
  }

  private RDFNode createPath(Resource prop, Model shacl) {
    // if prop has owl:inverseOf -> return a blank node with sh:inversePath inv
    Statement invStmt = prop.getProperty(OWL.inverseOf);
    if (invStmt != null && invStmt.getObject().isResource()) {
      Resource inv = invStmt.getResource();
      Resource b = shacl.createResource();
      b.addProperty(shaclProp("inversePath", shacl), inv);
      return b;
    }
    // otherwise use the property URI
    if (prop.isURIResource()) {
      return shacl.createResource(prop.getURI());
    }
    return shacl.createResource();
  }

  private void addClassOrDatatype(Resource ps, Resource value, Model shacl) {
    if (isDatatype(value)) {
      ps.addProperty(shaclProp("datatype", shacl), value);
    } else {
      ps.addProperty(shaclProp("class", shacl), value);
    }
  }

  private void addMinCountIfNeeded(Resource restriction, Resource ps, Model shacl) {
    if (restriction.hasProperty(OWL.someValuesFrom)
        && !restriction.hasProperty(OWL.minCardinality)
        && !restriction.hasProperty(OWL.cardinality)) {
      ps.addLiteral(shaclProp("minCount", shacl), 1);
    }
  }

  private void addCardinality(Resource restriction, Resource ps, Model shacl) {
    Integer min = intValue(restriction, OWL.minCardinality);
    Integer max = intValue(restriction, OWL.maxCardinality);
    Integer exact = intValue(restriction, OWL.cardinality);

    if (min != null) ps.addLiteral(shaclProp("minCount", shacl), min);
    if (max != null) ps.addLiteral(shaclProp("maxCount", shacl), max);
    if (exact != null) {
      ps.addLiteral(shaclProp("minCount", shacl), exact);
      ps.addLiteral(shaclProp("maxCount", shacl), exact);
    }
  }

  private Integer intValue(Resource res, Property p) {
    Statement s = res.getProperty(p);
    if (s == null) return null;
    RDFNode node = s.getObject();
    if (node.isLiteral()) {
      try {
        return ((Literal) node).getInt();
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  private RDFNode createOrList(Resource unionNode, Model shacl) {
    Resource nodeWithUnion = unionNode;
    Statement unionStmt = unionNode.getProperty(OWL.unionOf);
    if (unionStmt != null && unionStmt.getObject().isResource()) {
      nodeWithUnion = unionStmt.getResource();
    }
    // nodeWithUnion should be an RDFList or a node with rdf:first/rdf:rest
    if (nodeWithUnion.canAs(RDFList.class)) {
      RDFList list = nodeWithUnion.as(RDFList.class);
      List<RDFNode> shapes = new ArrayList<>();
      Iterator<RDFNode> it = list.iterator();
      while (it.hasNext()) {
        RDFNode member = it.next();
        if (member.isResource()) {
          Resource ps = shacl.createResource();
          addClassOrDatatype(ps, member.asResource(), shacl);
          shapes.add(ps);
        }
      }
      return shacl.createList(shapes.iterator());
    }
    return shacl.createResource();
  }

  private void generatePropertyShape(Resource restriction, Model shacl, Resource nodeShape) {
    Resource onProp = restriction.getPropertyResourceValue(OWL.onProperty);
    if (onProp == null) return;

    Resource ps = shacl.createResource();
    ps.addProperty(shaclProp("path", shacl), createPath(onProp, shacl));

    Resource some = restriction.getPropertyResourceValue(OWL.someValuesFrom);
    Resource all = restriction.getPropertyResourceValue(OWL.allValuesFrom);

    if (some != null) {
      if (some.hasProperty(OWL.unionOf)) {
        ps.addProperty(shaclProp("or", shacl), createOrList(some, shacl));
      } else {
        addClassOrDatatype(ps, some, shacl);
      }
    }

    if (all != null) {
      if (all.hasProperty(OWL.unionOf)) {
        ps.addProperty(shaclProp("or", shacl), createOrList(all, shacl));
      } else {
        addClassOrDatatype(ps, all, shacl);
      }
    }

    addMinCountIfNeeded(restriction, ps, shacl);
    addCardinality(restriction, ps, shacl);

    nodeShape.addProperty(shaclProp("property", shacl), ps);
  }

  private void generateNodeShape(Resource cls, Model ontology, Model shacl) {
    Resource ns = shacl.createResource(cls.getURI() + "Shape");
    ns.addProperty(RDF.type, shacl.createResource(SH + "NodeShape"));
    ns.addProperty(shaclProp("targetClass", shacl), cls);

    // find rdfs:subClassOf values that are OWL.Restriction
    Iterator<Statement> it = ontology.listStatements(cls, RDFS.subClassOf, (RDFNode) null);
    while (it.hasNext()) {
      Statement st = it.next();
      RDFNode obj = st.getObject();
      if (obj.isResource()) {
        Resource r = obj.asResource();
        if (r.hasProperty(RDF.type, OWL.Restriction)) {
          generatePropertyShape(r, shacl, ns);
        }
      }
    }
  }
}
