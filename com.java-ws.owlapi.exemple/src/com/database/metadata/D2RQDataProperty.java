package com.database.metadata;

public class D2RQDataProperty {
    // Change fields to private
    private String property;
    private D2RQClassMap domain;
    private String type;
    private String column;

    // Default constructor
    public D2RQDataProperty() {
    }

    // Constructor with parameters
    public D2RQDataProperty(String property, D2RQClassMap domain, String type, String table, String column) {
        this.property = property;
        this.domain = domain;
        this.type = type;
        this.column = table + "." + column;
    }

    // Getter for property
    public String getProperty() {
        return property;
    }

    // Getter for domain
    public String getDomain() {
        return domain.getName();
    }

    // Getter for type
    public String getType() {
        return type;
    }

    // Getter for column
    public String getColumn() {
        return column;
    }
}
