package com.database.metadata;

import java.util.List;

public class D2RQObjectProperty {
    private String property;
    private D2RQClassMap domain;
    private D2RQClassMap range;
    private List<String> join;
    private String inverseOf;

    // Default constructor
    public D2RQObjectProperty() {
    }

    // Constructor without inverseOf
    public D2RQObjectProperty(String property, D2RQClassMap domain, D2RQClassMap range, List<String> join) {
        this.property = property;
        this.domain = domain;
        this.range = range;
        this.join = join;
    }

    // Constructor with inverseOf
    public D2RQObjectProperty(String property, D2RQClassMap domain, D2RQClassMap range, List<String> join, String inverseOf) {
        this.property = property;
        this.domain = domain;
        this.range = range;
        this.join = join;
        this.inverseOf = inverseOf;
    }

    // Getter for property
    public String getProperty() {
        return property;
    }

    // Getter for domain
    public String getDomain() {
        return domain.getName();
    }

    // Getter for range
    public String getRange() {
    	System.out.println(property);
        return range.getName();
    }

    // Getter for join
    public List<String> getJoin() {
        return join;
    }

    // Getter for inverseOf
    public String getInverseOf() {
        return inverseOf;
    }
}
