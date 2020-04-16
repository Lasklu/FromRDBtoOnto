package com.database.bean;

import java.util.ArrayList;

public class TableComponent {
	
	private String colName;
	private String dataType;
	private boolean isprimaryKey;
	private String nullable;
	private ArrayList<String> primaryKey;
	private String isUnique;
	private String idxName;
	
	
	public String getIdxName() {
		return idxName;
	}

	public TableComponent() {
		
	}
	
	public TableComponent(String colName, String dataType) {
		
		this.colName = colName;
		this.dataType = dataType;
	}
	
	public TableComponent(String colName, String dataType,String nullable) {
		
		this.colName = colName;
		this.dataType = dataType;
		this.nullable=nullable;
	}
	
	public TableComponent (String isUnique) {
		//this.idxName= idxName;
		this.isUnique = isUnique;
	}


	public TableComponent(ArrayList<String> primaryKey) {
		this.primaryKey = primaryKey;
	}
	
	public String getColName() {
		return colName;
	}

	public String getDataType() {
		return dataType;
	}

	public boolean isIsprimaryKey() {
		return isprimaryKey;
	}

	public String getNullable() {
		return nullable;
	}

	public ArrayList<String> getPrimaryKey() {
		return primaryKey;
	}
	
	public String getIsUnique() {
		return isUnique;
	}
	

}
