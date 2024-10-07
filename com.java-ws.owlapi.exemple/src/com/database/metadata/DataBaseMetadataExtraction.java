package com.database.metadata;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StreamDocumentTarget;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataCardinalityRestriction;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLObjectCardinalityRestriction;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.reasoner.ConsoleProgressMonitor;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import com.database.bean.TableComponent;
import com.database.model.MyConnection;
import com.database.metadata.D2RQClassMap;
import com.database.metadata.D2RQDataProperty;
import com.database.metadata.D2RQObjectProperty;
import com.database.metadata.D2RQMapping;

public class DataBaseMetadataExtraction {

	private static Connection cnx;
	
	static {
		 cnx = MyConnection.getConnections();
	}
	public static ArrayList<String> getTables() throws SQLException{
		ArrayList<String> arrTables = new ArrayList<String> ();
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet resultSet = databaseMetaData.getTables(null, null, null, new String[]{"TABLE"});
		while(resultSet.next()) {
			String tableName = resultSet.getString("TABLE_NAME");
			arrTables.add(tableName);
		}
		return arrTables;
	}
	
	public static ArrayList<TableComponent> getColumnsOfSpecifiedTable(String tblName) throws SQLException {
		ArrayList<TableComponent> tblCol = new ArrayList<TableComponent>();
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet columns=databaseMetaData.getColumns(null, null,tblName, null);
		while(columns.next()){
			String columnName= columns.getString("COLUMN_NAME");
			String columnNameCapFirst = columnName.toUpperCase().charAt(0)+columnName.substring(1,columnName.length());
			String isNullable = columns.getString("IS_NULLABLE");
			String dataType=columns.getString("TYPE_NAME");
			TableComponent cmp = new TableComponent(columnNameCapFirst, dataType,isNullable);
			tblCol.add(cmp);
		}
		return tblCol;
	}
	
	public static boolean isUnique(String tblName,String colName) throws SQLException{
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet rs = databaseMetaData.getIndexInfo(null, null, tblName, false, false);
		while (rs.next()) {
			String col = rs.getString(9);
			String columnNameCapFirst = col.toUpperCase().charAt(0)+col.substring(1,col.length());
			
			if (columnNameCapFirst.equals(colName)) 
                return true;
	      }
		
		return false;
	}
	
	
	private static  ArrayList<String> getAllPrimaryKeyFromSpecifiedTable (String tblName) throws SQLException {
		 ArrayList<String> pKeys = new ArrayList<String>();
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet primaryKeys = databaseMetaData.getPrimaryKeys(null,null,tblName);
		while(primaryKeys.next()) {
			pKeys.add(primaryKeys.getString("COLUMN_NAME"));
		}
		
		return pKeys;
	}
	public static ArrayList<String> listAllPrimaryKeysOfSpecifiedTable(String tblName) throws SQLException {
	
				return getAllPrimaryKeyFromSpecifiedTable (tblName);
		
	}
	
	public static int getNumberOfPrimaryKey(String tblName) throws SQLException {
		int numberOfPrimaryKeys = 0;
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet primaryKeys = databaseMetaData.getPrimaryKeys(null,null,tblName);
		while(primaryKeys.next()) {
			numberOfPrimaryKeys++;
		}
		
		return numberOfPrimaryKeys;
	}
	public static boolean isPrimaryKey(String colName, String tblName) throws SQLException {
		 ArrayList<String> pKeys = getAllPrimaryKeyFromSpecifiedTable (tblName);
		 for(int i=0 ; i<pKeys.size();i++) {
			String col=pKeys.get(i); 
			String colCap= col.toUpperCase().charAt(0)+col.substring(1,col.length());
			 if(!colCap.equals(colName)) 
				 return false;
		 }
		return true ;
	}
	
	public static boolean isPrimaryKeyComposite(String tblName) throws SQLException {
		
		if(getNumberOfPrimaryKey(tblName)==1) return false;
		
		return true ; 
	}
	

	
	
	public static ArrayList<String> getAllForeignKeys(String tblName) throws SQLException {
		
		ArrayList<String> forKeys = new ArrayList<String>();
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet foreignKeys = databaseMetaData.getImportedKeys(null,null,tblName);
		
		while (foreignKeys.next()) {
			String fKeys = foreignKeys.getString("FKCOLUMN_NAME");
			String fkNameCapFirst = fKeys.toUpperCase().charAt(0)+fKeys.substring(1,fKeys.length());
			forKeys.add(fKeys);
			
		}
		
		return forKeys;
	}
		
		
	
	public static boolean isForeignKey(String tblName, String colName) throws SQLException {
		
		ArrayList<String> forKeys = getAllForeignKeys(tblName);
		for(int i=0;i<forKeys.size();i++) {
			String fk =  forKeys.get(i);
			String capFk = fk.toUpperCase().charAt(0)+fk.substring(1,fk.length());
			if(colName.equals(capFk)) return true;
		}
		
		return false;
	}
	
	public static String getParentTable(String tblName, String col) throws SQLException {
		System.out.println(tblName);
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet foreignKeys = databaseMetaData.getImportedKeys(null,null,tblName);
		while (foreignKeys.next()) {
			foreignKeys.getString("PKCOLUMN_NAME");
			
			System.out.println("pktable"+foreignKeys.getString("PKTABLE_NAME"));
			System.out.println("pkcol"+foreignKeys.getString("PKCOLUMN_NAME"));
			System.out.println("fktable"+foreignKeys.getString("FKTABLE_NAME"));
			System.out.println("fkname"+foreignKeys.getString("FKCOLUMN_NAME"));
			
			//irgendwie so. Brauche ich das gleiche für FKCOLUMN_Name??? -> Vermutung: Nein, da das dann für getChildTable ist,
			boolean col_equals_pk_name = col.toLowerCase().equals(foreignKeys.getString("FKCOLUMN_NAME"));
			if (col_equals_pk_name || col.equals("")) {
				return foreignKeys.getString("PKTABLE_NAME");
			}
		}
		return " ";
	}
	
	public static String getParentTable(String tblName) throws SQLException {
		return getParentTable(tblName, "");
	}
	
