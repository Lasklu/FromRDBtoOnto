package com.exemple.test;

import java.util.HashSet;
import java.util.Set;

import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.io.StreamDocumentTarget;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

public class TestOwlApi {
	
	public static void main(String args[]) throws OWLOntologyCreationException, OWLOntologyStorageException {
		Set<OWLAxiom> axiomsE = new HashSet<OWLAxiom>();
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		IRI ontologyIRI = IRI.create("http://java-ws.com/ontologie/tstowlapi.owl");
		OWLDataFactory factory=manager.getOWLDataFactory();
		OWLOntology ontology = manager.createOntology(ontologyIRI);
		
		
		OWLClass person = factory.getOWLClass(IRI.create(ontologyIRI+"#Person"));
		OWLClass woman = factory.getOWLClass(IRI.create(ontologyIRI+"#Woman"));
		OWLClass man = factory.getOWLClass(IRI.create(ontologyIRI+"#man"));
		OWLIndividual mary = factory.getOWLNamedIndividual(IRI.create(ontologyIRI+"#Mary"));
		
		
		OWLDeclarationAxiom maryDeclarationAxiom = factory.getOWLDeclarationAxiom((OWLEntity) mary);
		OWLDeclarationAxiom womanDeclarationAxiom = factory.getOWLDeclarationAxiom((OWLEntity) woman);
		OWLDeclarationAxiom manDeclarationAxiom = factory.getOWLDeclarationAxiom((OWLEntity) man);
		OWLDeclarationAxiom personDeclarationAxiom = factory.getOWLDeclarationAxiom((OWLEntity) person);
		
		
		OWLClassAssertionAxiom maryAxiom = factory.getOWLClassAssertionAxiom(woman, mary);
		OWLSubClassOfAxiom womanKindOfPerson = factory.getOWLSubClassOfAxiom(woman, person);
		
		
		OWLObjectProperty objectProperty = factory.getOWLObjectProperty(IRI.create(ontologyIRI+"hasSpouse"));
		OWLObjectPropertyDomainAxiom domain = factory.getOWLObjectPropertyDomainAxiom(objectProperty, man);
		OWLObjectPropertyRangeAxiom range = factory.getOWLObjectPropertyRangeAxiom(objectProperty, woman);
		axiomsE.add(range);
		axiomsE.add(domain);
		
		OWLSymmetricObjectPropertyAxiom symAxiom = factory.getOWLSymmetricObjectPropertyAxiom(objectProperty);
		
		manager.addAxiom(ontology, maryDeclarationAxiom);
		manager.addAxiom(ontology, womanDeclarationAxiom);
		manager.addAxiom(ontology, manDeclarationAxiom);
		manager.addAxiom(ontology, personDeclarationAxiom);
		manager.addAxiom(ontology, womanKindOfPerson);
		manager.addAxiom(ontology, maryAxiom);
		manager.addAxioms(ontology, axiomsE);
		
		manager.addAxiom(ontology, symAxiom);
		
		manager.saveOntology(ontology,new RDFXMLOntologyFormat(),
										new StreamDocumentTarget(System.out));
		
		
		
	}

}
