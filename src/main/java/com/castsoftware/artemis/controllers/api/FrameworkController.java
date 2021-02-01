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

import com.castsoftware.artemis.config.LanguageConfiguration;
import com.castsoftware.artemis.config.LanguageProp;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.util.*;

public class FrameworkController {

  /**
   * Find a framework using its name
   *
   * @param neo4jAL Neo4j Access Layer
   * @param name Name of the Framework to find
   * @return
   * @throws Neo4jQueryException
   * @throws Neo4jBadNodeFormatException
   */
  public static FrameworkNode findFrameworkByName(Neo4jAL neo4jAL, String name)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {

    return FrameworkNode.findFrameworkByName(neo4jAL, name);
  }

  /**
   * Find a framework using its name and its internal type
   *
   * @param neo4jAL Neo4j Access Layer
   * @param name Name of the Framework to find
   * @param internalType Internal type of the object
   * @return
   * @throws Neo4jQueryException
   * @throws Neo4jBadNodeFormatException
   */
  public static FrameworkNode findFrameworkByNameAndType(
      Neo4jAL neo4jAL, String name, String internalType)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {

    return FrameworkNode.findFrameworkByNameAndType(neo4jAL, name, internalType);
  }

  /**
   * Delete a framework in the database using its name and its internal type
   * @param neo4jAL Neo4j Access Layer
   * @param name Name of the Framework to find
   * @param internalType
   * @return
   * @throws Neo4jQueryException
   * @throws Neo4jBadNodeFormatException
   */
  public static Boolean deleteFrameworkByNameAndType(
          Neo4jAL neo4jAL, String name, String internalType)
          throws Neo4jQueryException, Neo4jBadNodeFormatException {

    FrameworkNode fn = FrameworkNode.findFrameworkByNameAndType(neo4jAL, name, internalType);
    if (fn != null) {
      fn.delete();
      return true;
    }

    return false;
  }

  /**
   * Create a Framework node in the Database
   *
   * @param neo4jAL Neo4j Access Layer
   * @param name Name of the Framework
   * @param discoveryDate Date of the discovery
   * @param location Location of the Framework ( repository, url, etc...)
   * @param description Description
   * @param type Framework category ( Framework, NotFramework, etc..)
   * @param category Category of the framework
   * @param internalType Internal type of the object detected
   * @return The node created
   * @throws Neo4jQueryException
   */
  public static FrameworkNode addFramework(
      Neo4jAL neo4jAL,
      String name,
      String discoveryDate,
      String location,
      String description,
      String type,
      String category,
      String internalType)
      throws Neo4jQueryException {

    FrameworkNode fn =
        new FrameworkNode(neo4jAL, name, discoveryDate, location, description, 0l, .0, new Date().getTime());
    fn.setInternalType(internalType);
    fn.setCategory(category);
    fn.setFrameworkType(FrameworkType.getType(type));
    fn.createNode();

    neo4jAL.logInfo(
        String.format("Framework with name %s has been inserted through API call", name));

    return fn;
  }

  /**
   * Update a Framework node in the Database
   *
   * @param neo4jAL Neo4j Access Layer
   * @param name Name of the Framework
   * @param discoveryDate Date of the discovery
   * @param location Location of the Framework ( repository, url, etc...)
   * @param description Description
   * @param type Framework category ( Framework, NotFramework, etc..)
   * @param category Category of the framework
   * @param internalType Internal type of the object detected
   * @param numberOfDetection Number of detection
   * @param percentageOfDetection Detection rate
   * @return The new node
   * @throws Neo4jQueryException
   * @throws Neo4jBadNodeFormatException
   */
  public static FrameworkNode updateFramework(
      Neo4jAL neo4jAL,
      String oldName,
      String oldInternalType,
      String name,
      String discoveryDate,
      String location,
      String description,
      String type,
      String category,
      Long numberOfDetection,
      Double percentageOfDetection,
      String internalType)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {

    FrameworkNode fn =
        new FrameworkNode(
            neo4jAL,
            name,
            discoveryDate,
            location,
            description,
            numberOfDetection,
            percentageOfDetection,
                new Date().getTime());
    fn.setInternalType(internalType);
    fn.setCategory(category);
    fn.setFrameworkType(FrameworkType.getType(type));

    return FrameworkNode.updateFrameworkByName(neo4jAL, oldName, oldInternalType, fn);
  }

