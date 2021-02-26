/*
 * Copyright (C) 2020  Hugo JOBY
 *
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty ofnMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNUnLesser General Public License v3 for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public v3 License along with this library; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.castsoftware.artemis.datasets;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.controllers.api.CategoryController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.database.Neo4jTypeManager;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.pythia.PythiaCom;
import org.json.JSONObject;
import org.neo4j.graphdb.*;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class FrameworkNode {

  // Static Artemis Properties
  private static final String LABEL_PROPERTY = Configuration.get("artemis.frameworkNode.label");
  private static final String NAME_PROPERTY = Configuration.get("artemis.frameworkNode.name");
  private static final String DISCOVERY_DATE_PROPERTY =
      Configuration.get("artemis.frameworkNode.discoveryDate");
  private static final String LOCATION_PROPERTY =
      Configuration.get("artemis.frameworkNode.location");
  private static final String DESCRIPTION_PROPERTY =
      Configuration.get("artemis.frameworkNode.description");
  private static final String NUMBER_OF_DETECTION_PROPERTY =
      Configuration.get("artemis.frameworkNode.numberOfDetection");
  private static final String PERCENTAGE_OF_DETECTION_PROPERTY =
      Configuration.get("artemis.frameworkNode.percentageOfDetection");
  private static final String TYPE_PROPERTY =
      Configuration.get("artemis.frameworkNode.frameworkType");
  private static final String CONFIRMED_PROPERTY =
      Configuration.get("artemis.frameworkNode.confirmed");
  private static final String CATEGORY_PROPERTY =
      Configuration.get("artemis.frameworkNode.category");
  private static final String DETECTION_DATA_PROPERTY =
          Configuration.get("artemis.frameworkNode.detection_data");
  private static final String INTERNAL_TYPE_PROPERTY =
      Configuration.get("artemis.frameworkNode.internal_type");
  private static final String USER_CREATED_PROPERTY =
      Configuration.get("artemis.frameworkNode.user_created");
  private static final String CREATION_DATE_PROPERTY =
      Configuration.get("artemis.frameworkNode.creation_date");
  private static final String MODIFIED_PROPERTY =
          Configuration.get("artemis.frameworkNode.modified_property");

  private static final String CATEGORY_RELATIONSHIP =
      Configuration.get("artemis.category.to.frameworkNode");

  private static final String ERROR_PREFIX = "FRAMNx";

  // Neo4j Properties
  private final Neo4jAL neo4jAL;
  private Node node;

  // Properties
  private String name;
  private String discoveryDate;
  private String location = "";
  private String description = "";
  private String category;
  private String detectionData= "";
  private List<String> internalTypes = new ArrayList<>();
  private Long numberOfDetection = 0L;
  private Double percentageOfDetection = 0.0;
  private FrameworkType frameworkType = FrameworkType.NOT_KNOWN;
  private Boolean userCreated = false;
  private Long creationDate = (new Timestamp(System.currentTimeMillis()).getTime());

  public FrameworkNode(
      Neo4jAL neo4jAL,
      String name,
      String discoveryDate,
      String location,
      String description,
      Long numberOfDetection) {
    this.neo4jAL = neo4jAL;
    this.name = name;
    this.discoveryDate = discoveryDate;
    this.location = location;
    this.description = description;
    this.numberOfDetection = numberOfDetection;
    this.percentageOfDetection = 0.0;
    this.category = CategoryController.getDefaultName(neo4jAL);
  }

  public FrameworkNode(
      Neo4jAL neo4jAL,
      String name,
      String discoveryDate,
      String location,
      String description,
      Long numberOfDetection,
      Double percentageDetection,
      Long creationDate) {
    this.neo4jAL = neo4jAL;
    this.name = name;
    this.discoveryDate = discoveryDate;
    this.location = location;
    this.description = description;
    this.numberOfDetection = numberOfDetection;
    this.percentageOfDetection = percentageDetection;
    this.creationDate = creationDate;
    this.category = CategoryController.getDefaultName(neo4jAL);
  }

  public static String getLabel() {
    return LABEL_PROPERTY;
  }

  public static String getNameProperty() {
    return NAME_PROPERTY;
  }

  public static String getCreationDateProperty() {
    return CREATION_DATE_PROPERTY;
  }

  public static String getNumberOfDetectionProperty() {
    return NUMBER_OF_DETECTION_PROPERTY;
  }

  public static String getConfirmedProperty() {
    return CONFIRMED_PROPERTY;
  }

  public static String getCategoryProperty() {
    return CATEGORY_PROPERTY;
  }

  public static String getDetectionDataProperty() { return DETECTION_DATA_PROPERTY; }

  public static String getUserCreatedProperty() {
    return USER_CREATED_PROPERTY;
  }

  /**
   * Check if a Framework exists in the database, searching by its name.
   *
   * @param neo4jAL Neo4j Access Layer
   * @param frameworkName Name of the framework
   * @return The Framework node if found, null otherwise
   * @throws Neo4jQueryException
   * @throws Neo4jBadNodeFormatException
   */
  public static FrameworkNode findFrameworkByName(Neo4jAL neo4jAL, String frameworkName)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {
    String matchReq =
        String.format(
            "MATCH (n:%s) WHERE n.%s=$frameworkName  RETURN n as node LIMIT 1;",
            LABEL_PROPERTY, NAME_PROPERTY);

    Map<String, Object> params = Map.of("frameworkName", frameworkName);
    Result res = neo4jAL.executeQuery(matchReq, params);
    // Check if the query returned a correct result
    if (!res.hasNext()) {
      return null;
    }
    // Node was found, return corresponding Framework Node
    Node n = (Node) res.next().get("node");

    return FrameworkNode.fromNode(neo4jAL, n);
  }

  /**
   * Create a FrameworkNode object from a Neo4j node
   *
   * @param neo4jAL Neo4j access layer
   * @param n Node to transform
   * @return the FrameworkNode corresponding to the database node
   */
  public static FrameworkNode fromNode(Neo4jAL neo4jAL, Node n) throws Neo4jBadNodeFormatException {
    Label frameworkLabel = Label.label(LABEL_PROPERTY);
    // Check if the node has the correctLabel
    if (!n.hasLabel(frameworkLabel))
      throw new Neo4jBadNodeFormatException(
          "The node isn't labeled has a framework.", ERROR_PREFIX + "FRON1");

    try {
      String name = (String) n.getProperty(NAME_PROPERTY);
      String discoveryDate = (String) n.getProperty(DISCOVERY_DATE_PROPERTY);

      List<String> internalType = Neo4jTypeManager.getAsStringList(n, INTERNAL_TYPE_PROPERTY);

      // Get or Set
      String location = "";
      if (!n.hasProperty(LOCATION_PROPERTY)) {
        n.setProperty(LOCATION_PROPERTY, "");
      } else {
        location = (String) n.getProperty(LOCATION_PROPERTY);
      }

      String description = "No description";
      if (n.hasProperty(DESCRIPTION_PROPERTY)) {
        description = (String) n.getProperty(DESCRIPTION_PROPERTY);
      } else {
        n.setProperty(DESCRIPTION_PROPERTY, "");
      }

      Long numDetection = Neo4jTypeManager.getAsLong(n, NUMBER_OF_DETECTION_PROPERTY);
      Double percentageDetection =
          Neo4jTypeManager.getAsDouble(n, PERCENTAGE_OF_DETECTION_PROPERTY);

      String frameworkType = (String) n.getProperty(TYPE_PROPERTY);
      FrameworkType type = FrameworkType.getType(frameworkType);

      // Categories
      String category = CategoryController.getDefaultName(neo4jAL);
      if (n.hasProperty(CATEGORY_PROPERTY)) {
        String temp = (String) n.getProperty(CATEGORY_PROPERTY);
        if (!temp.isBlank()) category = temp;
      }

      String detectionData = "";
      if(n.hasProperty(DETECTION_DATA_PROPERTY)) {
        detectionData = (String) n.getProperty(DETECTION_DATA_PROPERTY);
      }

      // User created
      Boolean userCreated = false;
      if (n.hasProperty(USER_CREATED_PROPERTY)) {
        try {
          userCreated = (Boolean) n.getProperty(USER_CREATED_PROPERTY);
        } catch (ClassCastException | NotFoundException ignored) {
          // Ignored
        }
      }

      // Assign current Date if the framework has no date
      Long timestamp = 0L;
      if (!n.hasProperty(CREATION_DATE_PROPERTY)) {
        timestamp = new Date().getTime();
        n.setProperty(CREATION_DATE_PROPERTY, timestamp);
      } else {
        try {
          timestamp = (Long) n.getProperty(CREATION_DATE_PROPERTY, timestamp);
        } catch (ClassCastException | NotFoundException ignored) {
          timestamp = new Date().getTime();
          n.setProperty(CREATION_DATE_PROPERTY, timestamp);
        }
      }

      FrameworkNode fn =
          new FrameworkNode(
              neo4jAL,
              name,
              discoveryDate,
              location,
              description,
              numDetection,
              percentageDetection,
              timestamp);

      fn.setDetectionData(detectionData);
      fn.setFrameworkType(type);
      fn.setInternalTypes(internalType);
      fn.setUserCreated(userCreated);

      try {
        fn.setCategory(category);
      } catch (Exception e) {
        neo4jAL.logError("Failed to change the category of the node", e);
      }

      fn.setNode(n);

      return fn;
    } catch (Exception e) {
      String msg =
          String.format("The Framework node with id: %d is not in a correct format", n.getId());
      neo4jAL.logError(msg, e);
      throw new Neo4jBadNodeFormatException(msg, e, ERROR_PREFIX + "FRON2");
    }
  }

  /**
   * Find a framework in the database using its name and internal type
   *
   * @param neo4jAL Neo4j access layer
   * @param objectName Name of the framework
   * @param internalType Internal type of the object
   * @return The Framework node
   * @throws Neo4jQueryException
   * @throws Neo4jBadNodeFormatException
   */
  public static FrameworkNode findFrameworkByNameAndType(
      Neo4jAL neo4jAL, String objectName, String internalType)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {
    String matchReq =
        String.format(
            "MATCH (n:%s) WHERE n.%s=$frameworkName AND $internalType in n.%s RETURN n as node LIMIT 1;",
            LABEL_PROPERTY, NAME_PROPERTY, INTERNAL_TYPE_PROPERTY);

    Map<String, Object> params = Map.of("frameworkName", objectName, "internalType", internalType);

    Result res = neo4jAL.executeQuery(matchReq, params);
    // Check if the query returned a correct result
    if (!res.hasNext()) {
      return null;
    }
    // Node was found, return corresponding Framework Node
    Node n = (Node) res.next().get("node");
    FrameworkNode fn = FrameworkNode.fromNode(neo4jAL, n);
    return fn;
  }

  /**
   * Update a framework in the database
   *
   * @param neo4jAL Neo4j Access Layer
   * @param frameworkName Name of the framework
   * @param fn New Framework Node
   * @return
   * @throws Neo4jQueryException
   * @throws Neo4jBadNodeFormatException
   */
  public static FrameworkNode updateFrameworkByName(
      Neo4jAL neo4jAL,
      String frameworkName,
      List<String> internalType,
      String category,
      FrameworkNode fn)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {
    deleteFrameworkByNameAndType(neo4jAL, frameworkName, internalType);
    fn.createNode();

    // Change the category
    CategoryNode cn = CategoryController.getOrCreateByName(neo4jAL, category);
    fn.setCategory(cn);

    return fn;
  }

  public static void deleteFrameworkByNameAndType(
      Neo4jAL neo4jAL, String frameworkName, List<String> internalTypes)
      throws Neo4jQueryException {

    String matchReq =
        String.format(
            "MATCH (n:%s) WHERE n.%s=$frameworkName AND n.%s=$internalTypes DETACH DELETE n ;",
            LABEL_PROPERTY, NAME_PROPERTY, INTERNAL_TYPE_PROPERTY);

    neo4jAL.logInfo(
        String.format(
            "Query : %s",
            matchReq
                .replace("$frameworkName", frameworkName)
                .replace("$internalType", String.join(", ", internalTypes))));

    Map<String, Object> params =
        Map.of("frameworkName", frameworkName, "internalTypes", internalTypes);
    neo4jAL.executeQuery(matchReq, params);
  }

  /**
   * Create a node based on the characteristics of the Framework
   *
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
    n.setProperty(PERCENTAGE_OF_DETECTION_PROPERTY, getPercentageOfDetection().floatValue());
    n.setProperty(TYPE_PROPERTY, getFrameworkType().toString());
    n.setProperty(INTERNAL_TYPE_PROPERTY, getInternalTypes().toArray(new String[0]));
    n.setProperty(USER_CREATED_PROPERTY, getUserCreated());
    n.setProperty(CREATION_DATE_PROPERTY, getCreationDate()); // Last modification

    setNode(n);
    return n;
  }

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

  public Double getPercentageOfDetection() {
    return percentageOfDetection;
  }

  public void setPercentageOfDetection(Double percentageOfDetection) {
    this.percentageOfDetection = percentageOfDetection;
  }

  public String getDetectionData() {
    return this.detectionData;
  }

  public FrameworkType getFrameworkType() {
    return frameworkType;
  }

  public void setFrameworkType(FrameworkType frameworkType) {
    this.frameworkType = frameworkType;
  }

  public void setDetectionData(String detectionData) {
    this.detectionData = detectionData;
  }

  public List<String> getInternalTypes() {
    return internalTypes;
  }

  public void setInternalTypes(List<String> internalTypes) {
    this.internalTypes = internalTypes;
  }

  public Boolean getUserCreated() {
    return userCreated;
  }

  public void setUserCreated(Boolean userCreated) {
    this.userCreated = userCreated;
  }

  public Long getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Long creationDate) {
    this.creationDate = creationDate;
  }

  /**
   * Get the whole list of framework
   *
   * @param neo4jAL Neo4j Access Layer
   * @return
   * @throws Neo4jQueryException
   */
  public static List<FrameworkNode> getAll(Neo4jAL neo4jAL) throws Neo4jQueryException {
    return neo4jAL.findNodes(Label.label(LABEL_PROPERTY)).stream()
        .map(
            x -> {
              try {
                return FrameworkNode.fromNode(neo4jAL, x);
              } catch (Neo4jBadNodeFormatException err) {
                neo4jAL.logError("Failed to retrieve framework", err);
                return null;
              }
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  /**
   * Update the internal types of the node
   *
   * @param listTypes
   */
  public void updateInternalTypes(List<String> listTypes) {
    this.internalTypes = listTypes;

    if (node == null) return;
    node.setProperty(getInternalTypeProperty(), listTypes.toArray(new String[0]));
  }

  public void flagAsModified() {
    if (node == null) return;
    node.setProperty(MODIFIED_PROPERTY, true );
  }

  public static String getInternalTypeProperty() {
    return INTERNAL_TYPE_PROPERTY;
  }

  public void updateDescription(String description) {
    this.description = description;

    if (node == null) return;
    node.setProperty(getDescriptionProperty(), description);
  }

  public void updateDetectionData(String detectionData) {
    this.detectionData = detectionData;

    if (node == null) return;
    node.setProperty(getDetectionDataProperty(), detectionData);
  }

  public static String getDescriptionProperty() {
    return DESCRIPTION_PROPERTY;
  }

  public void updateType(FrameworkType ft) {
    this.frameworkType = ft;

    if (node == null) return;
    node.setProperty(getTypeProperty(), ft.toString());
  }

  public static String getTypeProperty() {
    return TYPE_PROPERTY;
  }

  public void updateDetectionScore(Double score) {
    this.percentageOfDetection = score;

    if (node == null) return;
    node.setProperty(getPercentageOfDetectionProperty(), score);
  }

  public static String getPercentageOfDetectionProperty() {
    return PERCENTAGE_OF_DETECTION_PROPERTY;
  }

  public void updateLocation(String location) {
    this.location = location;

    if (node == null) return;
    node.setProperty(getLocationProperty(), location);
  }

  public static String getLocationProperty() {
    return LOCATION_PROPERTY;
  }

  public Node getNode() {
    return this.node;
  }

  public void setNode(Node n) {
    this.node = n;
  }

  /** Flag node as created by the user */
  public boolean flagUserCreated() {
    this.userCreated = true;
    if (node == null) return false;
    node.setProperty(USER_CREATED_PROPERTY, true);
    return true;
  }

  /** Delete the node from the database */
  public void delete() {
    if (this.node == null) return;
    this.node.delete();
  }

  /**
   * Increment the number of detection for this specific framework
   *
   * @return the new number of detection
   */
  public Long incrementNumberDetection() {
    Long numDetect = 0L;
    if (this.node.hasProperty(NUMBER_OF_DETECTION_PROPERTY)) {
      numDetect = (Long) this.node.getProperty(NUMBER_OF_DETECTION_PROPERTY);
    }
    numDetect++;
    this.node.setProperty(NUMBER_OF_DETECTION_PROPERTY, numDetect);
    this.numberOfDetection = numDetect;
    return numDetect;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        name, discoveryDate, location, description, numberOfDetection, percentageOfDetection);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FrameworkNode that = (FrameworkNode) o;
    return Objects.equals(name, that.name) && Objects.equals(internalTypes, that.internalTypes);
  }

  public JSONObject toJSON() {
    JSONObject o = new JSONObject();
    o.put("name", this.name);
    o.put("discoveryDate", this.discoveryDate);
    o.put("location", this.location);
    o.put("description", this.description);
    o.put("category", this.category);
    o.put("internalType", this.internalTypes);
    o.put("numberOfDetection", this.numberOfDetection);
    o.put("percentageOfDetection", this.percentageOfDetection);
    o.put("type", this.frameworkType.toString());
    o.put("userCreated", this.userCreated);
    return o;
  }


  /**
   * Get the category of the node, if no category node is connected, it will create a custom one
   *
   * @return
   */
  public String getCategory() {
    if (this.node == null) return null;
    Iterator<Relationship> itCat =
        this.node
            .getRelationships(Direction.INCOMING, RelationshipType.withName(CATEGORY_RELATIONSHIP))
            .iterator();
    if (itCat.hasNext()) {
      // Get the category node
      Node category = itCat.next().getStartNode();
      try {
        CategoryNode cn = null;
        cn = new CategoryNode(category);
        return cn.getName();
      } catch (Neo4jBadNodeFormatException e) {
        return CategoryController.getDefaultName(neo4jAL);
      }
    } else {
      try {
        if (category == null) category = "Externals";
        CategoryNode cn = CategoryController.getOrCreateByName(neo4jAL, this.category);
        cn.getNode()
            .createRelationshipTo(this.node, RelationshipType.withName(CATEGORY_RELATIONSHIP));
        return cn.getName();
      } catch (Neo4jQueryException | Neo4jBadNodeFormatException e) {
        return CategoryController.getDefaultName(neo4jAL); // Default value
      }
    }
  }

  public void setCategory(CategoryNode cn) {
    if (this.node == null || cn.getNode() == null) return;
    Iterator<Relationship> itCat =
        this.node
            .getRelationships(Direction.INCOMING, RelationshipType.withName(CATEGORY_RELATIONSHIP))
            .iterator();
    while (itCat.hasNext()) {
      itCat.next().delete();
    }

    cn.getNode().createRelationshipTo(this.node, RelationshipType.withName(CATEGORY_RELATIONSHIP));
  }

  public void setCategory(String category) {
    if (this.node == null || category.isBlank()) return;
    Iterator<Relationship> itCat =
        this.node
            .getRelationships(Direction.INCOMING, RelationshipType.withName(CATEGORY_RELATIONSHIP))
            .iterator();
    while (itCat.hasNext()) {
      itCat.next().delete();

      try {
        CategoryNode cn = CategoryController.getOrCreateByName(neo4jAL, category);
        cn.getNode()
            .createRelationshipTo(this.node, RelationshipType.withName(CATEGORY_RELATIONSHIP));
      } catch (Neo4jQueryException | Neo4jBadNodeFormatException e) {
        neo4jAL.logError("Failed to attach a new category to the node", e);
      }
    }
  }

  /**
   * Send this framework node to the Pythia repository
   */
  public void sendToPythia() {
    if (PythiaCom.isSet(neo4jAL)) {
      PythiaCom.getInstance(neo4jAL).asyncSendFramework(this);
    }
  }
}
