package com.database.metadata;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.text.MessageFormat;

import java.util.logging.LogRecord;



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

    private static final Logger logger = Logger.getLogger(DataBaseMetadataExtraction.class.getName());
    
	private static Connection cnx;
	
	static {
		 
	}
	public static ArrayList<String> getTables() throws SQLException {
		ArrayList<String> arrTables = new ArrayList<String>();
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet resultSet = databaseMetaData.getTables(null, null, null, new String[]{"TABLE"});
		while (resultSet.next()) {
			String tableName = resultSet.getString("TABLE_NAME");
			arrTables.add(tableName);
			logger.log(Level.INFO, "Found table: {0}", tableName);
		}
		return arrTables;
	}
	
	public static ArrayList<TableComponent> getColumnsOfSpecifiedTable(String tblName) throws SQLException {
		ArrayList<TableComponent> tblCol = new ArrayList<TableComponent>();
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet columns = databaseMetaData.getColumns(null, null, tblName, null);
		while (columns.next()) {
			String columnName = columns.getString("COLUMN_NAME");
			String columnNameCapFirst = columnName.toUpperCase().charAt(0) + columnName.substring(1, columnName.length());
			String isNullable = columns.getString("IS_NULLABLE");
			String dataType = columns.getString("TYPE_NAME");
			TableComponent cmp = new TableComponent(columnNameCapFirst, dataType, isNullable);
			tblCol.add(cmp);
			logger.log(Level.INFO, "Column found: {0} of type {1}, nullable: {2}", new Object[]{columnNameCapFirst, dataType, isNullable});
		}
		return tblCol;
	}
	
	public static boolean isUnique(String tblName, String colName) throws SQLException {
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet rs = databaseMetaData.getIndexInfo(null, null, tblName, false, false);
		while (rs.next()) {
			String col = rs.getString(9);
			String columnNameCapFirst = col.toUpperCase().charAt(0) + col.substring(1, col.length());
			if (columnNameCapFirst.equals(colName)) {
				logger.log(Level.INFO, "Column {0} is unique in table {1}", new Object[]{colName, tblName});
				return true;
			}
		}
		logger.log(Level.INFO, "Column {0} is not unique in table {1}", new Object[]{colName, tblName});
		return false;
	}
	
	private static ArrayList<String> getAllPrimaryKeyFromSpecifiedTable(String tblName) throws SQLException {
		ArrayList<String> pKeys = new ArrayList<String>();
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet primaryKeys = databaseMetaData.getPrimaryKeys(null, null, tblName);
		while (primaryKeys.next()) {
			String pKey = primaryKeys.getString("COLUMN_NAME");
			pKeys.add(pKey);
			logger.log(Level.INFO, "Primary key found: {0} in table {1}", new Object[]{pKey, tblName});
		}
		return pKeys;
	}
	
	public static ArrayList<String> listAllPrimaryKeysOfSpecifiedTable(String tblName) throws SQLException {
		return getAllPrimaryKeyFromSpecifiedTable(tblName);
	}
	
	public static int getNumberOfPrimaryKey(String tblName) throws SQLException {
		int numberOfPrimaryKeys = 0;
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet primaryKeys = databaseMetaData.getPrimaryKeys(null, null, tblName);
		while (primaryKeys.next()) {
			numberOfPrimaryKeys++;
		}
		logger.log(Level.INFO, "Number of primary keys in table {0}: {1}", new Object[]{tblName, numberOfPrimaryKeys});
		return numberOfPrimaryKeys;
	}
	
	public static boolean isPrimaryKey(String colName, String tblName) throws SQLException {
		ArrayList<String> pKeys = getAllPrimaryKeyFromSpecifiedTable(tblName);
		for (int i = 0; i < pKeys.size(); i++) {
			String col = pKeys.get(i);
			String colCap = col.toUpperCase().charAt(0) + col.substring(1, col.length());
			if (!colCap.equals(colName)) {
				logger.log(Level.INFO, "Column {0} is not a primary key in table {1}", new Object[]{colName, tblName});
				return false;
			}
		}
		logger.log(Level.INFO, "Column {0} is a primary key in table {1}", new Object[]{colName, tblName});
		return true;
	}
	
	public static boolean isPrimaryKeyComposite(String tblName) throws SQLException {
		boolean isComposite = getNumberOfPrimaryKey(tblName) > 1;
		logger.log(Level.INFO, "Table {0} has a composite primary key: {1}", new Object[]{tblName, isComposite});
		return isComposite;
	}
	
	public static ArrayList<String> getAllForeignKeys(String tblName) throws SQLException {
		ArrayList<String> forKeys = new ArrayList<String>();
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet foreignKeys = databaseMetaData.getImportedKeys(null, null, tblName);
		while (foreignKeys.next()) {
			String fKeys = foreignKeys.getString("FKCOLUMN_NAME");
			String fkNameCapFirst = fKeys.toUpperCase().charAt(0) + fKeys.substring(1, fKeys.length());
			forKeys.add(fKeys);
			logger.log(Level.INFO, "Foreign key found: {0} in table {1}", new Object[]{fkNameCapFirst, tblName});
		}
		return forKeys;
	}
	
	public static boolean isForeignKey(String tblName, String colName) throws SQLException {
		ArrayList<String> forKeys = getAllForeignKeys(tblName);
		for (int i = 0; i < forKeys.size(); i++) {
			String fk = forKeys.get(i);
			String capFk = fk.toUpperCase().charAt(0) + fk.substring(1, fk.length());
			if (colName.equals(capFk)) {
				logger.log(Level.INFO, "Column {0} is a foreign key in table {1}", new Object[]{colName, tblName});
				return true;
			}
		}
		logger.log(Level.INFO, "Column {0} is not a foreign key in table {1}", new Object[]{colName, tblName});
		return false;
	}
	
	public static String getParentTable(String tblName, String col) throws SQLException {
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet foreignKeys = databaseMetaData.getImportedKeys(null, null, tblName);
		while (foreignKeys.next()) {
			boolean col_equals_pk_name = col.toLowerCase().equals(foreignKeys.getString("FKCOLUMN_NAME"));
			if (col_equals_pk_name || col.equals("")) {
				String parentTable = foreignKeys.getString("PKTABLE_NAME");
				logger.log(Level.INFO, "Parent table {0} found for table {1}", new Object[]{parentTable, tblName});
				return parentTable;
			}
		}
		logger.log(Level.WARNING, "No parent table found for table {0} and column {1}", new Object[]{tblName, col});
		return " ";
	}
	
	public static String getParentTable(String tblName) throws SQLException {
		return getParentTable(tblName, "");
	}
	
	public static List<String> getJoinPartners(String leftTblName, String rightTblName, String fk_column) throws SQLException {
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet foreignKeys = databaseMetaData.getImportedKeys(null, null, leftTblName);
		List<String> joins = new ArrayList<>();
		while (foreignKeys.next()) {
			boolean right_equals_PKTable_and_left_equals_FKTable = rightTblName.equals(foreignKeys.getString("PKTABLE_NAME")) && leftTblName.equals(foreignKeys.getString("FKTABLE_NAME")) && fk_column.toLowerCase().equals(foreignKeys.getString("FKCOLUMN_NAME"));
			boolean left_equals_PKTable_and_right_equals_FKTable = rightTblName.equals(foreignKeys.getString("FKTABLE_NAME")) && leftTblName.equals(foreignKeys.getString("PKTABLE_NAME")) && fk_column.toLowerCase().equals(foreignKeys.getString("FKCOLUMN_NAME"));
			if (right_equals_PKTable_and_left_equals_FKTable || left_equals_PKTable_and_right_equals_FKTable || rightTblName.isEmpty()) {
				String leftside = foreignKeys.getString("PKTABLE_NAME") + "." + foreignKeys.getString("PKCOLUMN_NAME");
				String rightside = foreignKeys.getString("FKTABLE_NAME") + "." + foreignKeys.getString("FKCOLUMN_NAME");
				if (leftside.equals(rightside)) {
					continue;
				}
				joins.add(leftside + " = " + rightside);
				logger.log(Level.INFO, "Join found between {0} and {1}", new Object[]{leftside, rightside});
			}
		}
		logger.log(Level.INFO, "Join partners for tables {0} and {1}: {2}", new Object[]{leftTblName, rightTblName, joins});
		return joins;
	}
	
	public static List<String> getJoinPartners(String tblName) throws SQLException {
		return getJoinPartners(tblName, "", "");
	}
	
	public static ArrayList<String> getParentTables(String tblName) throws SQLException {
		ArrayList<String> fKeys = new ArrayList<String>();
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet foreignKeys = databaseMetaData.getImportedKeys(null, null, tblName);
		while (foreignKeys.next()) {
			fKeys.add(foreignKeys.getString("PKTABLE_NAME"));
			logger.log(Level.INFO, "Parent table found: {0} for table {1}", new Object[]{foreignKeys.getString("PKTABLE_NAME"), tblName});
		}
		return fKeys;
	}
	
	public static ArrayList<String> getParentColumns(String tblName) throws SQLException {
		ArrayList<String> fKeys = new ArrayList<String>();
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet foreignKeys = databaseMetaData.getImportedKeys(null, null, tblName);
		while (foreignKeys.next()) {
			fKeys.add(foreignKeys.getString("PKCOLUMN_NAME"));
			logger.log(Level.INFO, "Parent column found: {0} for table {1}", new Object[]{foreignKeys.getString("PKCOLUMN_NAME"), tblName});
		}
		return fKeys;
	}
	
	public static String getChildTable(String tblName, String col) throws SQLException {
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet foreignKeys = databaseMetaData.getImportedKeys(null, null, tblName);
		while (foreignKeys.next()) {
			boolean col_equals_fk_name = col.toLowerCase().equals(foreignKeys.getString("FKCOLUMN_NAME"));
			if (col_equals_fk_name || col.equals("")) {
				String childTable = foreignKeys.getString("FKTABLE_NAME");
				logger.log(Level.INFO, "Child table found: {0} for table {1}", new Object[]{childTable, tblName});
				return childTable;
			}
		}
		logger.log(Level.WARNING, "No child table found for table {0} and column {1}", new Object[]{tblName, col});
		return " ";
	}
	
	public static String getChildTable(String tblName) throws SQLException {
		return getChildTable(tblName, "");
	}
	
	public static int getNumberOfForeignKeys(String tblName) throws SQLException {
		int nbForeignKeys = 0;
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet foreignKeys = databaseMetaData.getImportedKeys(null, null, tblName);
		while (foreignKeys.next()) {
			nbForeignKeys++;
		}
		logger.log(Level.INFO, "Number of foreign keys in table {0}: {1}", new Object[]{tblName, nbForeignKeys});
		return nbForeignKeys;
	}
	
	public static int getNumberOfColumns(String tblName) throws SQLException {
		int nbColumns = 0;
		DatabaseMetaData meta = cnx.getMetaData();
		ResultSet columns = meta.getColumns(null, null, tblName, null);
		while (columns.next()) {
			nbColumns++;
		}
		logger.log(Level.INFO, "Number of columns in table {0}: {1}", new Object[]{tblName, nbColumns});
		return nbColumns;
	}
	
	public static ArrayList<String> getAllColumns(String tblName) throws SQLException {
		ArrayList<String> allCols = new ArrayList<String>();
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet columns = databaseMetaData.getColumns(null, null, tblName, null);
		while (columns.next()) {
			allCols.add(columns.getString("COLUMN_NAME"));
			logger.log(Level.INFO, "Column found: {0} in table {1}", new Object[]{columns.getString("COLUMN_NAME"), tblName});
		}
		return allCols;
	}
	
	public static boolean isAssociativeTable(String tblName) throws SQLException {
		int num = 0;
		ArrayList<String> pKeys = getAllPrimaryKeyFromSpecifiedTable(tblName);
		if ((getNumberOfPrimaryKey(tblName) == 2) && (getNumberOfForeignKeys(tblName) == 2)) {
			for (int i = 0; i < pKeys.size(); i++) {
				String col = pKeys.get(i);
				String colCap = col.toUpperCase().charAt(0) + col.substring(1, col.length());
				if (isForeignKey(tblName, colCap)) {
					num++;
				}
			}
			logger.log(Level.INFO, "Associative table check for {0}, num = {1}", new Object[]{tblName, num});
			if (num == 2) {
				return true;
			}
		}
		logger.log(Level.INFO, "Table {0} is not an associative table", tblName);
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
			default:
				return "SET DEFAULT";
		}
	}
	
	public static String foreignKeyCrossReference(String parentTable, String foreignTable) throws SQLException {
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet rs = databaseMetaData.getCrossReference(null, null, parentTable, null, null, foreignTable);
		String deleteAction = null;
		while (rs.next()) {
			deleteAction = cascadeOptionToString(rs.getInt("DELETE_RULE"));
			logger.log(Level.INFO, "Foreign key cross-reference between {0} and {1}: delete action = {2}", new Object[]{parentTable, foreignTable, deleteAction});
		}
		return deleteAction;
	}
	
	public static OWLDatatype getOwlDataType(TableComponent attribute, OWLDataFactory factory) {
		OWLDatatype dataType = null;
		if (attribute.getDataType().equals("INT")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
        if (attribute.getDataType().equals("SMALLINT")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
        if (attribute.getDataType().equals("INT UNSIGNED")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
        if (attribute.getDataType().equals("ENUM")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
        if (attribute.getDataType().equals("TEXT")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
        if (attribute.getDataType().equals("VARCHAR")) dataType = factory.getRDFPlainLiteral();
        if (attribute.getDataType().equals("varchar")) dataType = factory.getRDFPlainLiteral();
        if (attribute.getDataType().equals("DATE")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DATE_TIME.getIRI());
        if (attribute.getDataType().equals("DATETIME")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DATE_TIME.getIRI());
        if (attribute.getDataType().equals("DECIMAL")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DECIMAL.getIRI());
        if (attribute.getDataType().equals("FLOAT")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_FLOAT.getIRI());
        if (attribute.getDataType().equals("DOUBLE")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_DOUBLE.getIRI());
        if (attribute.getDataType().equals("CHAR")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
        if (attribute.getDataType().equals("SMALLINT UNSIGNED")) dataType = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
        return dataType;
	}


	
	public static void main(String args[]) throws SQLException, OWLOntologyCreationException, OWLOntologyStorageException {
			cnx = MyConnection.getConnections(args[0]);
			ArrayList<TableComponent> cp;
	        ArrayList<TableComponent> cp1;
	        ArrayList<String> pKeys;
	        Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
	        OWLDatatype dataType = null;
	        String prefix = cnx.getCatalog();
	        Logger rootLogger = Logger.getLogger("");
	        Handler[] handlers = rootLogger.getHandlers();
	        for (Handler handler : handlers) {
	            rootLogger.removeHandler(handler);
	        }

	        // Create a new console handler with a custom formatter
	        ConsoleHandler consoleHandler = new ConsoleHandler();
	        consoleHandler.setLevel(Level.ALL);
	        consoleHandler.setFormatter(new Formatter() {
	            @Override
	            public String format(LogRecord record) {
	                return String.format("%s: %s%n", record.getLevel(), MessageFormat.format(record.getMessage(), record.getParameters()));
	            }
	        });

	        rootLogger.addHandler(consoleHandler);


	        logger.info("Catalog prefix: " + prefix);

	        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	        IRI ontologyIRI = IRI.create("http://java-ws.com/ontologie/myFamily.owl");
	        logger.info("Creating ontology with IRI: " + ontologyIRI);

	        OWLDataFactory factory = manager.getOWLDataFactory();
	        OWLOntology ontology = manager.createOntology(ontologyIRI);
	        logger.info("Ontology created successfully.");

	        D2RQMapping mapping = new D2RQMapping();
	        logger.info("Initialized D2RQMapping object.");

	        ArrayList<String> arrTables = getTables();
	        logger.info("Retrieved tables: " + arrTables);

	        for (int i = 0; i < arrTables.size(); i++) {
	        	logger.info("Processing table: " + arrTables.get(i));

	            boolean isNMTable = isPrimaryKeyComposite(arrTables.get(i)) && isAssociativeTable(arrTables.get(i)) &&
	                    ((getNumberOfPrimaryKey(arrTables.get(i)) == 2) && (getNumberOfForeignKeys(arrTables.get(i)) == 2) &&
	                            getNumberOfColumns(arrTables.get(i)) == 2);
	            if (isNMTable) {
	            	logger.info("Skipping N:M associative table: " + arrTables.get(i));
	                continue;
	            }

	            List<String> pks = listAllPrimaryKeysOfSpecifiedTable(arrTables.get(i));
	            logger.info("Primary keys for table " + arrTables.get(i) + ": " + pks);

	            String id = "";
	            for (int j = 0; j < pks.size(); j++) {
	                id = id + "@@" + arrTables.get(i) + "." + pks.get(j) + "@@" + "/";
	            }
	            logger.info("Generated ID for ClassMap: " + id);

	            D2RQClassMap classmap = new D2RQClassMap(id, arrTables.get(i), "ClassMap_" + arrTables.get(i), prefix);
	            logger.info("Created D2RQClassMap for table " + arrTables.get(i));

	            mapping.addClass(arrTables.get(i), classmap);
	            logger.info("Added ClassMap to mapping for table: " + arrTables.get(i));
	        }
		// für jede Tabelle
	        for (int i = 0; i < arrTables.size(); i++) {
	            logger.info("Processing table: " + arrTables.get(i));
	            logger.info("Is Associative - " + arrTables.get(i) + " : " + isAssociativeTable(arrTables.get(i)));
	            
	            // creates concept for each table -> Wir müssen hier also ein classmap erstellen: Was ist die URI? PK ist die URI.
	            OWLClass person = factory.getOWLClass(IRI.create(ontologyIRI + "#" + arrTables.get(i)));
	            
	            OWLDeclarationAxiom personDeclarationAxiom = factory.getOWLDeclarationAxiom((OWLEntity) person);
	            
	            cp = getColumnsOfSpecifiedTable(arrTables.get(i));
	            logger.info("Number of columns in table " + arrTables.get(i) + ": " + cp.size());

	            // für jede spalte
	            for (int j = 0; j < cp.size(); j++) {
	                logger.info("Processing column: " + cp.get(j).getColName());

	                // SCENARIO 1
	                // wenns kein PK ist, die spalte null sein kann und sie nicht unique ist
	                // -> DatatypeProperty + MinCardinality = 1
	                if ((!isPrimaryKey(cp.get(j).getColName(), arrTables.get(i))) &&
	                    ((cp.get(j).getNullable().equals("YES"))) && (!isUnique(arrTables.get(i), cp.get(j).getColName()))) {
	                    
	                    logger.info("Scenario 1: Column is not a primary key, is nullable, and is not unique");
	                    OWLDataProperty dataproperty = factory.getOWLDataProperty(IRI.create(ontologyIRI + "#has" + cp.get(j).getColName()));
	                    
	                    dataType = getOwlDataType(cp.get(j), factory);
	                    
	                    OWLDataPropertyRangeAxiom range = factory.getOWLDataPropertyRangeAxiom(dataproperty, dataType);
	                    OWLDataPropertyDomainAxiom domain = factory.getOWLDataPropertyDomainAxiom(dataproperty, person);
	                    D2RQDataProperty d2rq_range = new D2RQDataProperty("has" + cp.get(j).getColName(), mapping.getClassByKey(arrTables.get(i)), dataType.toString(), arrTables.get(i), cp.get(j).getColName());
	                    mapping.addDataProperty(d2rq_range.getProperty() + "_" + d2rq_range.getColumn(), d2rq_range);
	                    axioms.add(range);
	                    axioms.add(domain);
	                    logger.info("Created D2RQDataProperty: Property=" + d2rq_range.getProperty() + ", Domain=" + d2rq_range.getDomain() + ", Type=" + d2rq_range.getType() + ", Column=" + d2rq_range.getColumn());
	                }
	                
	                // SCENARIO 2
	                // Attribute + NOT NULL
	                // -> DatatypeProperty + MinCardinality = 1
	                if ((cp.get(j).getNullable().equals("NO")) && (!isPrimaryKey(cp.get(j).getColName(), arrTables.get(i))) && (!isUnique(arrTables.get(i), cp.get(j).getColName()))) {
	                    
	                    logger.info("Scenario 2: Column is not nullable, not a primary key, and not unique");
	                    OWLDataProperty dataproperty = factory.getOWLDataProperty(IRI.create(ontologyIRI + "#has" + cp.get(j).getColName()));
	                    
	                    dataType = getOwlDataType(cp.get(j), factory);
	                    
	                    OWLDataPropertyRangeAxiom range = factory.getOWLDataPropertyRangeAxiom(dataproperty, dataType);
	                    OWLDataPropertyDomainAxiom domain = factory.getOWLDataPropertyDomainAxiom(dataproperty, person);
	                    D2RQDataProperty d2rq_range = new D2RQDataProperty("has" + cp.get(j).getColName(), mapping.getClassByKey(arrTables.get(i)), dataType.toString(), arrTables.get(i), cp.get(j).getColName());
	                    mapping.addDataProperty(d2rq_range.getProperty() + "_" + d2rq_range.getColumn(), d2rq_range);
	                    axioms.add(range);
	                    axioms.add(domain);
	                    logger.info("Created D2RQDataProperty: Property=" + d2rq_range.getProperty() + ", Domain=" + d2rq_range.getDomain() + ", Type=" + d2rq_range.getType() + ", Column=" + d2rq_range.getColumn());
	                    
	                    OWLDataMinCardinality minCardinality = factory.getOWLDataMinCardinality(1, dataproperty);
	                    OWLSubClassOfAxiom axiom = factory.getOWLSubClassOfAxiom(person, minCardinality);
	                    manager.addAxiom(ontology, axiom);
	                }
	                
	                // SCENARIO 3
	                // Attribute + Not FK + Unique
	                // -> DatatypeProperty + MaxCardinality = 1
	                if (isUnique(arrTables.get(i), cp.get(j).getColName()) && (cp.get(j).getNullable().equals("YES")) && (!isPrimaryKey(cp.get(j).getColName(), arrTables.get(i)))) {
	                    
	                    logger.info("Scenario 3: Column is unique, nullable, and not a primary key");
	                    OWLDataProperty dataproperty = factory.getOWLDataProperty(IRI.create(ontologyIRI + "#has" + cp.get(j).getColName()));
	                    
	                    dataType = getOwlDataType(cp.get(j), factory);
	                    
	                    OWLDataPropertyRangeAxiom range = factory.getOWLDataPropertyRangeAxiom(dataproperty, dataType);
	                    OWLDataPropertyDomainAxiom domain = factory.getOWLDataPropertyDomainAxiom(dataproperty, person);
	                    D2RQDataProperty d2rq_range = new D2RQDataProperty("has" + cp.get(j).getColName(), mapping.getClassByKey(arrTables.get(i)), dataType.toString(), arrTables.get(i), cp.get(j).getColName());
	                    mapping.addDataProperty(d2rq_range.getProperty() + "_" + d2rq_range.getColumn(), d2rq_range);
	                    axioms.add(range);
	                    axioms.add(domain);
	                    logger.info("Created D2RQDataProperty: Property=" + d2rq_range.getProperty() + ", Domain=" + d2rq_range.getDomain() + ", Type=" + d2rq_range.getType() + ", Column=" + d2rq_range.getColumn());
	                    
	                    OWLDataMaxCardinality maxCardinality = factory.getOWLDataMaxCardinality(1, dataproperty);
	                    OWLSubClassOfAxiom axiom = factory.getOWLSubClassOfAxiom(person, maxCardinality);
	                    manager.addAxiom(ontology, axiom);
	                }
			
			
			
	             // SCENARIO 4
	             // PK is not Composite, but PK
	             // -> Datatype Property + ExactCardinality = 1
	             if(!isPrimaryKeyComposite(arrTables.get(i))) {
	                 if(isPrimaryKey(cp.get(j).getColName(), arrTables.get(i))) {
	                     logger.info("Processing Primary Key for table: " + arrTables.get(i) + ", column: " + cp.get(j).getColName() + ", data type: " + cp.get(j).getDataType() + ". Condition: PK is not Composite and is a Primary Key.");
	                     
	                     OWLDataProperty dataproperty = factory.getOWLDataProperty(IRI.create(ontologyIRI+"#has"+cp.get(j).getColName()));
	                     dataType = getOwlDataType(cp.get(j), factory);
	                     logger.info("Determined data type: " + dataType);
	                     
	                     OWLDataPropertyRangeAxiom range = factory.getOWLDataPropertyRangeAxiom(dataproperty,dataType);
	                     OWLDataPropertyDomainAxiom domain = factory.getOWLDataPropertyDomainAxiom(dataproperty, person);
	                     axioms.add(range);
	                     axioms.add(domain);

	                     OWLDataCardinalityRestriction card = factory.getOWLDataExactCardinality(1, dataproperty);
	                     OWLSubClassOfAxiom axiom= factory.getOWLSubClassOfAxiom(person, card);

	                     D2RQDataProperty d2rq_range = new D2RQDataProperty("has"+cp.get(j).getColName(), mapping.getClassByKey(arrTables.get(i)), dataType.toString(), arrTables.get(i), cp.get(j).getColName());
	                     mapping.addDataProperty(d2rq_range.getProperty() + "_" + d2rq_range.getColumn(), d2rq_range);
	                     logger.info("Created D2RQ data property mapping for column: " + cp.get(j).getColName() + ", property: " + d2rq_range.getProperty() + ", data type: " + d2rq_range.getType());

	                     manager.addAxiom(ontology, axiom);
	                     manager.addAxioms(ontology, axioms);
	                 }
	             }   

	             // This function maps each primary key to a "composite primary key class" in the ontology
	             // PK is Composite + no nm-Table + No FKs
	             if(isPrimaryKeyComposite(arrTables.get(i)) && (!isAssociativeTable(arrTables.get(i))) &&
	                 getNumberOfForeignKeys(arrTables.get(i))==0) {
	                 logger.info("Processing composite primary key for table: " + arrTables.get(i) + ". Condition: PK is Composite, not an nm-Table, and has no Foreign Keys.");

	                 Set<OWLAxiom> axioms1 = new HashSet<OWLAxiom>();
	                 Set<OWLAxiom> axioms2 = new HashSet<OWLAxiom>();
	                 ArrayList<String> p = listAllPrimaryKeysOfSpecifiedTable(arrTables.get(i));
	                 OWLClass cmpClass = factory.getOWLClass(IRI.create(ontologyIRI+"#pk_Class"+arrTables.get(i)));

	                 logger.info("Defined composite primary key class: " + cmpClass);

	                 OWLDeclarationAxiom cmpClassDeclarationAxiom = factory.getOWLDeclarationAxiom((OWLEntity) cmpClass);
	                 manager.addAxiom(ontology, cmpClassDeclarationAxiom);
	                 // für jeden primary key
	                 for(int k=0; k<p.size(); k++) {
	                     logger.info("Processing primary key column: " + p.get(k) + " for composite primary key class: " + cmpClass);
	                     OWLDataProperty dataproperty = factory.getOWLDataProperty(IRI.create(ontologyIRI+"#has"+p.get(k)));
	                     
	                     dataType = getOwlDataType(cp.get(k), factory);

	                     logger.info("Determined data type for composite primary key column: " + dataType);

	                     OWLDataPropertyRangeAxiom range1 = factory.getOWLDataPropertyRangeAxiom(dataproperty,dataType);
	                     OWLDataPropertyDomainAxiom domain1 = factory.getOWLDataPropertyDomainAxiom(dataproperty, cmpClass);
	                     axioms1.add(range1);
	                     axioms1.add(domain1);

	                     OWLObjectProperty objectProperty = factory.getOWLObjectProperty(IRI.create(ontologyIRI+"#hasPkClassOP"+arrTables.get(i)));
	                     logger.info("Defined object property for composite primary key: " + objectProperty);

	                     OWLObjectPropertyRangeAxiom range2 = factory.getOWLObjectPropertyRangeAxiom(objectProperty, cmpClass);
	                     OWLObjectPropertyDomainAxiom domain2 = factory.getOWLObjectPropertyDomainAxiom(objectProperty, person);
	                     OWLInverseFunctionalObjectPropertyAxiom invAxiom = factory.getOWLInverseFunctionalObjectPropertyAxiom(objectProperty);
	                     
	                     D2RQDataProperty d2rq_range = new D2RQDataProperty("has"+p.get(k), mapping.getClassByKey(arrTables.get(i)), dataType.toString(), arrTables.get(i), p.get(k));
	                     mapping.addDataProperty(d2rq_range.getProperty() + "_" + d2rq_range.getColumn(), d2rq_range);
	                     logger.info("Created D2RQ data property mapping for column: " + cp.get(j).getColName() + ", property: " + d2rq_range.getProperty() + ", data type: " + d2rq_range.getType());


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
	                     logger.info("Processing Foreign Key for table: " + arrTables.get(i) + ", column: " + cp.get(j).getColName() + ". Condition: FK is not part of a composite key, not a primary key, and is a Foreign Key.");
	                     Set<OWLAxiom> axiomsA = new HashSet<OWLAxiom>();
	                     String parentTable = getParentTable(arrTables.get(i), cp.get(j).getColName());
	                     String childTable = getChildTable(arrTables.get(i), cp.get(j).getColName());
	                     OWLClass parentClass = factory.getOWLClass(IRI.create(ontologyIRI+"#"+parentTable));
	                     OWLClass childClass = factory.getOWLClass(IRI.create(ontologyIRI+"#"+childTable));
	                     logger.info("Defined parent class: " + parentClass + " and child class: " + childClass);

	                     OWLObjectProperty objectProperty = factory.getOWLObjectProperty(IRI.create(ontologyIRI+"#has"+cp.get(j).getColName()));
	                     OWLObjectPropertyDomainAxiom domain = factory.getOWLObjectPropertyDomainAxiom(objectProperty, childClass);
	                     OWLObjectPropertyRangeAxiom range = factory.getOWLObjectPropertyRangeAxiom(objectProperty, parentClass);
	                     	
	                     D2RQClassMap parentMap = mapping.getClassByKey(parentTable); // Es kann passieren, dass es diese Tabelle noch nicht gibt!
	                     String join = getJoinPartners(childTable, parentTable, cp.get(j).getColName()).get(0); //TODO kann das hier nur einen join geben?
	                     D2RQClassMap childMap = mapping.getClassByKey(childTable); // Es kann passieren, dass es diese Tabelle noch nicht gibt!
	                     List<String> joins = new ArrayList<>();
	                     joins.add(join);
	                     String property = "has"+cp.get(j).getColName();
	                     D2RQObjectProperty objectproperty = new D2RQObjectProperty(property, childMap, parentMap, joins);
	                     mapping.addObjectProperty(property + "_" + getParentTable(arrTables.get(i)) + "_" + getChildTable(arrTables.get(i)), objectproperty);

	                     logger.info("Added object property mapping for Foreign Key: " + property + ". Mapping details: property: " + objectproperty.getProperty() + ", domain: " + objectproperty.getDomain() + ", range: " + objectproperty.getRange() + ", joins: " + joins);

	                     axiomsA.add(domain);
	                     axiomsA.add(range);
	                     manager.addAxioms(ontology, axiomsA);
	                 }
	             }
			

	           //Scenario2 pour FK: FK is part of primary key
	          // CompositePK + Zahl der PKs übersteigt FKs
	          if (isPrimaryKeyComposite(arrTables.get(i)) && (getNumberOfPrimaryKey(arrTables.get(i)) > getNumberOfForeignKeys(arrTables.get(i)))) {
	              logger.info("Scenario2 pour FK: FK is part of primary key");

	              Set<OWLAxiom> axiomsB = new HashSet<OWLAxiom>();
	              ArrayList<String> p = listAllPrimaryKeysOfSpecifiedTable(arrTables.get(i));
	              for (int k = 0; k < p.size(); k++) {
	                  // für jede Spalte die teil des PKs ist, wird geschaut, ob die Spalte auch FK ist
	            	  String col_name = cp.get(j).getColName();
	                  if (isForeignKey(arrTables.get(i), col_name)) {
	                      logger.info("Foreign key is part of the primary key. Processing relationship between parent and child tables.");
	                      String parentTable = getParentTable(arrTables.get(i), col_name);
	                      String childTable = getChildTable(arrTables.get(i), col_name);
	                      OWLClass parentClass = factory.getOWLClass(IRI.create(ontologyIRI + "#" + parentTable));
	                      OWLClass childClass = factory.getOWLClass(IRI.create(ontologyIRI + "#" + childTable));
	                      logger.info("Processing table: " + arrTables.get(i) + ". Parent table: " + parentTable + ". Child table: " + childTable);

	                      OWLObjectProperty objectProperty = factory.getOWLObjectProperty(IRI.create(ontologyIRI + "#has" + col_name));
	                      OWLObjectPropertyDomainAxiom domain = factory.getOWLObjectPropertyDomainAxiom(objectProperty, childClass);
	                      OWLObjectPropertyRangeAxiom range = factory.getOWLObjectPropertyRangeAxiom(objectProperty, parentClass);
	                      OWLFunctionalObjectPropertyAxiom axiom = factory.getOWLFunctionalObjectPropertyAxiom(objectProperty);

	                      D2RQClassMap parentMap = mapping.getClassByKey(parentTable); // Es kann passieren, dass es diese Tabelle noch nicht gibt!
	                      String join = getJoinPartners(childTable, parentTable, col_name).get(0); //TODO Kann es hier nur einen join partner geben???
	                      D2RQClassMap childMap = mapping.getClassByKey(childTable); // Es kann passieren, dass es diese Tabelle noch nicht gibt!
	                      List<String> joins = new ArrayList<>();
	                      joins.add(join);
	                      String property = "has" + col_name;
	                      D2RQObjectProperty objectproperty = new D2RQObjectProperty(property, childMap, parentMap, joins);
	                      mapping.addObjectProperty(property + "_" + parentTable + "_" + childTable, objectproperty);

	                      axiomsB.add(range);
	                      axiomsB.add(domain);
	                      manager.addAxioms(ontology, axiomsB);
	                      manager.addAxiom(ontology, axiom);
	                  } else {
	                      OWLClass cl = factory.getOWLClass(IRI.create(ontologyIRI + "#" + arrTables.get(i)));
	                      OWLDeclarationAxiom clDeclarationAxiom = factory.getOWLDeclarationAxiom((OWLEntity) person);
	                      logger.info("Column " + col_name + " is not a foreign key. Processing as data property.");

	                      OWLDataProperty dataproperty = factory.getOWLDataProperty(IRI.create(ontologyIRI + "#has" + col_name));
	                      dataType = getOwlDataType(cp.get(j), factory);

	                      OWLDataPropertyRangeAxiom range = factory.getOWLDataPropertyRangeAxiom(dataproperty, dataType);
	                      OWLDataPropertyDomainAxiom domain = factory.getOWLDataPropertyDomainAxiom(dataproperty, cl);

	                      D2RQDataProperty d2rq_range = new D2RQDataProperty("has" + col_name, mapping.getClassByKey(arrTables.get(i)), dataType.toString(), arrTables.get(i), col_name);
	                      mapping.addDataProperty(d2rq_range.getProperty() + "_" + d2rq_range.getColumn(), d2rq_range);

	                      logger.info("Adding data property range: " + range + " and domain: " + domain);
	                      axioms.add(range);
	                      axioms.add(domain);

	                      OWLDataMinCardinality minCardinality = factory.getOWLDataMinCardinality(1, dataproperty);
	                      OWLSubClassOfAxiom axiom = factory.getOWLSubClassOfAxiom(person, minCardinality);
	                      manager.addAxiom(ontology, clDeclarationAxiom);
	                      manager.addAxiom(ontology, axiom);
	                  }
	              }
	          }

	          //Scenario3: Many-to-Many Relationship (primary key is the full foreign key)
	          //oben wird schon eine klasse erstellt, obwohl laut paper das nicht notwendig ist
	          // CompositePK & AssociativeTable & #PK == 2 & #FK == 2 & #Columns == 2
	          if (isPrimaryKeyComposite(arrTables.get(i)) && isAssociativeTable(arrTables.get(i)) && ((getNumberOfPrimaryKey(arrTables.get(i)) == 2) && (getNumberOfForeignKeys(arrTables.get(i)) == 2) && getNumberOfColumns(arrTables.get(i)) == 2)) {
	              Set<OWLAxiom> axiomsC = new HashSet<OWLAxiom>();
	              logger.info("Scenario3: Many-to-Many Relationship (primary key is the full foreign key)");

	              ArrayList<String> p = listAllPrimaryKeysOfSpecifiedTable(arrTables.get(i));
	              ArrayList<String> fk = getParentTables(arrTables.get(i));
	              ArrayList<String> fk_cols = getParentColumns(arrTables.get(i));
	              logger.info("Parent tables: " + fk + " and parent columns: " + fk_cols + " for associative table: " + arrTables.get(i));

	              // für jeden PK
	              for (int k = 0; k < p.size(); k++) {
	                  String col = p.get(k);
	                  String colCap = col.toUpperCase().charAt(0) + col.substring(1, col.length());
	                  if (isForeignKey(arrTables.get(i), colCap)) {
	                      logger.info("Many-to-Many Scenario: Two Object Properties will be created in this case");

	                      for (int l = 0; k < fk.size(); l++) {
	                          OWLClass parentClass = factory.getOWLClass(IRI.create(ontologyIRI + "#" + fk.get(l)));
	                          OWLClass childClass = factory.getOWLClass(IRI.create(ontologyIRI + "#" + fk.get(l + 1)));

	                          String col1 = fk.get(l); //left class
	                          String colCap1 = col1.toUpperCase().charAt(0) + col1.substring(1, col1.length());
	                          String col2 = fk.get(l + 1); //right class
	                          String colCap2 = col2.toUpperCase().charAt(0) + col2.substring(1, col2.length());

	                          logger.info("Creating object property for " + colCap1 + " and " + colCap2);
	                          OWLObjectProperty objectProperty = factory.getOWLObjectProperty(IRI.create(ontologyIRI + "#has" + colCap1 + colCap2));
	                          logger.info("Object Property: " + objectProperty.toString());
	                          OWLObjectPropertyDomainAxiom domain = factory.getOWLObjectPropertyDomainAxiom(objectProperty, parentClass);
	                          OWLObjectPropertyRangeAxiom range = factory.getOWLObjectPropertyRangeAxiom(objectProperty, childClass);

	                          D2RQClassMap parentMap = mapping.getClassByKey(fk.get(l)); 
	                          List<String> joins = getJoinPartners(arrTables.get(i));
	                          D2RQClassMap childMap = mapping.getClassByKey(fk.get(l + 1));
	                          String property = "has" + colCap1 + colCap2;
	                          String inverseProperty = "has" + colCap2 + colCap1;
	                          D2RQObjectProperty objectproperty = new D2RQObjectProperty(property, childMap, parentMap, joins, inverseProperty);
	                          mapping.addObjectProperty(property + "_" + getParentTable(arrTables.get(i)) + "_" + getChildTable(arrTables.get(i)), objectproperty);

	                          OWLObjectProperty objectProperty1 = factory.getOWLObjectProperty(IRI.create(ontologyIRI + "#has" + colCap2 + colCap1));
	                          OWLInverseObjectPropertiesAxiom invObjAxiom = factory.getOWLInverseObjectPropertiesAxiom(objectProperty1, objectProperty);

	                          axiomsC.add(range);
	                          axiomsC.add(domain);
	                          manager.addAxioms(ontology, axiomsC);
	                          manager.addAxiom(ontology, invObjAxiom);
	                          break;
	                      }
	                  }
	              }
	          }
	          // Scenario 4: Many-to-Many with attribute Relationship (primary key is the full foreign key)
	          // einziger unterschied ist, dass es mehr als zwei attribute gibt
	          if (isPrimaryKeyComposite(arrTables.get(i)) && isAssociativeTable(arrTables.get(i)) &&
	                  ((getNumberOfPrimaryKey(arrTables.get(i)) == 2) && (getNumberOfForeignKeys(arrTables.get(i)) == 2))) {
	              Set<OWLAxiom> axiomsD = new HashSet<OWLAxiom>();
	              ArrayList<String> p = listAllPrimaryKeysOfSpecifiedTable(arrTables.get(i));
	              ArrayList<String> cols = getAllColumns(arrTables.get(i));
	              ArrayList<TableComponent> cp2 = getColumnsOfSpecifiedTable(arrTables.get(i));

	              if (getNumberOfColumns(arrTables.get(i)) > 2) {
	                  logger.info("Scenario 4: Handling many-to-many with attribute relationship. Table: " + arrTables.get(i));

	                  OWLClass relationshipClass = factory.getOWLClass(IRI.create(ontologyIRI + "#" + arrTables.get(i)));

	                  for (int l = 0; l < cp2.size(); l++) {
	                      if (!isForeignKey(arrTables.get(i), cp2.get(l).getColName())) {
	                          logger.info("Processing column (not a foreign key): " + cp2.get(l).getColName());

	                          OWLDataProperty dataproperty = factory.getOWLDataProperty(IRI.create(ontologyIRI + "#has" + cp2.get(l).getColName()));
	                          
	                          dataType = getOwlDataType(cp2.get(l), factory);

	                          OWLDataPropertyRangeAxiom range = factory.getOWLDataPropertyRangeAxiom(dataproperty, dataType);
	                          OWLDataPropertyDomainAxiom domain = factory.getOWLDataPropertyDomainAxiom(dataproperty, relationshipClass);
	                          D2RQDataProperty d2rq_range = new D2RQDataProperty("has" + cp2.get(l).getColName(),
	                                  mapping.getClassByKey(arrTables.get(i)), dataType.toString(), arrTables.get(i), cp2.get(l).getColName());
	                          logger.info("Adding D2RQDataProperty: Property - " + d2rq_range.getProperty() + ", Domain - " + d2rq_range.getDomain() + ", Type - " + d2rq_range.getType() + ", Column - " + d2rq_range.getColumn());
	                          mapping.addDataProperty(d2rq_range.getProperty() + "_" + d2rq_range.getColumn(), d2rq_range);

	                          axiomsD.add(range);
	                          axiomsD.add(domain);

	                          manager.addAxioms(ontology, axiomsD);
	                      }

	                      if (isForeignKey(arrTables.get(i), cp2.get(l).getColName())) {
	                          logger.info("Processing foreign key column: " + cp2.get(l).getColName());

	                          OWLClass parentClass = factory.getOWLClass(IRI.create(ontologyIRI + "#" + getParentTable(arrTables.get(i))));
	                          OWLClass childClass = factory.getOWLClass(IRI.create(ontologyIRI + "#" + getChildTable(arrTables.get(i))));
	                          String property = "#has" + cp2.get(l).getColName();
	                          String inverseProperty = "#is" + cp2.get(l).getColName() + "Of";

	                          OWLObjectProperty objectProperty = factory.getOWLObjectProperty(IRI.create(ontologyIRI + property));
	                          OWLObjectPropertyDomainAxiom domain = factory.getOWLObjectPropertyDomainAxiom(objectProperty, parentClass);
	                          OWLObjectPropertyRangeAxiom range = factory.getOWLObjectPropertyRangeAxiom(objectProperty, childClass);
	                          OWLObjectCardinalityRestriction card = factory.getOWLObjectExactCardinality(1, objectProperty);
	                          OWLSubClassOfAxiom axiom = factory.getOWLSubClassOfAxiom(parentClass, card);
	                          OWLObjectProperty objectProperty1 = factory.getOWLObjectProperty(IRI.create(ontologyIRI + inverseProperty));
	                          OWLInverseObjectPropertiesAxiom invObjAxiom = factory.getOWLInverseObjectPropertiesAxiom(objectProperty1, objectProperty);

	                          D2RQClassMap parentMap = mapping.getClassByKey(getParentTable(arrTables.get(i), cp2.get(l).getColName()));
	                          D2RQClassMap childMap = mapping.getClassByKey(getChildTable(arrTables.get(i), cp2.get(l).getColName()));
	                          List<String> joins = getJoinPartners(getChildTable(arrTables.get(i), cp2.get(l).getColName()), getParentTable(arrTables.get(i), cp2.get(l).getColName()),cp2.get(l).getColName());
	                          D2RQObjectProperty objectproperty = new D2RQObjectProperty(property, childMap, parentMap, joins);
	                          logger.info("Adding D2RQObjectProperty: Property - " + objectproperty.getProperty() + ", Domain - " + objectproperty.getDomain() + ", Range - " + objectproperty.getRange() + ", Joins - " + objectproperty.getJoin());
	                          mapping.addObjectProperty(property + "_" + getParentTable(arrTables.get(i)) + "_" + getChildTable(arrTables.get(i)), objectproperty);

	                          axiomsD.add(range);
	                          axiomsD.add(domain);
	                          manager.addAxioms(ontology, axiomsD);
	                          manager.addAxiom(ontology, axiom);
	                          manager.addAxiom(ontology, invObjAxiom);
	                      }
	                  }
	              }
	          }

	          // Scenario 5: The foreign key is the same primary key (Inheritance)
	          if (!isPrimaryKeyComposite(arrTables.get(i))) {
	              if (isPrimaryKey(cp.get(j).getColName(), arrTables.get(i)) && isForeignKey(arrTables.get(i), cp.get(j).getColName())) {
	                  logger.info("Scenario 5: The foreign key is the same primary key (Inheritance). Table: " + arrTables.get(i));

	                  String parentTable = getParentTable(arrTables.get(i), cp.get(j).getColName());
	                  String childTable = getChildTable(arrTables.get(i), cp.get(j).getColName());
	                  List<String> joins = getJoinPartners(childTable, parentTable, cp.get(j).getColName());
	                  D2RQClassMap childD2RQClass = mapping.getClassByKey(childTable);
	                  D2RQClassMap parentD2RQClass = mapping.getClassByKey(parentTable);
	                  childD2RQClass.addSubClass(parentD2RQClass.getName());
	                  childD2RQClass.addJoin(joins);
	                  OWLClass parentClass = factory.getOWLClass(IRI.create(ontologyIRI + "#" + parentTable));
	                  OWLClass childClass = factory.getOWLClass(IRI.create(ontologyIRI + "#" + childTable));
	                  OWLSubClassOfAxiom childClassisKindOfParentClass = factory.getOWLSubClassOfAxiom(childClass, parentClass);

	                  manager.addAxiom(ontology, childClassisKindOfParentClass);
	              }
	          }

	          // Scenario 6: The child and parent table for the FK are the same (Symmetric)
	          if (isPrimaryKeyComposite(arrTables.get(i))) {
	              logger.info("Scenario 6: Handling symmetric child and parent table relationship. Table: " + arrTables.get(i));
	              if (isForeignKey(arrTables.get(i), cp.get(j).getColName()) &&
	                      getChildTable(arrTables.get(i)).equals(getParentTable(arrTables.get(i)))) {
	                  logger.info("Scenario 6: The child and parent table for the FK are the same (Symmetric). Table: " + arrTables.get(i));

	                  String property = "has" + cp.get(j).getColName();
	                  Set<OWLAxiom> axiomsE = new HashSet<OWLAxiom>();
	                  OWLClass parentClass = factory.getOWLClass(IRI.create(ontologyIRI + "#" + getParentTable(arrTables.get(i))));
	                  OWLClass childClass = factory.getOWLClass(IRI.create(ontologyIRI + "#" + getChildTable(arrTables.get(i))));

	                  D2RQClassMap parentMap = mapping.getClassByKey(getParentTable(arrTables.get(i), cp.get(j).getColName()));
	                  D2RQClassMap childMap = mapping.getClassByKey(getChildTable(arrTables.get(i), cp.get(j).getColName()));
	                  List<String> joins = getJoinPartners(getChildTable(arrTables.get(i), cp.get(j).getColName()), getParentTable(arrTables.get(i), cp.get(j).getColName()), cp.get(j).getColName());
	                  D2RQObjectProperty objectproperty = new D2RQObjectProperty(property, childMap, parentMap, joins, property);
	                  logger.info("Adding D2RQObjectProperty: Property - " + objectproperty.getProperty() + ", Domain - " + objectproperty.getDomain() + ", Range - " + objectproperty.getRange() + ", Joins - " + objectproperty.getJoin());
	                  mapping.addObjectProperty(property + "_" + getParentTable(arrTables.get(i)) + "_" + getChildTable(arrTables.get(i)), objectproperty);

	                  OWLObjectProperty objectProperty = factory.getOWLObjectProperty(IRI.create(ontologyIRI + property));
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

