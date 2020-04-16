package com.exemple.test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.xerces.util.SynchronizedSymbolTable;

import com.database.model.MyConnection;

public class MetadataExtrcation {
	
	
private static Connection cnx;
private static DatabaseMetaData databaseMetaData;
	
	static {
		 cnx = MyConnection.getConnections();
	}
	
	public static ArrayList<String> getTableInfo() throws SQLException
	{
		  databaseMetaData = cnx.getMetaData();
		  ArrayList<String> tblName;
			//Print TABLE_TYPE "TABLE"
		  tblName=new ArrayList<String>();
			ResultSet resultSet = databaseMetaData.getTables(null, null, null, new String[]{"TABLE"});
	
			System.out.println("Printing TABLE_TYPE \"TABLE\" ");
	
			System.out.println("----------------------------------");
	
			while(resultSet.next())
	
			{
				tblName.add(resultSet.getString("TABLE_NAME"));
			}
			return tblName ;
	}
	
	
	public static ArrayList<String> getAllForeignKeys(String tblName) throws SQLException {
		
		ArrayList<String> forKeys = new ArrayList<String>();
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet foreignKeys = databaseMetaData.getImportedKeys(null,null,tblName);
		
		while (foreignKeys.next()) {
			System.out.println(foreignKeys.getString("FK_NAME"));
			String fKeys = foreignKeys.getString("FKCOLUMN_NAME");
			forKeys.add(fKeys);
			
		}
		
		return forKeys;
	}
	
	public static String getNameOfForeignKey(String tblName) throws SQLException {
		String s=" ";
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet foreignKeys = databaseMetaData.getImportedKeys(null,null,tblName);
		while (foreignKeys.next()) {
			s= foreignKeys.getString("PKCOLUMN_NAME");
		}
		return s;
	}
	
	public static String getNameOfForeignPKey(String tblName) throws SQLException {
		String s=" ";
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet foreignKeys = databaseMetaData.getImportedKeys(null,null,tblName);
		while (foreignKeys.next()) {
			s= foreignKeys.getString("FK_NAME");
		}
		return s;
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
	
	public static String getParentTable(String tblName) throws SQLException {
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet foreignKeys = databaseMetaData.getImportedKeys(null,null,tblName);
		
		while (foreignKeys.next()) return foreignKeys.getString("PKTABLE_NAME");
		
		return " ";
	}
	
	public static String getChildTable(String tblName) throws SQLException {
		
		DatabaseMetaData databaseMetaData = cnx.getMetaData();
		ResultSet foreignKeys = databaseMetaData.getImportedKeys(null,null,tblName);
		while (foreignKeys.next()) return foreignKeys.getString("FKTABLE_NAME");
		return " ";
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
	
	public static boolean isPrimaryKey(String colName, String tblName) throws SQLException {
		 ArrayList<String> pKeys = getAllPrimaryKeyFromSpecifiedTable (tblName);
		 for(int i=0 ; i<pKeys.size();i++) {
			String col=pKeys.get(i); 
			 if(!col.equals(colName)) 
				 return false;
		 }
		return true ;
	}

	public static void getColumnInfo(String tblName) throws SQLException {
		
		databaseMetaData = cnx.getMetaData();
		
		ResultSet columns=databaseMetaData.getColumns(null, null,tblName, null);
		
		while(columns.next()) {
			String columnName = columns.getString("COLUMN_NAME");
			String columnType = columns.getString("TYPE_NAME");
			String columnsize = columns.getString("COLUMN_SIZE");
			String isNullable = columns.getString("IS_NULLABLE");
			String is_autoIncrment = columns.getString("IS_AUTOINCREMENT");
			
			if(!isForeignKey(tblName, columnName)&& !isPrimaryKey(columnName, tblName)) {
				if(isNullable.equals("YES")) {
					System.out.println("hi");
					Statement stmt = cnx.createStatement();
					String alterQuery = "ALTER TABLE "+tblName+" CHANGE COLUMN "+columnName+" "+tblName+"_"+columnName+" "+ columnType
							+"("+columnsize+")";
					System.out.println(alterQuery);
					  stmt.execute(alterQuery);
				}
				
				if(isNullable.equals("NO")) {
					System.out.println("hi");
					Statement stmt = cnx.createStatement();
					String alterQuery = "ALTER TABLE "+tblName+" CHANGE COLUMN "+columnName+" "+tblName+"_"+columnName+" "+ columnType
							+"("+columnsize+") NOT NULL";
					System.out.println(alterQuery);
					  stmt.execute(alterQuery);
				}
				
			}
			
			else if(isPrimaryKey(columnName, tblName)) {
				System.out.println(" ");
			}
			
			else {
				String s= getNameOfForeignKey(tblName);
				if(isNullable.equals("YES")) {
					System.out.println("hello");
					Statement stmt = cnx.createStatement();
					String alterQuery = " ALTER TABLE "+tblName+" DROP FOREIGN KEY "+s+" ; "+
								" ALTER TABLE "+tblName+ " CHANGE COLUMN "+columnName+" "+tblName+"_"+columnName+" "+columnType+" ; "+
								" ALTER TABLE "+tblName+" ADD CONSTRAINT "+s+" "+" FOREIGN KEY ("+tblName+"_"+columnName+")"+	
								" REFERENCES "+getParentTable(tblName)+"("+getNameOfForeignPKey(tblName)+") ON UPDATE CASCADE ;";
					stmt.execute(alterQuery);
				}
				
				if(isNullable.equals("NO")) {
					Statement stmt = cnx.createStatement();
					String alterQuery = " ALTER TABLE "+tblName+" DROP FOREIGN KEY "+s+" ; "+
							" ALTER TABLE "+tblName+ " CHANGE COLUMN "+columnName+" "+tblName+"_"+columnName+" "+columnType+" NOT NULL ; "+
							" ALTER TABLE "+tblName+" ADD CONSTRAINT "+s+" "+" FOREIGN KEY ("+tblName+"_"+columnName+")"+	
							" REFERENCES "+getParentTable(tblName)+"("+getNameOfForeignPKey(tblName)+") ON UPDATE CASCADE ;";
					
					System.out.println(alterQuery);
				      stmt.execute(alterQuery);
				}
			}	
		}
	}
	
	
	
	
	
	public static void main(String args[]) throws SQLException {
		
		 ArrayList<String> tblName = getTableInfo();
		 
		for(int i=0; i<tblName.size();i++) {
			getColumnInfo(tblName.get(i));
			 //getAllForeignKeys(tblName.get(i));
		}
	}
	
	
}


