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

package com.castsoftware.artemis.controllers.api;

import com.castsoftware.artemis.config.UserConfiguration;
import com.castsoftware.artemis.datasets.RegexNode;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;

import java.util.Iterator;
import java.util.List;

public class RegexNodeController {

  public static RegexNode createRegexNodeWithParent(
      Neo4jAL neo4jAL,
      String name,
      List<String> regexes,
      List<String> internalTypes,
      String framework,
      String category,
      Long parentId)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {
    RegexNode rn =
        RegexNode.createRegexNode(neo4jAL, name, regexes, internalTypes, framework, category);
    RegexNode.linkToParent(neo4jAL, rn.getId(), parentId);

    return rn;
  }

  /**
   * Link a node to a parent
   *
   * @see RegexNode#linkToParent(Neo4jAL, Long, Long)
   */
  public static Relationship linkToParent(Neo4jAL neo4jAL, Long idChild, Long idParent)
      throws Neo4jQueryException {
    return RegexNode.linkToParent(neo4jAL, idChild, idParent);
  }

  /**
   * Get all Regex nodes
   *
   * @see RegexNode#getAllNodes(Neo4jAL)
   */
  public static List<RegexNode> getAllRegexNode(Neo4jAL neo4jAL) throws Neo4jQueryException {
    return RegexNode.getAllNodes(neo4jAL);
  }

  /**
   * Remove a regex node by its id
   *
   * @see RegexNode#removeRegexNode(Neo4jAL, Long)
   */
  public static boolean removeRegexNodeById(Neo4jAL neo4jAL, Long idNode)
      throws Neo4jQueryException {
    return RegexNode.removeRegexNode(neo4jAL, idNode);
  }

  /**
   * Update a Regex node by its ID
   *
   * @param neo4jAL Neo4j Access Layer
   * @param id Id of the Node
   * @param name New Name
   * @param regexes New regex rules
   * @param internalTypes New internal types
   * @param framework New associated framework
   * @param category New Category
   * @return
   * @throws Neo4jQueryException
   * @throws Neo4jBadNodeFormatException
   */
  public static RegexNode updateById(
      Neo4jAL neo4jAL,
      Long id,
      String name,
      List<String> regexes,
      List<String> internalTypes,
      String framework,
      String category,
      Long parentId)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {
    RegexNode rn = getRegexNodeById(neo4jAL, id);
    if (rn == null || rn.getNode() == null) return null;
    // Create the new Regex node
    RegexNode newNode = createRegexNode(neo4jAL, name, regexes, internalTypes, framework, category);

    // Delete the old node
    rn.getNode().delete();

    if (parentId == -1) return newNode; // Ignore if parent is not set
    RegexNode.linkToParent(neo4jAL, newNode.getId(), parentId);

    // If parent fou,d
    return newNode;
  }

  /**
   * Get a specific Regex Node
   *
   * @see RegexNode#getRegexNodeById(Neo4jAL, Long)
   */
  public static RegexNode getRegexNodeById(Neo4jAL neo4jAL, Long idNode)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {
    return RegexNode.getRegexNodeById(neo4jAL, idNode);
  }

  /**
   * Create a Regex Node
   *
   * @see RegexNode#createRegexNode(Neo4jAL, String, List, List, String, String)
   */
  public static RegexNode createRegexNode(
      Neo4jAL neo4jAL,
      String name,
      List<String> regexes,
      List<String> internalTypes,
      String framework,
      String category) {
    return RegexNode.createRegexNode(neo4jAL, name, regexes, internalTypes, framework, category);
  }

  /**
   * Test a Regex Node and get the number of matching object
   *
   * @param neo4jAL Neo4j Access Layer
   * @param id Id of the regex Node
   * @return
   * @throws Neo4jQueryException
   * @throws Neo4jBadNodeFormatException
   */
  public static Long testRegexNode(Neo4jAL neo4jAL, Long id)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {
    neo4jAL.logInfo("Testing node : " + id);
    RegexNode rn = RegexNode.getRegexNodeById(neo4jAL, id);
    if (rn == null) return null;

    return testRegex(neo4jAL, rn);
  }