  /**
   * Get the complete list of framework in the database (Not recommended, prefer the batch search)
   *
   * @param neo4jAL Neo4j Access Layer
   * @return
   */
  public static List<FrameworkNode> getAllFrameworks(Neo4jAL neo4jAL) throws Neo4jQueryException {
    return FrameworkNode.getAll(neo4jAL);
  }

  /**
   * Return a list of the Frameworks batched. The frameworks are sorted by name. To get the number
   * of framework in the database please see {@link #getNumFrameworks}
   *
   * @param startIndex Start index of the batch
   * @param neo4jAL Neo4j Access Layer
   * @return
   * @throws Neo4jQueryException
   */
  public static List<FrameworkNode> getBatchedFrameworks(
      Neo4jAL neo4jAL, Long startIndex, Long endIndex) throws Neo4jQueryException {
    Long limit = endIndex - startIndex;
    String request =
        String.format(
            "MATCH(o:%s) RETURN o as framework ORDER BY o.%s SKIP $toSkip LIMIT $limit;",
            FrameworkNode.getLabel(), FrameworkNode.getNameProperty(), startIndex, limit);
    Map<String, Object> params =
        Map.of(
            "toSkip", startIndex,
            "limit", limit);

    Result res = neo4jAL.executeQuery(request, params);

    List<FrameworkNode> frameworkNodeList = new ArrayList<>();
    Node n;
    while (res.hasNext()) {
      n = (Node) res.next().get("framework");
      try {
        frameworkNodeList.add(FrameworkNode.fromNode(neo4jAL, n));
      } catch (Neo4jBadNodeFormatException e) {
        neo4jAL.logError("Failed to retrieve a framework.", e);
      }
    }

    return frameworkNodeList;
  }

  /**
   * Return a list of the Frameworks with only the specified internal type batched.
   *
   * @param neo4jAL Neo4j access Layer
   * @param startIndex Start index of the batch
   * @param neo4jAL Neo4j Access Layer
   * @param internalType Internal type
   * @return
   * @throws Neo4jQueryException
   */
  public static List<FrameworkNode> getBatchedFrameworksByType(
      Neo4jAL neo4jAL, Long startIndex, Long endIndex, String internalType)
      throws Neo4jQueryException {
    Long limit = endIndex - startIndex;
    String request =
        String.format(
            "MATCH(o:%s) WHERE o.%s=$internalType RETURN o as framework ORDER BY o.%s SKIP $toSkip LIMIT $limit;",
            FrameworkNode.getLabel(),
            FrameworkNode.getInternalTypeProperty(),
            FrameworkNode.getNameProperty());

    Map<String, Object> params =
        Map.of(
            "internalType", internalType,
            "toSkip", startIndex,
            "limit", limit);
    Result res = neo4jAL.executeQuery(request, params);

    List<FrameworkNode> frameworkNodeList = new ArrayList<>();
    Node n;
    while (res.hasNext()) {
      n = (Node) res.next().get("framework");
      try {
        frameworkNodeList.add(FrameworkNode.fromNode(neo4jAL, n));
      } catch (Neo4jBadNodeFormatException e) {
        neo4jAL.logError("Failed to retrieve a framework.", e);
      }
    }

    return frameworkNodeList;
  }

  /**
   * Get the number of Frameworks in th database
   *
   * @param neo4jAL Neo4j Access Layer
   * @return
   * @throws Neo4jQueryException
   */
  public static Long getNumFrameworks(Neo4jAL neo4jAL) throws Neo4jQueryException {
    String request =
        String.format("MATCH(o:%s) RETURN COUNT(o) as count;", FrameworkNode.getLabel());
    Result res = neo4jAL.executeQuery(request);

    if (!res.hasNext()) {
      neo4jAL.logError("Failed to retrieve the number of Framework");
      return 0L;
    }

    return (Long) res.next().get("count");
  }

  /**
   * Get the number of Frameworks in th database
   *
   * @param neo4jAL Neo4j Access Layer
   * @return
   * @throws Neo4jQueryException
   */
  public static Long getNumFrameworksByInternalType(Neo4jAL neo4jAL, String internalType)
      throws Neo4jQueryException {
    String request =
        String.format(
            "MATCH(o:%s) WHERE o.%s=$internalType RETURN COUNT(o) as count;",
            FrameworkNode.getLabel(), FrameworkNode.getInternalTypeProperty(), internalType);
    Map<String, Object> param = Map.of("internalType", internalType);
    Result res = neo4jAL.executeQuery(request, param);

    if (!res.hasNext()) {
      neo4jAL.logError("Failed to retrieve the number of Framework");
      return 0L;
    }

    return (Long) res.next().get("count");
  }

