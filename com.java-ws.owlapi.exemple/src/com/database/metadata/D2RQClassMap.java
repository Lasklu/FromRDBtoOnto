package com.database.metadata;
import java.util.List;

public class D2RQClassMap {
    
    private String id;
    private String _class;
    private String name;
    private String prefix;
    private String subClassOf;
    private List<String> join;
    
    // Constructor with parameters
    public D2RQClassMap(String id, String _class, String name, String prefix) {
        this.id = id;
        this._class = _class;
        this.name = name;
        this.prefix = prefix;
    }
    
    public void addSubClass(String name) {
    	this.subClassOf = name;
    }
    
    public void addJoin(List<String> join) {
    	this.join = join;
    }
    
    public List<String> getJoin() {
    	return join;
    }
    
    public String getSubClassOf() {
    	return subClassOf;
    }

    // Getter for id
    public String getId() {
        return id;
    }

    // Setter for id
    public void setId(String id) {
        this.id = id;
    }

    // Getter for _class
    public String getClassField() {  // Using getClassField() because getClass() is reserved by Java
        return _class;
    }

    // Setter for _class
    public void setClassField(String _class) {
        this._class = _class;
    }

    // Getter for name
    public String getName() {
        return name;
    }

    // Setter for name
    public void setName(String name) {
        this.name = name;
    }

    // Getter for prefix
    public String getPrefix() {
        return prefix;
    }

    // Setter for prefix
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
