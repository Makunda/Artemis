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

import com.castsoftware.artemis.config.NodeConfiguration;
import com.castsoftware.artemis.config.detection.LanguageConfiguration;
import com.castsoftware.artemis.config.detection.LanguageProp;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.database.Neo4jTypeManager;
import com.castsoftware.artemis.datasets.CategoryNode;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
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
   * Parse the list of framework compliant with the internal type and check if one of them match the name.
   * @param neo4jAL Neo4j Access Layer
   * @param objectName Name of the object
   * @param internalType Internal Type of the object
   * @return The matching framework node or null if no matches were found.
   * @throws Neo4jQueryException
   * @throws Neo4jBadNodeFormatException
   */
  public static FrameworkNode findMatchingFrameworkByType(
          Neo4jAL neo4jAL, String objectName, String internalType)
          throws Neo4jQueryException, Neo4jBadNodeFormatException {
    List<FrameworkNode> frameworkNodesList = FrameworkNode.findFrameworkByType(neo4jAL, internalType);

    FrameworkNode bestMatch = null;
    for (FrameworkNode fn : frameworkNodesList) {
      // Check if the pattern is matching
      neo4jAL.logInfo(String.format("Comparing (Name) %s to fn %s", objectName, fn.getPattern()));
      if(fn.isPatternMatching(objectName)) {
        if(bestMatch != null) {
          if(bestMatch.getPattern().length() < fn.getPattern().length()) {
            bestMatch = fn;
          }
        } else {
          bestMatch = fn;
        }
      }
    }

    return bestMatch;
  }


  /**
   * Delete a framework in the database using its name and its internal type
   *
   * @param neo4jAL Neo4j Access Layer
   * @param name Name of the Framework to find
   * @param internalTypes List of the internal type of the framework
   * @return
   * @throws Neo4jQueryException
   * @throws Neo4jBadNodeFormatException
   */
  public static void deleteFrameworkByNameAndType(
      Neo4jAL neo4jAL, String name, List<String> internalTypes)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {
    FrameworkNode.deleteFrameworkByNameAndType(neo4jAL, name, internalTypes);
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
      List<String> oldInternalType,
      String name,
      String pattern,
      Boolean isRegex,
      String discoveryDate,
      String location,
      String description,
      String type,
      String category,
      Long numberOfDetection,
      Double percentageOfDetection,
      List<String> internalType)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {

    FrameworkNode fn =
        new FrameworkNode(
            neo4jAL,
            name,
            pattern,
            isRegex,
            discoveryDate,
            location,
            description,
            numberOfDetection,
            percentageOfDetection,
            new Date().getTime());

    fn.setInternalTypes(internalType);
    fn.setFrameworkType(FrameworkType.getType(type));

    return FrameworkNode.updateFrameworkByName(neo4jAL, oldName, oldInternalType, category, fn);
  }

  /**
   * Merge a Framework node if its overlapping with a current framework node, if not create a new
   * one
   *
   * @param neo4jAL Neo4j Access Layer
   * @param name Name of the Framework
   * @param discoveryDate Date of the discovery
   * @param location Location of the Framework ( repository, url, etc...)
   * @param description Description
   * @param type Framework category ( Framework, NotFramework, etc..)
   * @param category Category of the framework
   * @param internalTypes Internal types of the framework
   * @param numberOfDetection Number of detection
   * @param percentageOfDetection Detection rate
   * @return
   * @throws Neo4jQueryException
   * @throws Neo4jBadNodeFormatException
   */
  public static FrameworkNode mergeFramework(
      Neo4jAL neo4jAL,
      String name,
      String pattern,
      Boolean isRegex,
      String discoveryDate,
      String location,
      String description,
      String type,
      String category,
      Long numberOfDetection,
      Double percentageOfDetection,
      List<String> internalTypes)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {

    // Find framework with same name and overlapping type
    String req =
        String.format(
            "MATCH (o:%s) WHERE o.%s=$frameworkName "
                + "AND any( x IN $internalTypes WHERE x IN o.%s) AND o.%s=$type RETURN o as framework LIMIT 1",
            FrameworkNode.getLabel(),
            FrameworkNode.getNameProperty(),
            FrameworkNode.getInternalTypeProperty(),
            FrameworkNode.getTypeProperty());
    Map<String, Object> params =
        Map.of("frameworkName", name, "internalTypes", internalTypes, "type", type);

    Result res = neo4jAL.executeQuery(req, params);

    FrameworkNode fn = null;
    if (res.hasNext()) {
      // Overlapping Framework // Merge
      Node n = (Node) res.next().get("framework");
      fn = FrameworkNode.fromNode(neo4jAL, n);

      List<String> oldInternal =
          Neo4jTypeManager.getAsStringList(n, FrameworkNode.getInternalTypeProperty());

      // Update the categories
      Set<String> setCategories = new HashSet<>();
      setCategories.addAll(internalTypes);
      setCategories.addAll(oldInternal);

      fn.updateInternalTypes(new ArrayList<>(setCategories));
      fn.updatePattern(pattern, isRegex);

      // Description update
      if (!description.isBlank()) {
        fn.updateDescription(description);
      }

      // Location update
      if (!location.isBlank()) {
        fn.updateLocation(location);
      }

      // Percentage update
      if (percentageOfDetection > fn.getPercentageOfDetection()) {
        fn.updateDetectionScore(percentageOfDetection);
      }

      fn.sendToPythia();

    } else {
      fn =
          new FrameworkNode(
              neo4jAL,
              name,
              pattern,
              isRegex,
              discoveryDate,
              location,
              description,
              numberOfDetection,
              percentageOfDetection,
              new Date().getTime());

      fn.setInternalTypes(internalTypes);
      fn.setFrameworkType(FrameworkType.getType(type));

      CategoryNode cn = CategoryController.getOrCreateByName(neo4jAL, category);
      fn.setCategory(cn);

      fn.createNode();
      fn.sendToPythia();
    }

    return fn;
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
            FrameworkNode.getLabel(), FrameworkNode.getNameProperty());
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
            "MATCH(o:%s) WHERE $internalType in o.%s  RETURN o as framework ORDER BY o.%s SKIP $toSkip LIMIT $limit;",
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
            "MATCH(o:%s) WHERE $internalType in o.%s RETURN COUNT(o) as count;",
            FrameworkNode.getLabel(), FrameworkNode.getInternalTypeProperty());
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
  public static Long getNumCandidateByLanguage(Neo4jAL neo4jAL, String application, String language)
      throws Neo4jQueryException {
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
   *
   * @param neo4jAL Neo4j Access Layer
   * @param name Name to search
   * @param limit Limit of results
   * @return
   */
  public static List<FrameworkNode> findFrameworkNameContains(
      Neo4jAL neo4jAL, String name, Long limit) throws Neo4jQueryException {
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
   *
   * @param neo4jAL Neo4j Access Layer
   * @return
   * @throws Neo4jQueryException
   */
  public static List<String> getFrameworkInternalTypes(Neo4jAL neo4jAL) throws Neo4jQueryException {
    String request = "MATCH(o:Object) RETURN DISTINCT o.InternalType as internalType;";

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

  /**
   * Get the frameworks younger than a certain timestamp
   *
   * @param neo4jAL Neo4J Access Layer
   * @param limitTimestamp Timestamp
   * @return List of Framework youger than the timestamp provided
   * @throws Neo4jQueryException
   */
  public static List<FrameworkNode> getFrameworkYoungerThan(Neo4jAL neo4jAL, Long limitTimestamp)
      throws Neo4jQueryException {
    String req =
        String.format(
            "MATCH (o:%s) WHERE EXISTS(o.%2$s) AND o.%2$s > $timestamp RETURN o as framework",
            FrameworkNode.getLabel(), FrameworkNode.getCreationDateProperty());
    Map<String, Object> params = Map.of("timestamp", limitTimestamp);

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

  /**
   * Get the validated Frameworks but with missing category or description
   *
   * @param neo4jAL Neo4J Access Layer
   * @return The list of Framework awaiting some properties
   * @throws Neo4jQueryException
   */
  public static List<FrameworkNode> getToValidateFrameworks(Neo4jAL neo4jAL)
      throws Neo4jQueryException {
    String req =
        String.format(
            "MATCH (o:%s) WHERE o.%s=$frameworkType AND ( o.%s='' OR o.%s='' ) RETURN o as framework",
            FrameworkNode.getLabel(),
            FrameworkNode.getTypeProperty(),
            FrameworkNode.getCategoryProperty(),
            FrameworkNode.getDescriptionProperty());
    Map<String, Object> params = Map.of("frameworkType", "Framework");

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

  /**
   * Get duplicate nodes in the configuration, for user manual review
   *
   * @param neo4jAL Neo4j Access Layer
   * @return
   * @throws Neo4jQueryException
   */
  public static List<FrameworkNode> getDuplicates(Neo4jAL neo4jAL) throws Neo4jQueryException {
    String req =
        String.format(
            "MATCH (n:%s) WITH n.Name as name, COLLECT(n) as nodes  WHERE SIZE(nodes) > 1 UNWIND nodes as n RETURN n as framework",
            FrameworkNode.getLabel());

    Node n;
    List<FrameworkNode> frameworkNodeList = new ArrayList<>();
    Result res = neo4jAL.executeQuery(req);
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
   * Automatically clean the framework in the database, taking the best candidate if duplicates are
   * found
   *
   * @param neo4jAL
   * @return
   * @throws Neo4jQueryException
   */
  public static Long autoClean(Neo4jAL neo4jAL) throws Neo4jQueryException {
    String req =
        String.format(
            "MATCH (n:%s) WITH n.Name as name, COLLECT(n) as nodes  WHERE SIZE(nodes) > 1 RETURN nodes as nodes",
            FrameworkNode.getLabel());

    Node[] nodes;
    List<Node> nodesList;
    List<FrameworkNode> frameworkNodeList = new ArrayList<>();
    Long numGroupClean = 0L;
    Result res = neo4jAL.executeQuery(req);
    while (res.hasNext()) {
      nodesList = (List<Node>) res.next().get("nodes"); // Get the list of duplicates

      ListIterator<Node> itNode = nodesList.listIterator();

      FrameworkNode bestCandidate =
          null; // Get the first node considered as a framework. The first node otherwise
      while (itNode.hasNext()) {
        Node next = itNode.next();

        try {
          FrameworkNode fn = FrameworkNode.fromNode(neo4jAL, next);
          if (bestCandidate == null || fn.getFrameworkType() == FrameworkType.FRAMEWORK) {
            bestCandidate = fn;
          }
        } catch (Exception | Neo4jBadNodeFormatException e) {
          // Not a valid node, delete
          next.getRelationships().forEach(Relationship::delete);
          next.delete();
          itNode.remove();
        }
      }

      // If there is no best candidate, go to the next group
      if (bestCandidate == null) continue;

      // Reiterate and merge other properties
      itNode = nodesList.listIterator();
      while (itNode.hasNext()) {
        Node next = itNode.next();
        try {
          FrameworkNode fn = FrameworkNode.fromNode(neo4jAL, next);

          // Merge description, locations and type

          if (bestCandidate.getDescription().isBlank() && !fn.getDescription().isBlank()) {
            bestCandidate.updateDescription(fn.getDescription());
          }

          if (bestCandidate != null
              && bestCandidate.getLocation().isBlank()
              && !fn.getLocation().isBlank()) {
            bestCandidate.updateLocation(fn.getLocation());
          }

          // Delete other nodes ( if its not the best candidate)
          if (next.getId() != bestCandidate.getNode().getId()) {
            next.getRelationships().forEach(Relationship::delete);
            next.delete();
          }

        } catch (Exception | Neo4jBadNodeFormatException e) {
          // Not a valid node, delete
          next.getRelationships().forEach(Relationship::delete);
          next.delete();
          itNode.remove();
        }
      }
      numGroupClean++;
    }

    return numGroupClean;
  }

  /**
   * Get the number of frameworks younger than a certain timestamp
   *
   * @param neo4jAL Neo4J Access Layer
   * @param limitTimestamp Timestamp @ The number of frameworks youger
   * @throws Neo4jQueryException
   */
  public static Long getFrameworkYoungerThanForecast(Neo4jAL neo4jAL, Long limitTimestamp)
      throws Neo4jQueryException {
    String req =
        String.format(
            "MATCH (o:%s) WHERE EXISTS(o.%2$s) AND o.%2$s > $timestamp ADN o.%3$s=$type RETURN COUNT(DISTINCT o) as numFramework",
            FrameworkNode.getLabel(),
            FrameworkNode.getCreationDateProperty(),
            FrameworkNode.getTypeProperty());
    Map<String, Object> params = Map.of("timestamp", limitTimestamp, "type", "Framework");

    Result res = neo4jAL.executeQuery(req, params);
    if (res.hasNext()) {
      return (Long) res.next().get("numFramework");
    } else {
      return 0L;
    }
  }

  /**
   * Get the timestamp of the last update in the Database
   *
   * @param neo4jAL Neo4j Access Layer
   * @return The Last update timestamp
   * @throws Neo4jQueryException
   * @throws Neo4jBadRequestException
   */
  public static Long getLastUpdate(Neo4jAL neo4jAL)
      throws Neo4jQueryException, Neo4jBadRequestException {
    NodeConfiguration nodeConf = NodeConfiguration.getInstance(neo4jAL);
    return nodeConf.getLastUpdate();
  }

  /**
   * Reformat the Framework nodes in the database ( Add default properties if they aren't present)
   *
   * @param neo4jAL Neo4j Access Layer
   * @return
   * @throws Neo4jQueryException
   */
  public static Long reformatFrameworks(Neo4jAL neo4jAL) throws Neo4jQueryException {
    String req = String.format("MATCH (o:%s) RETURN o as framework", FrameworkNode.getLabel());

    Node n;
    Long numFramework = 0L;
    Result res = neo4jAL.executeQuery(req);
    while (res.hasNext()) {
      n = (Node) res.next().get("framework");
      try {
        FrameworkNode.fromNode(neo4jAL, n);
        numFramework++;
      } catch (Exception | Neo4jBadNodeFormatException e) {
        neo4jAL.logError("Failed to reformat a framework.", e);
      }
    }

    return numFramework;
  }

  /**
   * Update the framework using its ID ( will deleted the previuous node and recreate a new one)
   *
   * @param neo4jAl
   * @param id
   * @param name
   * @param discoveryDate
   * @param location
   * @param description
   * @param type
   * @param category
   * @param internalTypes
   * @return
   * @throws Neo4jQueryException
   * @throws Neo4jBadNodeFormatException
   */
  public static Boolean updateById(
      Neo4jAL neo4jAl,
      Long id,
      String name,
      String pattern,
      Boolean isRegex,
      String discoveryDate,
      String location,
      String description,
      String type,
      String category,
      List<String> internalTypes)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {
    String req =
        String.format(
            "MATCH (o:%s) WHERE ID(o)=$idNode RETURN o as framework LIMIT 1",
            FrameworkNode.getLabel());
    Map<String, Object> params = Map.of("idNode", id);

    Result res = neo4jAl.executeQuery(req, params);
    if (res.hasNext()) {
      Node oldFramework = (Node) res.next().get("framework");

      FrameworkNode newFramework =
          addFramework(
              neo4jAl, name, pattern, isRegex, discoveryDate, location, description, type, category, internalTypes);

      for (Relationship rel : oldFramework.getRelationships()) rel.delete();
      oldFramework.delete();

      return true;
    } else {
      return false;
    }
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
   * @param internalTypes Internal type of the object detected
   * @return The node created
   * @throws Neo4jQueryException
   */
  public static FrameworkNode addFramework(
      Neo4jAL neo4jAL,
      String name,
      String pattern,
      Boolean isRegex,
      String discoveryDate,
      String location,
      String description,
      String type,
      String category,
      List<String> internalTypes)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {

    FrameworkNode fn =
        new FrameworkNode(
            neo4jAL, name, pattern, isRegex, discoveryDate, location, description, 0l, .0, new Date().getTime());
    fn.setInternalTypes(internalTypes);
    fn.setFrameworkType(FrameworkType.getType(type));
    fn.createNode();

    // Set category
    CategoryNode cn = CategoryController.getOrCreateByName(neo4jAL, category);
    fn.setCategory(cn);

    neo4jAL.logInfo(
        String.format("Framework with name %s has been inserted through API call", name));
    neo4jAL.logInfo(
            String.format("Sending to Pythia", name));
    fn.sendToPythia();
    return fn;
  }

  /**
   * Delete a Framework node by its ID
   *
   * @param neo4jAl Neo4j Access Layer
   * @param id Id of the Framework node
   * @return
   * @throws Neo4jQueryException
   */
  public static Boolean deleteById(Neo4jAL neo4jAl, Long id) throws Neo4jQueryException {
    String req =
        String.format(
            "MATCH (o:%s) WHERE ID(o)=$idNode RETURN o as framework LIMIT 1",
            FrameworkNode.getLabel());
    Map<String, Object> params = Map.of("idNode", id);

    Result res = neo4jAl.executeQuery(req, params);
    if (res.hasNext()) {
      Node oldFramework = (Node) res.next().get("framework");
      for (Relationship rel : oldFramework.getRelationships()) rel.delete();
      oldFramework.delete();
      return true;
    } else {
      return false;
    }
  }

  public static Boolean toggleValidationById(Neo4jAL neo4jAl, Long id)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {
    String req =
        String.format(
            "MATCH (o:%s) WHERE ID(o)=$idNode RETURN o as framework LIMIT 1",
            FrameworkNode.getLabel());
    Map<String, Object> params = Map.of("idNode", id);

    Result res = neo4jAl.executeQuery(req, params);
    if (res.hasNext()) {
      Node oldFramework = (Node) res.next().get("framework");
      FrameworkNode frameworkNode = FrameworkNode.fromNode(neo4jAl, oldFramework);
      FrameworkType ft = frameworkNode.getFrameworkType();

      switch (ft) {
        case FRAMEWORK:
          frameworkNode.updateType(FrameworkType.NOT_FRAMEWORK);
          break;
        case TO_INVESTIGATE:
        case NOT_FRAMEWORK:
        case NOT_KNOWN:
          frameworkNode.updateType(FrameworkType.FRAMEWORK);
          break;
      }
      return true;
    } else {
      return false;
    }
  }
}