	public static List<String> getJoinPartners(String leftTblName, String rightTblName) throws SQLException {
		System.out.println("left_"+leftTblName + " right_" + rightTblName);
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet foreignKeys = databaseMetaData.getImportedKeys(null,null,leftTblName);
		List<String> joins = new ArrayList<>();
		while (foreignKeys.next()) {
			foreignKeys.getString("PKTABLE_NAME");
			foreignKeys.getString("FKTABLE_NAME");
			boolean right_equals_PKTable_and_left_equals_FKTable = rightTblName.equals(foreignKeys.getString("PKTABLE_NAME")) && leftTblName.equals(foreignKeys.getString("FKTABLE_NAME"));
			boolean left_equals_PKTable_and_right_equals_FKTable = rightTblName.equals(foreignKeys.getString("FKTABLE_NAME")) && leftTblName.equals(foreignKeys.getString("PKTABLE_NAME"));
			if (right_equals_PKTable_and_left_equals_FKTable || left_equals_PKTable_and_right_equals_FKTable || rightTblName == "") {
				String leftside = foreignKeys.getString("PKTABLE_NAME") + "." + foreignKeys.getString("PKCOLUMN_NAME");
				String rightside = foreignKeys.getString("FKTABLE_NAME") + "." + foreignKeys.getString("FKCOLUMN_NAME");
				if (leftside.equals(rightside)) {
					continue;
				}
				joins.add(leftside + " = " + rightside);
			}
		}
		System.out.println("joins" + joins);
		return joins;
	}
	
	public static List<String> getJoinPartners(String tblName) throws SQLException {
		return getJoinPartners(tblName, "");
	}
	
	
	
	public static ArrayList<String> getParentTables(String tblName) throws SQLException{
		ArrayList<String> fKeys = new ArrayList<String>();
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet foreignKeys = databaseMetaData.getImportedKeys(null,null,tblName);
		
		while (foreignKeys.next()) fKeys.add(foreignKeys.getString("PKTABLE_NAME"));
		return fKeys;  
	}
	
	public static ArrayList<String> getParentColumns(String tblName) throws SQLException{
		ArrayList<String> fKeys = new ArrayList<String>();
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet foreignKeys = databaseMetaData.getImportedKeys(null,null,tblName);
		
		while (foreignKeys.next()) fKeys.add(foreignKeys.getString("PKCOLUMN_NAME"));
		return fKeys;  
	}
	
	public static String getChildTable(String tblName, String col) throws SQLException {
		
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet foreignKeys = databaseMetaData.getImportedKeys(null,null,tblName);
		while (foreignKeys.next()) {
			boolean col_equals_fk_name = col.toLowerCase().equals(foreignKeys.getString("FKCOLUMN_NAME"));
			if (col_equals_fk_name || col.equals("")) {
				return foreignKeys.getString("FKTABLE_NAME");
			}
		}
		return " ";
	}
	
	public static String getChildTable(String tblName) throws SQLException {
		return getChildTable(tblName, "");
	}
	
	
	public static int getNumberOfForeignKeys(String tblName) throws SQLException
	{
		int nbForeignKeys=0;
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet foreignKeys = databaseMetaData.getImportedKeys(null,null,tblName);
			 while(foreignKeys.next()) {
				 nbForeignKeys++;
			 }

		return nbForeignKeys;
	}

	public static int  getNumberOfColumns(String tblName) throws SQLException
	{
		int nbColumns=0;
		DatabaseMetaData meta =cnx.getMetaData();
		ResultSet columns = meta.getColumns(null,null,tblName, null);

		while (columns.next()) nbColumns++;
	   return nbColumns;
			
	}
	
	public static  ArrayList<String> getAllColumns(String tblName) throws SQLException{
		ArrayList<String> allCols = new ArrayList<String>();
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet columns=databaseMetaData.getColumns(null, null,tblName, null);
		while(columns.next()) {
			allCols.add(columns.getString("COLUMN_NAME"));
		}
		
		return allCols;
	}
	
