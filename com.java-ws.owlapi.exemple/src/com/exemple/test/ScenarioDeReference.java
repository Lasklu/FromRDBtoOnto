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
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

public class ScenarioDeReference {

	public static void main(String args[]) throws OWLOntologyCreationException, OWLOntologyStorageException {
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		IRI ontologyIRI = IRI.create("http://java-ws.com/ontologie/myFamily.owl");
		OWLDataFactory factory=manager.getOWLDataFactory();
		OWLOntology ontology = manager.createOntology(ontologyIRI);
		
		OWLClass person = factory.getOWLClass(IRI.create(ontologyIRI+"#Person"));
		OWLClass human = factory.getOWLClass(IRI.create(ontologyIRI+"#Human"));
		OWLClass man =factory.getOWLClass(IRI.create(ontologyIRI+"#Man"));
		OWLClass woman = factory.getOWLClass(IRI.create(ontologyIRI+"#Woman"));
		
		OWLIndividual mary = factory.getOWLNamedIndividual(IRI.create(ontologyIRI+"#Mary"));
		OWLIndividual jhon = factory.getOWLNamedIndividual(IRI.create(ontologyIRI+"#Jhon"));
		OWLIndividual jim = factory.getOWLNamedIndividual(IRI.create(ontologyIRI+"#Jim"));
		OWLIndividual bill = factory.getOWLNamedIndividual(IRI.create(ontologyIRI+"#Bill"));
		OWLIndividual james = factory.getOWLNamedIndividual(IRI.create(ontologyIRI+"#James"));
		OWLIndividual jack = factory.getOWLNamedIndividual(IRI.create(ontologyIRI+"#Jack"));
		
		OWLDisjointClassesAxiom manOrWoman		= factory.getOWLDisjointClassesAxiom(woman,man);
		OWLEquivalentClassesAxiom personIsHuman	= factory.getOWLEquivalentClassesAxiom(person,human);
		OWLSubClassOfAxiom womanKindOfPerson 	= factory.getOWLSubClassOfAxiom(woman, person);
		OWLSubClassOfAxiom manKindOfPerson 		= factory.getOWLSubClassOfAxiom(man, person);
		
		OWLClassAssertionAxiom maryAxiom = factory.getOWLClassAssertionAxiom(woman, mary);
		OWLClassAssertionAxiom jhonAxiom = factory.getOWLClassAssertionAxiom(man, jhon);
		OWLClassAssertionAxiom jimAxiom = factory.getOWLClassAssertionAxiom(person, jim);
		OWLClassAssertionAxiom billAxiom = factory.getOWLClassAssertionAxiom(man, bill);
		OWLClassAssertionAxiom jamesAxiom = factory.getOWLClassAssertionAxiom(man, james);
		OWLClassAssertionAxiom jackAxiom = factory.getOWLClassAssertionAxiom(man, jack);
		
		OWLObjectProperty hasWife = factory.getOWLObjectProperty(IRI.create(ontologyIRI+"#hasWife"));
		OWLObjectPropertyRangeAxiom rangeAxiom =  factory.getOWLObjectPropertyRangeAxiom(hasWife, woman);
		OWLObjectPropertyDomainAxiom domainAxiom = factory.getOWLObjectPropertyDomainAxiom(hasWife, man);
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		axioms.add(rangeAxiom);
		axioms.add(domainAxiom);
		
		
		
		
		OWLObjectPropertyAssertionAxiom hasWifeAxiom = factory.getOWLObjectPropertyAssertionAxiom(hasWife, jhon, mary);
		OWLNegativeObjectPropertyAssertionAxiom hasNotWifeAxiom = factory.getOWLNegativeObjectPropertyAssertionAxiom(hasWife, bill, mary);
		
		OWLObjectProperty hasSpouse = factory.getOWLObjectProperty(IRI.create(ontologyIRI+"#hasSpouse"));
		OWLSubObjectPropertyOfAxiom hasSpouseAxiom = factory.getOWLSubObjectPropertyOfAxiom(hasWife, hasSpouse);
		
		OWLDifferentIndividualsAxiom differentAxiom = factory.getOWLDifferentIndividualsAxiom(bill,jhon);
		OWLSameIndividualAxiom sameAxiom = factory.getOWLSameIndividualAxiom(james,jim);
		
		OWLDataProperty hasAge = factory.getOWLDataProperty(IRI.create(ontologyIRI+"#hasAge"));
		OWLDatatype integerDataType = factory.getIntegerOWLDatatype();
		OWLDataPropertyRangeAxiom range = factory.getOWLDataPropertyRangeAxiom(hasAge, integerDataType);
		OWLDataPropertyDomainAxiom domain = factory.getOWLDataPropertyDomainAxiom(hasAge, person);
		Set<OWLAxiom> axioms1 = new HashSet<OWLAxiom>();
		axioms1.add(range);
		axioms1.add(domain);
		
		
		OWLDataPropertyAssertionAxiom hasAgeAxiom = factory.getOWLDataPropertyAssertionAxiom(hasAge, jhon, 25);
		
		
		OWLLiteral age = factory.getOWLLiteral(53);
		OWLNegativeDataPropertyAssertionAxiom hasNotAgeAxiom = factory.getOWLNegativeDataPropertyAssertionAxiom
																	(hasAge, jack, age);
		
		manager.addAxiom(ontology, manOrWoman);
		manager.addAxiom(ontology, personIsHuman);
		manager.addAxiom(ontology, womanKindOfPerson);
		manager.addAxiom(ontology, manKindOfPerson);
		manager.addAxiom(ontology, maryAxiom);
		manager.addAxiom(ontology, jhonAxiom);
		manager.addAxiom(ontology, jimAxiom);
		manager.addAxiom(ontology, hasWifeAxiom);
		manager.addAxiom(ontology, hasNotWifeAxiom);
		manager.addAxiom(ontology,  hasSpouseAxiom);
		manager.addAxioms(ontology, axioms);
		manager.addAxioms(ontology, axioms1);
		manager.addAxiom(ontology, differentAxiom);
		manager.addAxiom(ontology, sameAxiom);
		manager.addAxiom(ontology, hasAgeAxiom);
		manager.addAxiom(ontology, hasNotAgeAxiom);
		manager.saveOntology(ontology,new RDFXMLOntologyFormat(),
				new StreamDocumentTarget(System.out));
		
		
		System.out.println("############Ontology Inferee################");
		System.out.println("#######Resultat aprés les inférences#####");
		PelletReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
		
		OWLOntology inferOntology = manager.createOntology();
		PelletReasoner reasoner = (PelletReasoner)reasonerFactory.createNonBufferingReasoner(ontology);
		
		InferredOntologyGenerator iog = new InferredOntologyGenerator(reasoner);
		
		iog.fillOntology(manager, inferOntology);
		
		
		
		manager.saveOntology(inferOntology, new RDFXMLOntologyFormat(),
				new StreamDocumentTarget(System.out));
	}
}
