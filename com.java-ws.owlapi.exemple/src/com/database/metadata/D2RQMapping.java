package com.database.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class D2RQMapping {
    // Use Map to store data
    private Map<String, D2RQClassMap> classes = new HashMap<>();
    private Map<String, D2RQObjectProperty> object_properties = new HashMap<>();
    private Map<String, D2RQDataProperty> data_properties = new HashMap<>();

    // Getters for Map
    public Map<String, D2RQClassMap> getClassesMap() {
        return classes;
    }

    public Map<String, D2RQObjectProperty> getObjectPropertiesMap() {
        return object_properties;
    }

    public Map<String, D2RQDataProperty> getDataPropertiesMap() {
        return data_properties;
    }

    // Get the lists (for JSON serialization)
    public List<D2RQClassMap> getClasses() {
        return new ArrayList<>(classes.values());
    }

    public List<D2RQObjectProperty> getObjectProperties() {
        return new ArrayList<>(object_properties.values());
    }

    public List<D2RQDataProperty> getDataProperties() {
        return new ArrayList<>(data_properties.values());
    }

    // Add methods with key-value pair
    public void addClass(String key, D2RQClassMap classMap) {
        this.classes.put(key, classMap);
    }

    public void addObjectProperty(String key, D2RQObjectProperty objectProperty) {
        this.object_properties.put(key, objectProperty);
    }

    public void addDataProperty(String key, D2RQDataProperty dataProperty) {
        this.data_properties.put(key, dataProperty);
    }

    // Functions to get a specific class/property by key
    public D2RQClassMap getClassByKey(String key) {
        return this.classes.get(key);
    }

    public D2RQObjectProperty getObjectPropertyByKey(String key) {
        return this.object_properties.get(key);
    }

    public D2RQDataProperty getDataPropertyByKey(String key) {
        return this.data_properties.get(key);
    }

    // Method to write the D2RQMapping object to a JSON file with the custom structure
    public void writeToJsonFile(String filePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Create root object
            ObjectNode rootNode = objectMapper.createObjectNode();

            // Add classes to the root JSON
            ArrayNode classesArray = objectMapper.createArrayNode();
            for (D2RQClassMap classMap : classes.values()) {
                classesArray.add(objectMapper.valueToTree(classMap));
            }
            rootNode.set("classes", classesArray);

            // Add data_properties to the root JSON
            ArrayNode dataPropertiesArray = objectMapper.createArrayNode();
            for (D2RQDataProperty dataProperty : data_properties.values()) {
                dataPropertiesArray.add(objectMapper.valueToTree(dataProperty));
            }
            rootNode.set("dataproperties", dataPropertiesArray);

            // Add object_properties to the root JSON
            ArrayNode objectPropertiesArray = objectMapper.createArrayNode();
            for (D2RQObjectProperty objectProperty : object_properties.values()) {
                objectPropertiesArray.add(objectMapper.valueToTree(objectProperty));
            }
            rootNode.set("objectproperties", objectPropertiesArray);

            // Write the custom JSON structure to a file
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), rootNode);
            System.out.println("Custom JSON file created: " + filePath);

        } catch (IOException e) {
            System.out.println("Error writing JSON to file: " + e.getMessage());
        }
    }
}