  /**
   * Test a regex expression and get the number of Result
   *
   * @param neo4jAL Neo4j Access Layer
   * @param rn Regex Node to launch
   * @return
   * @throws Neo4jQueryException
   */
  private static Long testRegex(Neo4jAL neo4jAL, RegexNode rn) throws Neo4jQueryException {
    if (rn.getRegexes().isEmpty()) return 0L;

    String req;
    if (rn.getInternalTypes().isEmpty()) {
      // Match all type
      req =
          String.format(
              "MATCH (o:Object) WHERE any( x IN %s WHERE o.FullName=~x ) RETURN COUNT(DISTINCT o) as count",
              arrayToList(rn.getRegexes()));
    } else {
      // Match specified Type
      req =
          String.format(
              "MATCH (o:Object) WHERE any( x IN %s WHERE o.FullName=~x ) WITH o WHERE o.InternalType in %s RETURN COUNT(DISTINCT o) as count",
              arrayToList(rn.getRegexes()), arrayToList(rn.getInternalTypes()));
    }

    Result res = neo4jAL.executeQuery(req);

    if (!res.hasNext()) return 0L;
    return (Long) res.next().get("count");
  }

  private static String arrayToList(List<String> array) {
    return "['" + String.join("','", array) + "']";
  }

  /**
   * Get the cypher regex of request of a regex
   *
   * @param neo4jAL Neo4j access layer
   * @param id Id if the regex node
   * @return
   * @throws Neo4jQueryException
   * @throws Neo4jBadNodeFormatException
   */
  public static String getRegexRequest(Neo4jAL neo4jAL, Long id)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {
    RegexNode rn = RegexNode.getRegexNodeById(neo4jAL, id);
    if (rn == null) return "";

    return getRequestRegex(neo4jAL, rn);
  }

  /**
   * Get te request related to the Regex Node
   *
   * @param rn Regex Node
   * @return
   */
  private static String getRequestRegex(Neo4jAL neo4jAL, RegexNode rn) {

    String demeterPrefix = UserConfiguration.get(neo4jAL, "demeter.prefix.group_level");
    String tagName = demeterPrefix + rn.getCategory();

    String clauseInternalType = "";
    if (!rn.getInternalTypes().isEmpty())
      clauseInternalType =
          String.format("AND o.InternalType in %s ", arrayToList(rn.getInternalTypes()));

    return String.format(
        "MATCH (obj:Object) WHERE any( x IN %1$s WHERE o.FullName=~x ) %2$s "
            + "SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN ['%3$s'] ELSE [ x IN obj.Tags WHERE NOT x CONTAINS '%4$s' ] + '%3$s' "
            + "END RETURN COUNT(DISTINCT obj) as count",
        arrayToList(rn.getRegexes()), clauseInternalType, tagName, demeterPrefix);
  }

  /**
   * Flag all the nodes from the regex
   *
   * @param neo4jAL Neo4j Access Layer
   * @return
   * @throws Neo4jQueryException
   */
  public static Long flagRegexNodes(Neo4jAL neo4jAL) throws Neo4jQueryException {
    List<RegexNode> toVisit = RegexNode.getRootRegexNodes(neo4jAL);

    Iterator<RegexNode> itr = toVisit.iterator();
    Long regexMatched = 0L;

    while (itr.hasNext()) {
      RegexNode r = itr.next();
      toVisit.addAll(r.getChildren()); // Add the children

      // Get regex and find node matching them
      regexMatched += flagObjects(neo4jAL, r);
      itr.remove(); // Remove the node
    }

    return regexMatched;
  }

  /**
   * Flag objects matching a regex node
   *
   * @param neo4jAL Neo4j Access Layer
   * @param rn Regex Node
   * @return
   * @throws Neo4jQueryException
   */
  private static Long flagObjects(Neo4jAL neo4jAL, RegexNode rn) throws Neo4jQueryException {
    if (rn.getRegexes().isEmpty()) return 0L;
    String req = getRequestRegex(neo4jAL, rn);

    Result res = neo4jAL.executeQuery(req);
    if (!res.hasNext()) return 0L;
    return (Long) res.next().get("count");
  }
}
