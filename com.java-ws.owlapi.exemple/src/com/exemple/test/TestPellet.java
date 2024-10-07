package com.exemple.test;

import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StreamDocumentTarget;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

public class TestPellet {
	
	public static void main(String args[]) throws OWLOntologyCreationException, OWLOntologyStorageException {
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		IRI ontologyIRI = IRI.create("http://java-ws.com/ontology/tstpellet.owl");
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLOntology ontology = manager.createOntology(ontologyIRI);
		
		
		OWLClass person = factory.getOWLClass(IRI.create(ontologyIRI+"#Person"));
		OWLClass woman = factory.getOWLClass(IRI.create(ontologyIRI+"#Woman"));
		
		OWLIndividual mary = factory.getOWLNamedIndividual(IRI.create(ontologyIRI+"#Mary"));
		
		OWLClassAssertionAxiom maryAxiom = factory.getOWLClassAssertionAxiom(woman, mary);
		OWLSubClassOfAxiom axiom = factory.getOWLSubClassOfAxiom(woman, person);
		
		manager.addAxiom(ontology, axiom);
		manager.addAxiom(ontology, maryAxiom);
		
		manager.saveOntology(ontology, new TurtleOntologyFormat(), 
				new StreamDocumentTarget(System.out));
		
		PelletReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
		
		OWLOntology inferOntology = manager.createOntology();
		PelletReasoner reasoner = (PelletReasoner)reasonerFactory.createNonBufferingReasoner(ontology);
		
		InferredOntologyGenerator iog = new InferredOntologyGenerator(reasoner);
		
		iog.fillOntology(manager, inferOntology);
		
		System.out.println("#######Resultat apr�s les inf�rences#####");
		
		manager.saveOntology(inferOntology, new TurtleOntologyFormat(),
				new StreamDocumentTarget(System.out));	
	}

}