  /**
   * Get the candidates for a specific language
   *
   * @param neo4jAL Neo4j access layer
   * @return
   */
  public static Long getNumCandidateByLanguage(
      Neo4jAL neo4jAL, String application, String language) throws Neo4jQueryException {
    String request =
        String.format(
            "MATCH(o:Object:`%s`) WHERE o.%s IN $listInternalType AND o.External=true RETURN COUNT(o) as count;",
            application, FrameworkNode.getInternalTypeProperty());
    LanguageConfiguration lc = LanguageConfiguration.getInstance();

    if (!lc.checkLanguageExistence(language)) return 0L;
    LanguageProp lp = lc.getLanguageProperties(language);

    Map<String, Object> params = Map.of("$listInternalType", lp.getObjectsInternalType());
    Result res = neo4jAL.executeQuery(request, params);

    if (!res.hasNext()) {
      neo4jAL.logError(
          String.format(
              "Failed to retrieve the candidates frameworks in application %s.", application));
      return 0L;
    }

    return (Long) res.next().get("count");
  }

  /**
   * Get the list of n framework where the name contains a specific string
   * @param neo4jAL Neo4j Access Layer
   * @param name Name to search
   * @param limit Limit of results
   * @return
   */
  public static List<FrameworkNode> findFrameworkNameContains(Neo4jAL neo4jAL, String name, Long limit) throws Neo4jQueryException {
    String request =
        String.format(
            "MATCH(o:%s) WHERE toLower(o.%2$s) CONTAINS toLower($toSearch) RETURN o as framework ORDER BY o.%2$s LIMIT $limit;",
            FrameworkNode.getLabel(), FrameworkNode.getNameProperty());

    Map<String, Object> params =
            Map.of(
                    "toSearch", name,
                    "limit", limit);
    Result res = neo4jAL.executeQuery(request, params);

    List<FrameworkNode> frameworkNodeList = new ArrayList<>();
    Node n;
    while (res.hasNext()) {
      n = (Node) res.next().get("framework");
      try {
        frameworkNodeList.add(FrameworkNode.fromNode(neo4jAL, n));
      } catch (Neo4jBadNodeFormatException e) {
        neo4jAL.logError("Failed to retrieve a framework.", e);
      }
    }

    return frameworkNodeList;
  }

  /**
   * Get the list of the internal type
   * @param neo4jAL Neo4j Access Layer
   * @return
   * @throws Neo4jQueryException
   */
  public static List<String> getFrameworkInternalTypes(Neo4jAL neo4jAL) throws Neo4jQueryException {
    String request =
            String.format(
                    "MATCH(o:Object) RETURN DISTINCT o.InternalType as internalType;",
                    FrameworkNode.getLabel(), FrameworkNode.getInternalTypeProperty());

    Result res = neo4jAL.executeQuery(request);

    List<String> internalType = new ArrayList<>();
    String internal;
    while (res.hasNext()) {
      internal = (String) res.next().get("internalType");
      internalType.add(internal);
    }

    // Clean the results
    internalType.removeAll(Collections.singleton(null));
    internalType.removeAll(Collections.singleton(""));

    return internalType;
  }

  public static List<FrameworkNode> getFrameworkOlderThan(Neo4jAL neo4jAL, Long limitTimestamp) throws Neo4jQueryException {
    String req = "MATCH (o:%s) WHERE EXISTS(o.%2$s) AND o.%2$s > $timestamp RETURN o as framework";
    Map<String, Object> params = Map.of("$timestamp", limitTimestamp);

    Node n;
    List<FrameworkNode> frameworkNodeList = new ArrayList<>();
    Result res = neo4jAL.executeQuery(req, params);
    while (res.hasNext()) {
      n = (Node) res.next().get("framework");
      try {
        frameworkNodeList.add(FrameworkNode.fromNode(neo4jAL, n));
      } catch (Neo4jBadNodeFormatException e) {
        neo4jAL.logError("Failed to retrieve a framework.", e);
      }
    }

    return frameworkNodeList;
  }
}