	public static boolean isAssociativeTable(String tblName) throws SQLException {
		int num =0;
		ArrayList<String> pKeys= getAllPrimaryKeyFromSpecifiedTable(tblName);
		if((getNumberOfPrimaryKey(tblName)==2) && (getNumberOfForeignKeys(tblName)==2) ) {
			for(int i=0; i<pKeys.size();i++){
				String col=pKeys.get(i); 
				String colCap= col.toUpperCase().charAt(0)+col.substring(1,col.length());
				if(isForeignKey(tblName, colCap)) num++;
			}
			System.out.println("num = "+ num);
			if(num==2) return true;
		}
		return false;
	}
	
	
	 private static String cascadeOptionToString(int option) {
		 switch (option) {
		 case DatabaseMetaData.importedKeyCascade:
			   return "CASCADE";
		 case DatabaseMetaData.importedKeySetNull:
			   return "SET NULL";
		 case DatabaseMetaData.importedKeyRestrict:
			 return "RESTRICT";
	     case DatabaseMetaData.importedKeyNoAction:
	    	 return "NO ACTION";
	   }
		 
		 return "SET DEFAULT";
	    }
	
	
	public static String foreingKeyCrossReference(String parentTable,String foreignTable) throws SQLException {
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet rs = databaseMetaData.getCrossReference(null, null, parentTable, null, null, foreignTable);
		String deleteAction=null;
		while(rs.next()) {
			 deleteAction = cascadeOptionToString(rs.getInt("DELETE_RULE"));
		}
		return deleteAction;
	}

	
	public static void main(String args[]) throws SQLException, OWLOntologyCreationException, OWLOntologyStorageException {
		ArrayList<TableComponent> cp;
		ArrayList<TableComponent> cp1;
		ArrayList<String> pKeys;
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		OWLDatatype dataType=null;
		String prefix = cnx.getCatalog();
		
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		IRI ontologyIRI = IRI.create("http://java-ws.com/ontologie/myFamily.owl");
		OWLDataFactory factory=manager.getOWLDataFactory();
		OWLOntology ontology = manager.createOntology(ontologyIRI);
		
		D2RQMapping mapping = new D2RQMapping();

		
		ArrayList<String> arrTables = getTables();
		for(int i=0; i<arrTables.size();i++) {
			boolean isNMTable = isPrimaryKeyComposite(arrTables.get(i)) && isAssociativeTable(arrTables.get(i)) && ((getNumberOfPrimaryKey(arrTables.get(i))==2)&&(getNumberOfForeignKeys(arrTables.get(i))==2) && 
					getNumberOfColumns(arrTables.get(i))==2);
			if (isNMTable) continue;
			List<String> pks = listAllPrimaryKeysOfSpecifiedTable(arrTables.get(i));
			String id = "";
			for(int j=0; j<pks.size();j++) {
				id = id + "@@" + arrTables.get(i) + "." + pks.get(j) + "@@" + "/";
			}
			D2RQClassMap classmap = new D2RQClassMap(id, arrTables.get(i), "ClassMap_" + arrTables.get(i), prefix);
			mapping.addClass(arrTables.get(i), classmap);
		}
		// für jede Tabelle
		for(int i=0; i<arrTables.size();i++) {
			System.out.println("isAssociatif- "+arrTables.get(i)+" : "+isAssociativeTable(arrTables.get(i)));
			System.out.println("Table Name: "+arrTables.get(i));
			//creates concept for each table -> Wir müssen hier also ein classmap erstellen: Was ist die URI? PK ist die URI.
			OWLClass person = factory.getOWLClass(IRI.create(ontologyIRI+"#"+arrTables.get(i)));
			//D2RQClassMap classmap = new D2RQClassMap("ClassMap_" + arrTables.get(i), "hall", "hal", prefix); 
			//mapping.addClass(arrTables.get(i), classmap);
			
			OWLDeclarationAxiom personDeclarationAxiom = factory.getOWLDeclarationAxiom((OWLEntity) person);
			 
			cp = getColumnsOfSpecifiedTable(arrTables.get(i));
			// für jede spalte
			for(int j=0; j<cp.size();j++){
				// SCENARIO 1
				// wenns kein PK ist, die spalte null sein kann und sie nicht unique ist
				// -> DatatypeProperty + MinCardinality = 1
				// done
				if((!isPrimaryKey(cp.get(j).getColName(), arrTables.get(i))) && 
						((cp.get(j).getNullable().equals("YES"))) &&(!isUnique(arrTables.get(i),cp.get(j).getColName()))) {
				
					OWLDataProperty dataproperty = factory.getOWLDataProperty(IRI.create(ontologyIRI+"#has"+cp.get(j).getColName()));
					if(cp.get(j).getDataType().equals("INT"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
					if(cp.get(j).getDataType().equals("SMALLINT"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
					if(cp.get(j).getDataType().equals("INT UNSIGNED"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
					if(cp.get(j).getDataType().equals("ENUM"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
					if(cp.get(j).getDataType().equals("TEXT"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
					if(cp.get(j).getDataType().equals("VARCHAR")) dataType = factory.getRDFPlainLiteral();
					if(cp.get(j).getDataType().equals("DATE"))    dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DATE_TIME.getIRI());
					if(cp.get(j).getDataType().equals("DATETIME"))    dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DATE_TIME.getIRI());
					if(cp.get(j).getDataType().equals("DECIMAL")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DECIMAL.getIRI());
					if(cp.get(j).getDataType().equals("FLOAT"))   dataType = factory.getOWLDatatype(OWL2Datatype.XSD_FLOAT.getIRI());
					if(cp.get(j).getDataType().equals("DOUBLE"))  dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DOUBLE.getIRI());
					if(cp.get(j).getDataType().equals("CHAR")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
					if(cp.get(j).getDataType().equals("SMALLINT UNSIGNED"))     	dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
					
					OWLDataPropertyRangeAxiom range = factory.getOWLDataPropertyRangeAxiom(dataproperty,dataType);
					OWLDataPropertyDomainAxiom domain = factory.getOWLDataPropertyDomainAxiom(dataproperty, person);
					D2RQDataProperty d2rq_range = new D2RQDataProperty("has"+cp.get(j).getColName(), mapping.getClassByKey(arrTables.get(i)), dataType.toString(), arrTables.get(i), cp.get(j).getColName());
					mapping.addDataProperty(d2rq_range.getProperty() + "_" + d2rq_range.getColumn(), d2rq_range);
					axioms.add(range);
					axioms.add(domain);
					// dataproperty für jede column
					

				}
					
				// SCENARIO 2
				// Attribute + NOT NULL
				// -> DatatypeProperty + MinCardinality = 1
				 if((cp.get(j).getNullable().equals("NO")) && (!isPrimaryKey(cp.get(j).getColName(), arrTables.get(i)))&&
						 (!isUnique(arrTables.get(i),cp.get(j).getColName()))) {
					 
					 	OWLDataProperty dataproperty = factory.getOWLDataProperty(IRI.create(ontologyIRI+"#has"+cp.get(j).getColName()));
						if(cp.get(j).getDataType().equals("INT"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
						if(cp.get(j).getDataType().equals("SMALLINT"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
						if(cp.get(j).getDataType().equals("INT UNSIGNED"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
						if(cp.get(j).getDataType().equals("ENUM"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
						if(cp.get(j).getDataType().equals("TEXT"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
						if(cp.get(j).getDataType().equals("VARCHAR")) dataType = factory.getRDFPlainLiteral();
						if(cp.get(j).getDataType().equals("DATE"))    dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DATE_TIME.getIRI());
						if(cp.get(j).getDataType().equals("DATETIME"))    dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DATE_TIME.getIRI());
						if(cp.get(j).getDataType().equals("DECIMAL")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DECIMAL.getIRI());
						if(cp.get(j).getDataType().equals("FLOAT"))   dataType = factory.getOWLDatatype(OWL2Datatype.XSD_FLOAT.getIRI());
						if(cp.get(j).getDataType().equals("DOUBLE"))  dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DOUBLE.getIRI());
						if(cp.get(j).getDataType().equals("CHAR"))    dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
						if(cp.get(j).getDataType().equals("SMALLINT UNSIGNED"))     	dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
						//map to column
						OWLDataPropertyRangeAxiom range = factory.getOWLDataPropertyRangeAxiom(dataproperty,dataType);
						OWLDataPropertyDomainAxiom domain = factory.getOWLDataPropertyDomainAxiom(dataproperty, person);	
						
						D2RQDataProperty d2rq_range = new D2RQDataProperty("has"+cp.get(j).getColName(), mapping.getClassByKey(arrTables.get(i)), dataType.toString(), arrTables.get(i), cp.get(j).getColName());
						mapping.addDataProperty(d2rq_range.getProperty() + "_" + d2rq_range.getColumn(), d2rq_range);
						
						axioms.add(range);
						axioms.add(domain);
					 
					OWLDataMinCardinality minCardinality = factory.getOWLDataMinCardinality(1, dataproperty);
					OWLSubClassOfAxiom axiom= factory.getOWLSubClassOfAxiom(person, minCardinality);				  
					manager.addAxiom(ontology, axiom);
					
			}
			// SCENARIO 3
			// Attribute + Not FK + Unique
			// -> DatatypeProperty + MaxCardinality = 1
			if(isUnique(arrTables.get(i),cp.get(j).getColName()) && (cp.get(j).getNullable().equals("YES"))&&
						 (!isPrimaryKey(cp.get(j).getColName(), arrTables.get(i))) ) {
					 OWLDataProperty dataproperty = factory.getOWLDataProperty(IRI.create(ontologyIRI+"#has"+cp.get(j).getColName()));
						if(cp.get(j).getDataType().equals("INT"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
						if(cp.get(j).getDataType().equals("SMALLINT"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
						if(cp.get(j).getDataType().equals("INT UNSIGNED"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
						if(cp.get(j).getDataType().equals("ENUM"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
						if(cp.get(j).getDataType().equals("TEXT"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
						if(cp.get(j).getDataType().equals("VARCHAR")) dataType = factory.getRDFPlainLiteral();
						if(cp.get(j).getDataType().equals("DATE"))    dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DATE_TIME.getIRI());
						if(cp.get(j).getDataType().equals("DATETIME"))    dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DATE_TIME.getIRI());
						if(cp.get(j).getDataType().equals("DECIMAL")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DECIMAL.getIRI());
						if(cp.get(j).getDataType().equals("FLOAT"))   dataType = factory.getOWLDatatype(OWL2Datatype.XSD_FLOAT.getIRI());
						if(cp.get(j).getDataType().equals("DOUBLE"))  dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DOUBLE.getIRI());
						if(cp.get(j).getDataType().equals("CHAR")) 	  dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
						if(cp.get(j).getDataType().equals("SMALLINT UNSIGNED"))     	dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
						
						OWLDataPropertyRangeAxiom range = factory.getOWLDataPropertyRangeAxiom(dataproperty,dataType);
						OWLDataPropertyDomainAxiom domain = factory.getOWLDataPropertyDomainAxiom(dataproperty, person);
						
						D2RQDataProperty d2rq_range = new D2RQDataProperty("has"+cp.get(j).getColName(), mapping.getClassByKey(arrTables.get(i)), dataType.toString(), arrTables.get(i), cp.get(j).getColName());
						mapping.addDataProperty(d2rq_range.getProperty() + "_" + d2rq_range.getColumn(), d2rq_range);
						
						axioms.add(range);
						axioms.add(domain);
					 
					OWLDataMaxCardinality maxCardinality = factory.getOWLDataMaxCardinality(1, dataproperty);
					OWLSubClassOfAxiom axiom= factory.getOWLSubClassOfAxiom(person, maxCardinality);
					manager.addAxiom(ontology, axiom);
		 }	 
			
			
			
			// SCENARIO 4
			// PK is not Composite, but PK
			// -> Datatype Property + ExactCardinality = 1
			if(!isPrimaryKeyComposite(arrTables.get(i))) {
				if(isPrimaryKey(cp.get(j).getColName(), arrTables.get(i))) {
					System.out.println(cp.get(j).getDataType());
					OWLDataProperty dataproperty = factory.getOWLDataProperty(IRI.create(ontologyIRI+"#has"+cp.get(j).getColName()));
					if(cp.get(j).getDataType().equals("INT"))     		dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
					if(cp.get(j).getDataType().equals("int4"))     		dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
					if(cp.get(j).getDataType().equals("SMALLINT"))     	dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
					if(cp.get(j).getDataType().equals("INT UNSIGNED"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
					if(cp.get(j).getDataType().equals("SMALLINT UNSIGNED"))     	dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
					if(cp.get(j).getDataType().equals("ENUM"))     		dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
					if(cp.get(j).getDataType().equals("TEXT"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
					if(cp.get(j).getDataType().equals("varchar")) dataType = factory.getRDFPlainLiteral();
					if(cp.get(j).getDataType().equals("DATE"))    dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DATE_TIME.getIRI());
					if(cp.get(j).getDataType().equals("DATETIME"))    dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DATE_TIME.getIRI());
					if(cp.get(j).getDataType().equals("DECIMAL")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DECIMAL.getIRI());
					if(cp.get(j).getDataType().equals("FLOAT"))   dataType = factory.getOWLDatatype(OWL2Datatype.XSD_FLOAT.getIRI());
					if(cp.get(j).getDataType().equals("DOUBLE"))  dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DOUBLE.getIRI());
					if(cp.get(j).getDataType().equals("CHAR")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
					
					System.out.println(dataType);
					OWLDataPropertyRangeAxiom range = factory.getOWLDataPropertyRangeAxiom(dataproperty,dataType);
					
					OWLDataPropertyDomainAxiom domain = factory.getOWLDataPropertyDomainAxiom(dataproperty, person);
					axioms.add(range);
					axioms.add(domain);
					
					OWLDataCardinalityRestriction card = factory.getOWLDataExactCardinality(1, dataproperty);
					OWLSubClassOfAxiom axiom= factory.getOWLSubClassOfAxiom(person, card);
					
					D2RQDataProperty d2rq_range = new D2RQDataProperty("has"+cp.get(j).getColName(), mapping.getClassByKey(arrTables.get(i)), dataType.toString(), arrTables.get(i), cp.get(j).getColName());
					mapping.addDataProperty(d2rq_range.getProperty() + "_" + d2rq_range.getColumn(), d2rq_range);
					
					manager.addAxiom(ontology, axiom);
					manager.addAxioms(ontology, axioms);
				}
			}	
		
			// This function maps each primary key to a "composite primary key class" in the ontology 
			// PK is Composite + no nm-Table + No FKs
			if(isPrimaryKeyComposite(arrTables.get(i)) && (!isAssociativeTable(arrTables.get(i))) &&
					getNumberOfForeignKeys(arrTables.get(i))==0) {
				Set<OWLAxiom> axioms1 = new HashSet<OWLAxiom>();
				Set<OWLAxiom> axioms2 = new HashSet<OWLAxiom>();
				ArrayList<String> p = listAllPrimaryKeysOfSpecifiedTable(arrTables.get(i));
				OWLClass cmpClass = factory.getOWLClass(IRI.create(ontologyIRI+"#pk_Class"+arrTables.get(i)));
				System.out.println("AJAJAJAJ");
				System.out.println(cmpClass);
				OWLDeclarationAxiom cmpClassDeclarationAxiom = factory.getOWLDeclarationAxiom((OWLEntity) cmpClass);
				System.out.println(cmpClassDeclarationAxiom);
				manager.addAxiom(ontology, cmpClassDeclarationAxiom);
				// für jeden primary key
				for(int k=0; k<p.size(); k++) {
					//Each column of primary key has cmp_class for its domain -> Ich vermute, dass das eine Klasse ist, die den composite key zusammenfässt
					OWLDataProperty dataproperty = factory.getOWLDataProperty(IRI.create(ontologyIRI+"#has"+p.get(k)));
					if(cp.get(k).getDataType().equals("INT"))     		dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
					if(cp.get(k).getDataType().equals("SMALLINT"))    	dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
					if(cp.get(k).getDataType().equals("INT UNSIGNED"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
					if(cp.get(k).getDataType().equals("ENUM"))     		dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
					if(cp.get(j).getDataType().equals("TEXT"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
					if(cp.get(k).getDataType().toUpperCase().equals("VARCHAR")) 		dataType = factory.getRDFPlainLiteral();
					if(cp.get(k).getDataType().equals("DATE"))    		dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DATE_TIME.getIRI());
					if(cp.get(k).getDataType().equals("DATETIME"))    dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DATE_TIME.getIRI());
					if(cp.get(k).getDataType().equals("DECIMAL"))		dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DECIMAL.getIRI());
					if(cp.get(k).getDataType().equals("FLOAT"))  		dataType = factory.getOWLDatatype(OWL2Datatype.XSD_FLOAT.getIRI());
					if(cp.get(k).getDataType().equals("DOUBLE")) 		dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DOUBLE.getIRI());
					if(cp.get(k).getDataType().equals("CHAR")) 			dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
					if(cp.get(k).getDataType().equals("SMALLINT UNSIGNED"))     	dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
					OWLDataPropertyRangeAxiom range1 = factory.getOWLDataPropertyRangeAxiom(dataproperty,dataType);
					OWLDataPropertyDomainAxiom domain1 = factory.getOWLDataPropertyDomainAxiom(dataproperty, cmpClass);
					axioms1.add(range1);
					axioms1.add(domain1);
					System.out.println(dataproperty);
					//-> Sagt quasi was der Primary KEy ist. Brauche ich aus meiner Sicht nicht.
					OWLObjectProperty objectProperty = factory.getOWLObjectProperty(IRI.create(ontologyIRI+"#hasPkClassOP"+arrTables.get(i)));
					System.out.println(objectProperty);
					
					
					//D2RQDataProperty d2rq_range = new D2RQDataProperty("#has"+p.get(k), mapping.getClassByKey(arrTables.get(i)), dataType.toString(), arrTables.get(i), cp.get(j).getColName());
					//mapping.addDataProperty(d2rq_range.property + "_" + d2rq_range.column, d2rq_range);
					
					
					OWLObjectPropertyRangeAxiom range2 = factory.getOWLObjectPropertyRangeAxiom(objectProperty, cmpClass);
					OWLObjectPropertyDomainAxiom domain2 = factory.getOWLObjectPropertyDomainAxiom(objectProperty, person);
					OWLInverseFunctionalObjectPropertyAxiom invAxiom = factory.getOWLInverseFunctionalObjectPropertyAxiom(objectProperty);

					OWLDataMinCardinality minCardinality = factory.getOWLDataMinCardinality(1, dataproperty);
					OWLSubClassOfAxiom axiom= factory.getOWLSubClassOfAxiom(person, minCardinality);
					manager.addAxiom(ontology, axiom);
					
					axioms2.add(range2);
					axioms2.add(domain2);
					
					manager.addAxioms(ontology, axioms1);
					manager.addAxioms(ontology, axioms2);
					manager.addAxiom(ontology, invAxiom);
				} 
			 }
			
			//Scenario1 pour FK: FK is simple Colmun (not a primary key)
			// not PKComposite + not PK + FK
			// -> has-Relationship with child and parent node (domain / range)
			if(!isPrimaryKeyComposite(arrTables.get(i))) {
				if((!isPrimaryKey(cp.get(j).getColName(), arrTables.get(i))) && (isForeignKey(arrTables.get(i), cp.get(j).getColName()))){
					System.out.println("Scenario1 pour FK: FK is simple Colmun (not a primary key)");
					System.out.println("The col Name: "+ cp.get(j).getColName());
					Set<OWLAxiom> axiomsA = new HashSet<OWLAxiom>();
					
					// hier werden die klassen aus den tabellen gebastelt
					OWLClass parentClass = factory.getOWLClass(IRI.create(ontologyIRI+"#"+getParentTable(arrTables.get(i))));
					OWLClass childClass = factory.getOWLClass(IRI.create(ontologyIRI+"#"+getChildTable(arrTables.get(i))));
					
					//hier wird die relationship gebaut
					OWLObjectProperty objectProperty = factory.getOWLObjectProperty(IRI.create(ontologyIRI+"#has"+cp.get(j).getColName()));
					OWLObjectPropertyDomainAxiom domain = factory.getOWLObjectPropertyDomainAxiom(objectProperty, childClass);
					OWLObjectPropertyRangeAxiom range = factory.getOWLObjectPropertyRangeAxiom(objectProperty, parentClass);
					
					// Adding Mapping
					D2RQClassMap parentMap = mapping.getClassByKey(getParentTable(arrTables.get(i))); // Es kann passieren, dass es diese Tabelle noch nicht gibt!
					String join = getJoinPartners(arrTables.get(i)).get(0); //TODO kann das hier nur einen join geben?
					D2RQClassMap childMap = mapping.getClassByKey(getChildTable(arrTables.get(i))); // Es kann passieren, dass es diese Tabelle noch nicht gibt!
					List<String> joins = new ArrayList<>();
					joins.add(join);
					String property = "has"+cp.get(j).getColName();
					D2RQObjectProperty objectproperty = new D2RQObjectProperty(property, childMap, parentMap, joins);
					mapping.addObjectProperty(property + "_" + getParentTable(arrTables.get(i)) + "_" + getChildTable(arrTables.get(i)), objectproperty);
					
					axiomsA.add(domain);
					axiomsA.add(range);
					manager.addAxioms(ontology, axiomsA);
				}
			}
			
			
			//Scenario2 pour FK: FK is part of primary key
			// CompositePK + Zahl der PKs übersteigt FKs
			if(isPrimaryKeyComposite(arrTables.get(i)) && (getNumberOfPrimaryKey(arrTables.get(i))>getNumberOfForeignKeys(arrTables.get(i))) ) {
				
				System.out.println("Scenario2 pour FK: FK is part of primary key");
				Set<OWLAxiom> axiomsB = new HashSet<OWLAxiom>();
				ArrayList<String> p= listAllPrimaryKeysOfSpecifiedTable(arrTables.get(i));
				for(int k =0;k<p.size();k++) {
					// für jede Spalte die teil des PKs ist, wird geschaut, ob die Spalte auch FK ist
					if(isForeignKey(arrTables.get(i), cp.get(j).getColName())) {
						System.out.println("Bilal");
						OWLClass parentClass = factory.getOWLClass(IRI.create(ontologyIRI+"#"+getParentTable(arrTables.get(i))));
						OWLClass childClass = factory.getOWLClass(IRI.create(ontologyIRI+"#"+getChildTable(arrTables.get(i))));
						System.out.println(arrTables.get(i));
						System.out.println("parent" + getParentTable(arrTables.get(i)));
						System.out.println("chikd" + getChildTable(arrTables.get(i)));
						OWLObjectProperty objectProperty = factory.getOWLObjectProperty(IRI.create(ontologyIRI+"#has"+cp.get(j).getColName()));
						OWLObjectPropertyDomainAxiom domain = factory.getOWLObjectPropertyDomainAxiom(objectProperty, childClass);
						OWLObjectPropertyRangeAxiom range = factory.getOWLObjectPropertyRangeAxiom(objectProperty, parentClass);
						OWLFunctionalObjectPropertyAxiom axiom = factory.getOWLFunctionalObjectPropertyAxiom(objectProperty);
						
						D2RQClassMap parentMap = mapping.getClassByKey(getParentTable(arrTables.get(i))); // Es kann passieren, dass es diese Tabelle noch nicht gibt!
						String join = getJoinPartners(getChildTable(arrTables.get(i)), getParentTable(arrTables.get(i))).get(0);//TODO Kann es hier nur einen join partner geben???
						D2RQClassMap childMap = mapping.getClassByKey(getChildTable(arrTables.get(i))); // Es kann passieren, dass es diese Tabelle noch nicht gibt!
						List<String> joins = new ArrayList<>();
						joins.add(join);
						String property = "has"+cp.get(j).getColName();
						D2RQObjectProperty objectproperty = new D2RQObjectProperty(property, childMap, parentMap, joins);
						mapping.addObjectProperty(property + "_" + getParentTable(arrTables.get(i)) + "_" + getChildTable(arrTables.get(i)), objectproperty);
						
						axiomsB.add(range);
						axiomsB.add(domain);
						manager.addAxioms(ontology, axiomsB);
						manager.addAxiom(ontology, axiom);	
					}
					
					else {
						OWLClass cl = factory.getOWLClass(IRI.create(ontologyIRI+"#"+arrTables.get(i)));
						OWLDeclarationAxiom clDeclarationAxiom = factory.getOWLDeclarationAxiom((OWLEntity) person);
						System.out.println("Non BILAL");
						
						OWLDataProperty dataproperty = factory.getOWLDataProperty(IRI.create(ontologyIRI+"#has"+cp.get(j).getColName()));
						if(cp.get(j).getDataType().equals("INT"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
						if(cp.get(j).getDataType().equals("SMALLINT"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
						if(cp.get(j).getDataType().equals("INT UNSIGNED"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
						if(cp.get(j).getDataType().equals("ENUM"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
						if(cp.get(j).getDataType().equals("TEXT"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
						if(cp.get(j).getDataType().equals("VARCHAR")) dataType = factory.getRDFPlainLiteral();
						if(cp.get(j).getDataType().equals("CHAR")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
						if(cp.get(j).getDataType().equals("DATE"))    dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DATE_TIME.getIRI());
						if(cp.get(j).getDataType().equals("DATETIME"))    dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DATE_TIME.getIRI());
						if(cp.get(j).getDataType().equals("DECIMAL")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DECIMAL.getIRI());
						if(cp.get(j).getDataType().equals("FLOAT"))   dataType = factory.getOWLDatatype(OWL2Datatype.XSD_FLOAT.getIRI());
						if(cp.get(j).getDataType().equals("DOUBLE"))  dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DOUBLE.getIRI());
						
						OWLDataPropertyRangeAxiom range = factory.getOWLDataPropertyRangeAxiom(dataproperty,dataType);
						OWLDataPropertyDomainAxiom domain = factory.getOWLDataPropertyDomainAxiom(dataproperty, cl);
						
						D2RQDataProperty d2rq_range = new D2RQDataProperty("has"+cp.get(j).getColName(), mapping.getClassByKey(arrTables.get(i)), dataType.toString(), arrTables.get(i), cp.get(j).getColName());
						mapping.addDataProperty(d2rq_range.getProperty() + "_" + d2rq_range.getColumn(), d2rq_range);
						
						System.out.println(range);
						System.out.println(domain);
						axioms.add(range);
						axioms.add(domain);
					 
					OWLDataMinCardinality minCardinality = factory.getOWLDataMinCardinality(1, dataproperty);
					OWLSubClassOfAxiom axiom= factory.getOWLSubClassOfAxiom(person, minCardinality);	
					manager.addAxiom(ontology, clDeclarationAxiom);
					manager.addAxiom(ontology, axiom);
						
					}
				}
			}
			
			//Scenario3: Many-to-Many Relationship (primary key is the full foreign key)
			//oben wird schon eine klasse erstellt, obwohl laut paper das nicht notwendig ist
			
			// CompositePK & AssociativeTable & #PK == 2 & #FK == 2 & #Columns == 2
			if(isPrimaryKeyComposite(arrTables.get(i)) && isAssociativeTable(arrTables.get(i)) && ((getNumberOfPrimaryKey(arrTables.get(i))==2)&&(getNumberOfForeignKeys(arrTables.get(i))==2) && 
					getNumberOfColumns(arrTables.get(i))==2)) {
				Set<OWLAxiom> axiomsC = new HashSet<OWLAxiom>();
				System.out.println("Scenario3: Many-to-Many Relationship (primary key is the full foreign key)");
				ArrayList<String> p= listAllPrimaryKeysOfSpecifiedTable(arrTables.get(i));
				ArrayList<String> fk = getParentTables(arrTables.get(i));
				ArrayList<String> fk_cols = getParentColumns(arrTables.get(i));
				System.out.println(fk);
				System.out.println(fk_cols);
				// für jeden PK
				for(int k =0;k<p.size();k++) {
					String col=p.get(k); 
					String colCap= col.toUpperCase().charAt(0)+col.substring(1,col.length());
					if(isForeignKey(arrTables.get(i),colCap)) {
						System.out.println("Many-to-Many Scenario: Two Object Properties will be created in this case");
						
						for(int l =0;k<fk.size();l++) {
							
							OWLClass parentClass 	= 	factory.getOWLClass(IRI.create(ontologyIRI+"#"+fk.get(l)));
							OWLClass childClass 	= 	factory.getOWLClass(IRI.create(ontologyIRI+"#"+fk.get(l+1)));
							
							String col1=fk.get(l); //left class
							String colCap1= col1.toUpperCase().charAt(0)+col1.substring(1,col1.length());
							String col2=fk.get(l+1); //right class
							String colCap2= col2.toUpperCase().charAt(0)+col2.substring(1,col2.length());
							
							System.out.println("coco");
							OWLObjectProperty objectProperty = factory.getOWLObjectProperty(IRI.create(ontologyIRI+"#has"+colCap1+""+colCap2));
							System.out.println("Object Property:"+objectProperty);
							OWLObjectPropertyDomainAxiom domain = factory.getOWLObjectPropertyDomainAxiom(objectProperty, parentClass);
							OWLObjectPropertyRangeAxiom range = factory.getOWLObjectPropertyRangeAxiom(objectProperty, childClass);
							
							D2RQClassMap parentMap = mapping.getClassByKey(fk.get(l)); // Es kann passieren, dass es diese Tabelle noch nicht gibt!
							List<String> joins = getJoinPartners(arrTables.get(i));
							D2RQClassMap childMap = mapping.getClassByKey(fk.get(l+1)); // Es kann passieren, dass es diese Tabelle noch nicht gibt!
							String property = "#has"+colCap1+""+colCap2;
							String inverseProperty = "#has"+colCap2+""+colCap1;
							D2RQObjectProperty objectproperty = new D2RQObjectProperty(property, childMap, parentMap, joins, inverseProperty);
							mapping.addObjectProperty(property + "_" + getParentTable(arrTables.get(i)) + "_" + getChildTable(arrTables.get(i)), objectproperty);
						
							OWLObjectProperty objectProperty1 = factory.getOWLObjectProperty(IRI.create(ontologyIRI+"#has"+colCap2+""+colCap1));
							OWLInverseObjectPropertiesAxiom invObjAxiom= factory.getOWLInverseObjectPropertiesAxiom(objectProperty1, objectProperty);
							
							axiomsC.add(range);
							axiomsC.add(domain);
							manager.addAxioms(ontology, axiomsC);
							manager.addAxiom(ontology, invObjAxiom);
							break;
						}
					}
				
				}
			}
			
			//Scenario4: Many-to-Many with attribute Relationship (primary key is the full foreign key)
			//einziger unterschied ist, dass es mehr als zwei attribute gibt 
			if(isPrimaryKeyComposite(arrTables.get(i)) && isAssociativeTable(arrTables.get(i)) && ((getNumberOfPrimaryKey(arrTables.get(i))==2)&&(getNumberOfForeignKeys(arrTables.get(i))==2))) {
				Set<OWLAxiom> axiomsD = new HashSet<OWLAxiom>();
				arrTables.get(i);
				ArrayList<String> p= listAllPrimaryKeysOfSpecifiedTable(arrTables.get(i));
				ArrayList<String> cols = getAllColumns(arrTables.get(i));
				ArrayList<TableComponent> cp2= getColumnsOfSpecifiedTable(arrTables.get(i));
				
				if(getNumberOfColumns(arrTables.get(i))>2) {
				 System.out.println("HI");
					OWLClass relationshipClass 	= factory.getOWLClass(IRI.create(ontologyIRI+"#"+arrTables.get(i)));
					
					// 
					for(int l=0 ; l<cp2.size();l++) {
						if(!isForeignKey(arrTables.get(i), cp2.get(l).getColName())){
							
							OWLDataProperty dataproperty = factory.getOWLDataProperty(IRI.create(ontologyIRI+"#has"+cp.get(l).getColName())); 
							if(cp.get(l).getDataType().equals("INT"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
							if(cp.get(l).getDataType().equals("SMALLINT"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
							if(cp.get(l).getDataType().equals("INT UNSIGNED"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
							if(cp.get(l).getDataType().equals("ENUM"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
							if(cp.get(j).getDataType().equals("TEXT"))     dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
							if(cp.get(l).getDataType().equals("VARCHAR")) dataType = factory.getRDFPlainLiteral();
							if(cp.get(l).getDataType().equals("DATE"))    dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DATE_TIME.getIRI());
							if(cp.get(l).getDataType().equals("DATETIME"))    dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DATE_TIME.getIRI());
							if(cp.get(l).getDataType().equals("DECIMAL")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DECIMAL.getIRI());
							if(cp.get(l).getDataType().equals("FLOAT"))   dataType = factory.getOWLDatatype(OWL2Datatype.XSD_FLOAT.getIRI());
							if(cp.get(l).getDataType().equals("DOUBLE"))  dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DOUBLE.getIRI());
							if(cp.get(l).getDataType().equals("CHAR")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
							if(cp.get(l).getDataType().equals("SMALLINT UNSIGNED"))     	dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
							OWLDataPropertyRangeAxiom range = factory.getOWLDataPropertyRangeAxiom(dataproperty,dataType);
							OWLDataPropertyDomainAxiom domain = factory.getOWLDataPropertyDomainAxiom(dataproperty, relationshipClass);
							
							D2RQDataProperty d2rq_range = new D2RQDataProperty("#has"+cp.get(j).getColName(), mapping.getClassByKey(arrTables.get(i)), dataType.toString(), arrTables.get(i), cp.get(j).getColName());
							mapping.addDataProperty(d2rq_range.getProperty() + "_" + d2rq_range.getColumn(), d2rq_range);
							
							axiomsD.add(range);
							axiomsD.add(domain);
							
							manager.addAxioms(ontology, axiomsD);
						}
						
						if(isForeignKey(arrTables.get(i), cp2.get(l).getColName()))  //erweiterung schreiben, die für eine Tabelle und die zugehörige Column die Parent und ChildTable queried.
						{
							OWLClass parentClass = factory.getOWLClass(IRI.create(ontologyIRI+"#"+getParentTable(arrTables.get(i))));
							OWLClass childClass = factory.getOWLClass(IRI.create(ontologyIRI+"#"+getChildTable(arrTables.get(i))));
							String property = "#has"+cp.get(l).getColName();
							String inverseProperty = "#is"+cp.get(l).getColName()+"Of";
							
							OWLObjectProperty objectProperty = factory.getOWLObjectProperty(IRI.create(ontologyIRI+property));
							OWLObjectPropertyDomainAxiom domain = factory.getOWLObjectPropertyDomainAxiom(objectProperty, parentClass);
							OWLObjectPropertyRangeAxiom range = factory.getOWLObjectPropertyRangeAxiom(objectProperty, childClass);
							OWLObjectCardinalityRestriction card = factory.getOWLObjectExactCardinality(1, objectProperty);
							OWLSubClassOfAxiom axiom= factory.getOWLSubClassOfAxiom(parentClass, card);
							OWLObjectProperty objectProperty1 = factory.getOWLObjectProperty(IRI.create(ontologyIRI+inverseProperty));
							OWLInverseObjectPropertiesAxiom invObjAxiom= factory.getOWLInverseObjectPropertiesAxiom(objectProperty1, objectProperty);
							
							D2RQClassMap parentMap = mapping.getClassByKey(getParentTable(arrTables.get(i), cp2.get(l).getColName())); // Es kann passieren, dass es diese Tabelle noch nicht gibt!
							D2RQClassMap childMap = mapping.getClassByKey(getChildTable(arrTables.get(i), cp2.get(l).getColName())); // Es kann passieren, dass es diese Tabelle noch nicht gibt!
							List<String> joins = getJoinPartners(getChildTable(arrTables.get(i), cp2.get(l).getColName()), getParentTable(arrTables.get(i), cp2.get(l).getColName()));
							D2RQObjectProperty objectproperty = new D2RQObjectProperty(property, childMap, parentMap, joins);
							mapping.addObjectProperty(property + "_" + getParentTable(arrTables.get(i)) + "_" + getChildTable(arrTables.get(i)), objectproperty);
							
							
							axiomsD.add(range);
							axiomsD.add(domain);
							manager.addAxioms(ontology, axiomsD);
							manager.addAxiom(ontology, axiom);
							manager.addAxiom(ontology, invObjAxiom);
						}		
					}
				}
			}// hinzugefügt von mir, damit scenario 5 überhaipt betrachtet werden kann
				
				//Scenario5: The foreign key is the same primary key (Inheritance) 
				
				if(!isPrimaryKeyComposite(arrTables.get(i))) {
					
					if(isPrimaryKey(cp.get(j).getColName(), arrTables.get(i)) && isForeignKey(arrTables.get(i), cp.get(j).getColName())) {
						
						System.out.println("Scenario5: The foreign key is the same primary key (Inheritance) ");
						String parentTable = getParentTable(arrTables.get(i));
						String childTable = getChildTable(arrTables.get(i));
						
						OWLClass parentClass = factory.getOWLClass(IRI.create(ontologyIRI+"#"+parentTable));
						OWLClass childClass = factory.getOWLClass(IRI.create(ontologyIRI+"#"+childTable));
						OWLSubClassOfAxiom childClassisKindOfParentClass = factory.getOWLSubClassOfAxiom(childClass, parentClass);
						// add sibclass realtionship
						
						manager.addAxiom(ontology, childClassisKindOfParentClass);
						
					}
				}
				
				//Scenario6: the child and parent table for the FK are the same (Symmetric)
				if(isPrimaryKeyComposite(arrTables.get(i))) {
					System.out.println("Sysa:"+arrTables.get(i));
					if(isForeignKey(arrTables.get(i), cp.get(j).getColName()) && getChildTable(arrTables.get(i)).equals(getParentTable(arrTables.get(i)))) {
						System.out.println("Scenario6: the child and parent table for the FK are the same (Symmetric)");
						String property = "has"+cp.get(j).getColName();
						Set<OWLAxiom> axiomsE = new HashSet<OWLAxiom>();
						OWLClass parentClass = factory.getOWLClass(IRI.create(ontologyIRI+"#"+getParentTable(arrTables.get(i))));
						OWLClass childClass = factory.getOWLClass(IRI.create(ontologyIRI+"#"+getChildTable(arrTables.get(i))));
						System.out.println("sce6"+getParentTable(arrTables.get(i),  cp.get(j).getColName()));
						System.out.println("sce6 child"+getChildTable(arrTables.get(i),  cp.get(j).getColName()));
						D2RQClassMap parentMap = mapping.getClassByKey(getParentTable(arrTables.get(i),  cp.get(j).getColName())); // Es kann passieren, dass es diese Tabelle noch nicht gibt!
						D2RQClassMap childMap = mapping.getClassByKey(getChildTable(arrTables.get(i),  cp.get(j).getColName())); // Es kann passieren, dass es diese Tabelle noch nicht gibt!
						List<String> joins = getJoinPartners(getChildTable(arrTables.get(i),  cp.get(j).getColName()), getParentTable(arrTables.get(i), cp.get(j).getColName()));
						D2RQObjectProperty objectproperty = new D2RQObjectProperty(property, childMap, parentMap, joins);
						mapping.addObjectProperty(property + "_" + getParentTable(arrTables.get(i)) + "_" + getChildTable(arrTables.get(i)), objectproperty);
						
						OWLObjectProperty objectProperty = factory.getOWLObjectProperty(IRI.create(ontologyIRI+property));
						OWLObjectPropertyDomainAxiom domain = factory.getOWLObjectPropertyDomainAxiom(objectProperty, parentClass);
						OWLObjectPropertyRangeAxiom range = factory.getOWLObjectPropertyRangeAxiom(objectProperty, childClass);
						
						OWLSymmetricObjectPropertyAxiom symAxiom = factory.getOWLSymmetricObjectPropertyAxiom(objectProperty);
						
						axiomsE.add(range);
						axiomsE.add(domain);
						
						manager.addAxioms(ontology, axiomsE);
						manager.addAxiom(ontology, symAxiom);
					}
				}
				//Scenario7: On delete Cascade Transitivite
				
				/*
				if(!isPrimaryKeyComposite(arrTables.get(i))){
					String kindOfAction = foreingKeyCrossReference(getParentTable(arrTables.get(i)) , getChildTable(arrTables.get(i)));
					if(kindOfAction.equals("CASCADE")) {
					 if(isForeignKey(arrTables.get(i), cp.get(j).getColName()) && getChildTable(arrTables.get(i)).equals(getParentTable(arrTables.get(i)))) {
						 
						Set<OWLAxiom> axiomsF = new HashSet<OWLAxiom>();
						OWLClass parentClass = factory.getOWLClass(IRI.create(ontologyIRI+"#"+getParentTable(arrTables.get(i))));
						OWLClass childClass = factory.getOWLClass(IRI.create(ontologyIRI+"#"+getChildTable(arrTables.get(i))));
						OWLObjectProperty objectProperty = factory.getOWLObjectProperty(IRI.create(ontologyIRI+"sub"+cp.get(j).getColName()));
						OWLObjectPropertyDomainAxiom domain = factory.getOWLObjectPropertyDomainAxiom(objectProperty, parentClass);
						OWLObjectPropertyRangeAxiom range = factory.getOWLObjectPropertyRangeAxiom(objectProperty, childClass);
						
						OWLTransitiveObjectPropertyAxiom trAxiom = factory.getOWLTransitiveObjectPropertyAxiom(objectProperty);
						
						axiomsF.add(range);
						axiomsF.add(domain);
						manager.addAxioms(ontology, axiomsF);
						manager.addAxiom(ontology, trAxiom);		
					}
				}
				*/
					
					//scenario8: ternary relationship 
			//}		
		}
	  }
			//manager.addAxiom(ontology, personDeclarationAxiom);
			manager.addAxioms(ontology, axioms);
			
			 
			
			mapping.writeToJsonFile("/Users/lukaslaskowski/Documents/HPI/KG/ontology_mappings/temp/output.json");
		}
	
		/* wieder einkommentieren!
		manager.saveOntology(ontology,new TurtleOntologyFormat(),
				new StreamDocumentTarget(System.out));
		ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
		OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor);
		 ///*
		 */
		
		
		
		//Scenario 15: il faut nettoyer data 
		 
		
		

	}

