package com.castsoftware.artemis.datasets;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.util.List;
import java.util.Objects;

public class FrameworkNode {

    // Static Artemis Properties
    private static final String LABEL_PROPERTY = Configuration.get("artemis.frameworkNode.label");
    private static final String NAME_PROPERTY = Configuration.get("artemis.frameworkNode.name");
    private static final String DISCOVERY_DATE_PROPERTY = Configuration.get("artemis.frameworkNode.discoveryDate");
    private static final String LOCATION_PROPERTY = Configuration.get("artemis.frameworkNode.location");
    private static final String DESCRIPTION_PROPERTY = Configuration.get("artemis.frameworkNode.description");
    private static final String NUMBER_OF_DETECTION_PROPERTY = Configuration.get("artemis.frameworkNode.numberOfDetection");
    private static final String PERCENTAGE_OF_DETECTION_PROPERTY = Configuration.get("artemis.frameworkNode.percentageDetection");
    private static final String TYPE_PROPERTY = Configuration.get("artemis.frameworkNode.frameworkType");
    private static final String CONFIRMED_PROPERTY = Configuration.get("artemis.frameworkNode.confirmed");

    private static final String ERROR_PREFIX = "FRAMNx";

    // Neo4j Properties
    private Neo4jAL neo4jAL;
    private Node node;

    // Properties
    private String name;
    private String discoveryDate;
    private String location;
    private String description;
    private Long numberOfDetection;
    private Double percentageDetection;
    private FrameworkType frameworkType;
    private Boolean confirmed;


