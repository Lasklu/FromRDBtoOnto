package com.exemple.test;

import java.sql.Connection;

import com.database.model.MyConnection;

public class EnumTestExample {
private static Connection cnx;
	
	static {
		 cnx = MyConnection.getConnections();
	}
	
	
	public static void main(String args[]) {
		
		
		
	}

}
