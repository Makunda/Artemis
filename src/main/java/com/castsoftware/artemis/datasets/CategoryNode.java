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

import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CategoryNode {

  private static final String LABEL = "ArtemisCategory";
  private static final String NAME_PROPERTY = "Name";
  private static final String ICON_URL_PROPERTY = "IconURL";

  private Node node;
  private String name;
  private String iconUrl;

  public CategoryNode(String name, String iconUrl) {
    this.name = name;
    this.iconUrl = iconUrl;
  }

  public CategoryNode(Node n) throws Neo4jBadNodeFormatException {
    assert (n != null) : "Cannot initialize a null node";
    this.node = n;

    try {
      this.name = (String) node.getProperty(NAME_PROPERTY);
      if (!node.hasProperty(ICON_URL_PROPERTY)) {
        node.setProperty(ICON_URL_PROPERTY, "");
      }
      this.iconUrl = (String) n.getProperty(ICON_URL_PROPERTY);
    } catch (Exception e) {
      throw new Neo4jBadNodeFormatException(
          String.format(
              "Category Node with id '%d' is not correctly formatted : %s",
              node.getId(), e.getLocalizedMessage()),
          "REGEXxCONS1");
    }
  }

  public static String getLabel() {
    return LABEL;
  }

  public static String getNameProperty() {
    return NAME_PROPERTY;
  }

  /**
   * Create the a category node
   *
   * @param neo4jAL Neo4j Access Lauer
   * @param name Name of the node to create
   * @param iconUrl URL of the icon
   * @return
   */
  public static CategoryNode createNode(Neo4jAL neo4jAL, String name, String iconUrl) {
    CategoryNode cn = new CategoryNode(name, iconUrl);
    Node node = neo4jAL.getTransaction().createNode(Label.label(LABEL));
    cn.initNode(node);
    return cn;
  }

  /**
   * Initialize the Node with it data
   *
   * @param n
   * @return
   */
  public Node initNode(Node n) {
    assert (n != null) : "Cannot initialize a null node";
    this.node = n;
    node.setProperty(NAME_PROPERTY, name);
    node.setProperty(ICON_URL_PROPERTY, iconUrl);
    return n;
  }

  /**
   * Get a Category node by its Id
   *
   * @param neo4jAL Neo4j Access Layer
   * @param idNode Id of the node
   * @return
   * @throws Neo4jQueryException
   */
  public static CategoryNode getNode(Neo4jAL neo4jAL, Long idNode)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {
    String req = String.format("MATCH (n:%1$s) WHERE ID(n)=$idNode RETURN n as node", LABEL);
    Map<String, Object> params = Map.of("idNode", idNode);

    Result res = neo4jAL.executeQuery(req, params);
    if (!res.hasNext()) return null;

    return new CategoryNode((Node) res.next().get("node"));
  }

  /**
   * Remove a category node using its Id
   *
   * @param neo4jAL
   * @param idNode
   * @return
   * @throws Neo4jQueryException
   */
  public static boolean removeNode(Neo4jAL neo4jAL, Long idNode) throws Neo4jQueryException {
    String req =
        String.format("MATCH (n:%1$s) WHERE ID(n)=$idNode DETACH DELETE n RETURN n as node", LABEL);
    Map<String, Object> params = Map.of("idNode", idNode);

    Result res = neo4jAL.executeQuery(req, params);
    return !res.hasNext();
  }

  /**
   * Get all the category nodes
   *
   * @param neo4jAL Neo4j Access Layer
   * @return
   * @throws Neo4jQueryException
   */
  public static List<CategoryNode> getAllNodes(Neo4jAL neo4jAL) throws Neo4jQueryException {
    String req = String.format("MATCH (n:%1$s) RETURN n as node", LABEL);
    Result res = neo4jAL.executeQuery(req);

    List<CategoryNode> categoryNodes = new ArrayList<>();
    while (res.hasNext()) {
      try {
        CategoryNode cn = new CategoryNode((Node) res.next().get("node"));
        categoryNodes.add(cn);
      } catch (Neo4jBadNodeFormatException e) {
        neo4jAL.logError("Failed to instantiate a Regex node", e);
      }
    }

    return categoryNodes;
  }

  /** Statics methods */
  public Node getNode() {
    return node;
  }

  public String getName() {
    return name;
  }

  public String getIconUrl() {
    return iconUrl;
  }

  /**
   * Get the Id of the node or null of the node doesn't exist
   *
   * @return
   */
  public Long getId() {
    if (node == null) return null;
    return node.getId();
  }
}