    // Getters and setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDiscoveryDate() {
        return discoveryDate;
    }

    public void setDiscoveryDate(String discoveryDate) {
        this.discoveryDate = discoveryDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getNumberOfDetection() {
        return numberOfDetection;
    }

    public void setNumberOfDetection(Long numberOfDetection) {
        this.numberOfDetection = numberOfDetection;
    }

    public Double getPercentageDetection() {
        return percentageDetection;
    }

    public void setPercentageDetection(Double percentageDetection) {
        this.percentageDetection = percentageDetection;
    }

    public FrameworkType getFrameworkType() {
        return frameworkType;
    }

    public void setFrameworkType(FrameworkType frameworkType) {
        this.frameworkType = frameworkType;
    }

    public void setNode(Node n) {
        this.node = n;
    }

    public Node getNode(){
        return this.node;
    }

    /**
     * Create a node based on the characteristics of the Framework
     * @return The node created
     * @throws Neo4jQueryException
     */
    public Node createNode() throws Neo4jQueryException {
        Label frameworkLabel = Label.label(LABEL_PROPERTY);
        Node n = neo4jAL.createNode(frameworkLabel);

        // Add properties
        n.setProperty(NAME_PROPERTY, getName());
        n.setProperty(DISCOVERY_DATE_PROPERTY, getDiscoveryDate());
        n.setProperty(LOCATION_PROPERTY, getDiscoveryDate());
        n.setProperty(LOCATION_PROPERTY, getLocation());
        n.setProperty(DESCRIPTION_PROPERTY, getDescription());
        n.setProperty(NUMBER_OF_DETECTION_PROPERTY, getNumberOfDetection());
        n.setProperty(PERCENTAGE_OF_DETECTION_PROPERTY, getPercentageDetection());
        n.setProperty(TYPE_PROPERTY, getFrameworkType().toString());

        setNode(n);
        return n;
    }

    /**
     * Create a FrameworkNode object from a node
     * @param neo4jAL Neo4j access layer
     * @param n Node to transform
     * @return the FrameworkNode corresponding to the database node
     */
    public static FrameworkNode fromNode(Neo4jAL neo4jAL, Node n) throws Neo4jBadNodeFormatException {
        Label frameworkLabel = Label.label(LABEL_PROPERTY);
        // Check if the node has the correctLabel
        if(!n.hasLabel(frameworkLabel))
            throw new Neo4jBadNodeFormatException("The node isn't labeled has a framework.", ERROR_PREFIX+"FRON1");

        try {
            String name = (String) n.getProperty(NAME_PROPERTY);
            String discoveryDate = (String) n.getProperty(DISCOVERY_DATE_PROPERTY);
            String location = (String) n.getProperty(LOCATION_PROPERTY);

            String description = "No description";
            if(n.hasProperty(DESCRIPTION_PROPERTY)) {
                description  = (String) n.getProperty(DESCRIPTION_PROPERTY);
            }
            Long numDetection;
            try {
                numDetection = (Long) n.getProperty(NUMBER_OF_DETECTION_PROPERTY);
            } catch (ClassCastException e) {
                Integer temp = (Integer) n.getProperty(NUMBER_OF_DETECTION_PROPERTY);
                numDetection = (Long) temp.longValue();
            }

            Double percentageDetection = (Double) n.getProperty(PERCENTAGE_OF_DETECTION_PROPERTY);

            String frameworkType = (String) n.getProperty(TYPE_PROPERTY);
            FrameworkType type = FrameworkType.getType(frameworkType);

            FrameworkNode fn = new FrameworkNode(neo4jAL, name, discoveryDate, location, description, numDetection, percentageDetection);
            fn.setFrameworkType(type);
            fn.setNode(n);

            return fn;
        } catch (Exception e) {
            String msg = String.format("The Framework node with id: %d is not in a correct format", n.getId());
            throw new Neo4jBadNodeFormatException(msg, e, ERROR_PREFIX+"FRON2");
        }
    }

    /**
     * Check if a Framework exists in the database, searching by its name.
     * @param neo4jAL Neo4j Access Layer
     * @param frameworkName Name of the framework
     * @return The Framework node if found, null otherwise
     * @throws Neo4jQueryException
     * @throws Neo4jBadNodeFormatException
     */
    public static FrameworkNode findFrameworkByName(Neo4jAL neo4jAL, String frameworkName) throws Neo4jQueryException, Neo4jBadNodeFormatException {
        String matchReq = String.format("MATCH (n:%s) WHERE n.%s='%s' RETURN n as node LIMIT 1;", LABEL_PROPERTY, NAME_PROPERTY, frameworkName);

        Result res = neo4jAL.executeQuery(matchReq);
        // Check if the query returned a correct result
        if(!res.hasNext()) {
            return null;
        }
        // Node was found, return corresponding Framework Node
        Node n = (Node) res.next().get("node");
        FrameworkNode fn = FrameworkNode.fromNode(neo4jAL, n);

        return fn;
    }

    /**
     * Increment the number of detection for this specific framework
     * @return the new number of detection
     */
    public Long incrementNumberDetection(){
        Long numDetect = 0L;
        if(this.node.hasProperty(NUMBER_OF_DETECTION_PROPERTY)) {
             numDetect = (Long) this.node.getProperty(NUMBER_OF_DETECTION_PROPERTY);
        }
        numDetect++;
        this.node.setProperty(NUMBER_OF_DETECTION_PROPERTY, numDetect);
        this.numberOfDetection = numDetect;
        return numDetect;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FrameworkNode that = (FrameworkNode) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, discoveryDate, location, description, numberOfDetection, percentageDetection);
    }

    public FrameworkNode(Neo4jAL neo4jAL, String name, String discoveryDate, String location, String description, Long numberOfDetection) {
        this.neo4jAL = neo4jAL;
        this.name = name;
        this.discoveryDate = discoveryDate;
        this.location = location;
        this.description = description;
        this.numberOfDetection = numberOfDetection;
        this.percentageDetection = 0.0;
    }
    public FrameworkNode(Neo4jAL neo4jAL, String name, String discoveryDate, String location, String description, Long numberOfDetection, Double percentageDetection) {
        this.neo4jAL = neo4jAL;
        this.name = name;
        this.discoveryDate = discoveryDate;
        this.location = location;
        this.description = description;
        this.numberOfDetection = numberOfDetection;
        this.percentageDetection = percentageDetection;
    }

}
