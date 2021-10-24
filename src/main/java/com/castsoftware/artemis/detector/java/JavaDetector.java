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

package com.castsoftware.artemis.detector.java;

import com.castsoftware.artemis.config.detection.DetectionParameters;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.detector.ADetector;
import com.castsoftware.artemis.detector.PythiaSituation;
import com.castsoftware.artemis.detector.java.utils.FrameworkTree;
import com.castsoftware.artemis.detector.java.utils.FrameworkTreeLeaf;
import com.castsoftware.artemis.detector.utils.DetectorTypeMapper;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.modules.nlp.SupportedLanguage;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaFramework;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaPattern;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/** Java detector */
public class JavaDetector extends ADetector {

  // Tree
  private final FrameworkTree externalTree;
  private final FrameworkTree internalTree;
  private final String corePrefix;

  /**
   * Detector for the Java
   *
   * @param neo4jAL Neo4j Access Layer
   * @param application Name of the application
   * @throws IOException
   * @throws Neo4jQueryException
   */
  public JavaDetector(Neo4jAL neo4jAL, String application, DetectionParameters detectionParameters)
      throws Neo4jQueryException, IOException {
    super(neo4jAL, application, SupportedLanguage.JAVA, detectionParameters);
    this.externalTree = this.getExternalBreakdown();
    this.internalTree = this.getInternalBreakdown();
    this.corePrefix = "";
  }

  /**
   * Create Framework tree based on External Classes
   *
   * @return The framework tree
   * @throws Neo4jQueryException
   */
  @Override
  public FrameworkTree getExternalBreakdown() throws Neo4jQueryException {
    List<Node> nodeList = this.getNodesByExternality(true);

    // Filter nodes based to make sure :
    nodeList.removeIf(n -> !n.hasProperty("FullName")); // They have a name
    nodeList.removeIf(
        n ->
            !n.hasProperty("Level")
                || !n.getProperty("Level").toString().equals("Java Class")); // They have a level

    // Create a framework tree
    String fullName;
    FrameworkTree ft = new FrameworkTree();
    for (Node n : nodeList) {
      fullName = (String) n.getProperty("FullName").toString();
      ft.insert(fullName, n);
    }

    return ft;
  }

  /**
   * Create a Framework tree based on the Internal Breakdown
   *
   * @return
   * @throws Neo4jQueryException
   */
  @Override
  public FrameworkTree getInternalBreakdown() throws Neo4jQueryException {
    List<Node> nodeList = this.getNodesByExternality(false);

    // Filter nodes based to make sure :
    nodeList.removeIf(n -> !n.hasProperty("FullName")); // They have a name
    nodeList.removeIf(
        n ->
            !n.hasProperty("Level")
                || !n.getProperty("Level").toString().equals("Java Class")); // They have a level

    // Create a framework tree
    String fullName;
    FrameworkTree ft = new FrameworkTree();
    for (Node n : nodeList) {
      fullName = (String) n.getProperty("FullName").toString();
      ft.insert(fullName, n);
    }

    return ft;
  }

  /***
   * Extract known utilities in the application
   * @return The List of Framework detected
   * @throws IOException
   * @throws Neo4jQueryException
   */
  @Override
  public List<FrameworkNode> extractUtilities() throws Neo4jQueryException {
    neo4jAL.logInfo("Now extract known utilities for Java");
    // Init properties
    List<FrameworkNode> listFramework = new ArrayList<>();

    neo4jAL.logInfo("Extract External utilities");
    // Build a tree based on the nodes to investigate
    FrameworkTree externals = this.getExternalBreakdown();
    externals.print();
    listFramework.addAll(analyzeFrameworkTree(externals, true));

    return listFramework;
  }

  /**
   * Analyze a tree of Java Classes
   *
   * @param tree Tree to analyze
   * @param external Type of investigation ( internal / external )
   * @return The list of Framework found
   */
  private List<FrameworkNode> analyzeFrameworkTree(FrameworkTree tree, Boolean external) {
    // Initialize the list
    Queue<FrameworkTreeLeaf> queue = new ConcurrentLinkedQueue<>();
    queue.addAll(tree.getRoot().getChildren());
    List<FrameworkNode> frameworkNodeList = new ArrayList<>();

    // Iterate over the Tree
    for (FrameworkTreeLeaf frameworkTreeLeaf : queue) {
      neo4jAL.logInfo(String.format("Exploring leaf: %s ", frameworkTreeLeaf.getName()));
      boolean addChildren = true;

      // Switch on the depth
      switch (frameworkTreeLeaf.getDepth()) {
        case 0:
          break; // Root ?
        case 1:
          break; // Organisation
        case 2: // Company
        case 3: // Packages
          // Add package for company and package, if nothing has been found deleted the children
          List<PythiaFramework> found = findOrSaveOnPythia(frameworkTreeLeaf, external);

          if (!found.isEmpty()) {
            neo4jAL.logInfo(
                String.format(
                    "Successful detection for pattern [%s] , %d frameworks has been found.",
                    frameworkTreeLeaf.getFullName(), found.size()));
            addChildren = false;

            // Convert to framework node
            frameworkNodeList.addAll(
                found.stream()
                    .map(
                        x ->
                            DetectorTypeMapper.pythiaFrameworkToFrameworkNode(
                                neo4jAL, x, frameworkTreeLeaf.getFullName(), true))
                    .collect(Collectors.toList()));
          } else {
            neo4jAL.logInfo(
                String.format(
                    "Detection for pattern [%s] returned no result. The framework has been saved.",
                    frameworkTreeLeaf.getFullName()));
          }
          // If too many children do not add children
          break;
        default:
          // Beyond 3 stop to correct
          addChildren = false;
          break;
      }
      if (addChildren) queue.addAll(frameworkTreeLeaf.getChildren()); // Add the children
    }
    return frameworkNodeList;
  }

  /**
   * Find a framework using the pattern or save it
   *
   * @param ftl Framework to save
   * @return True if the framework has been found, False otherwise
   */
  private List<PythiaFramework> findOrSaveOnPythia(FrameworkTreeLeaf ftl, Boolean external) {
    PythiaSituation pts = PythiaSituation.NOT_FOUND;
    List<PythiaFramework> returnList = new ArrayList<>();

    // If not activated, return not found
    if (!activatedPythia) return returnList;

    Optional<PythiaFramework> framework =
        this.findFrameworkOnPythia(ftl.getFullName()); // Find the framework

    // If not found, save it
    if (framework.isEmpty()) {
      // Save the framework
      this.saveFrameworkLeafOnPythia(ftl);
      return returnList; // Return empty list
    }

    // Framework has been found, save return value
    PythiaFramework pf = framework.get();

    if (pf.getRoot()) {
      // If root, add automatically its children look on pythia otherwise save it
      for (FrameworkTreeLeaf cftl : ftl.getChildren()) {
        // Save and Flag them
        PythiaFramework cpf;

        // Check on pythia
        Optional<PythiaFramework> childFramework =
                this.findFrameworkOnPythia(cftl.getFullName()); // Find the framework
        if(childFramework.isPresent()) {
          cpf = childFramework.get();
        } else {
          cpf = DetectorTypeMapper.frameworkLeafToPythia(cftl, pythiaLanguage); // Convert to pythia framework
          this.saveFrameworkLeafOnPythia(cftl); // As it is the children of a root
        }

        // Flag the nodes
        flagNodesPythia(cftl.getFullName(), cpf, external);
        returnList.add(cpf);
      }
    } else {
      // Else flag the framework
      flagNodesPythia(ftl.getFullName(), pf, external);
      returnList.add(pf);
    }

    // Return the value of the detection ( FOUND AND FOUND AS ROOT if successful ) NOT FOUND
    // OTHERWISE
    return returnList;
  }

  /**
   * Save a Framework Leaf on Pythia
   *
   * @param ftl Framework Tree leaf to save
   */
  private PythiaFramework saveFrameworkLeafOnPythia(FrameworkTreeLeaf ftl) {
    PythiaFramework pf = DetectorTypeMapper.frameworkLeafToPythia(ftl, this.pythiaLanguage);
    PythiaPattern pp = DetectorTypeMapper.fromFrameworkLeafToPattern(ftl, this.pythiaLanguage);
    this.saveFrameworkOnPythia(pf, Collections.singletonList(pp));
    return pf;
  }

  /** Flag nodes in application with pythia */
  private void flagNodesPythia(String pattern, PythiaFramework pythiaFramework, Boolean external) {
    // For all the patterns in detected framework
    String request =
        String.format(
            "MATCH (o:Object:`%s`) "
                + "WHERE o.External=$external AND o.FullName STARTS WITH $pattern "
                + "RETURN DISTINCT  o as node",
            this.application);
    Map<String, Object> params = Map.of("external", external, "pattern", pattern);

    // Try and print results
    try {

      int numFlagged = 0;
      Node n;
      Result results = neo4jAL.executeQuery(request, params);
      while (results.hasNext()) {
        // Flag all the nodes
        n = (Node) results.next().get("node");
        tagNodeWithPythia(n, pythiaFramework);
        numFlagged++;
      }

      neo4jAL.logInfo(
          String.format(
              "%d nodes have been identified by pattern '%s' as framework : %s",
              numFlagged, pattern, pythiaFramework.name));
    } catch (Neo4jQueryException e) {
      neo4jAL.logError(
          String.format("Failed to flag nodes using inputs pythia on pattern %s.", pattern), e);
    }
  }

  @Override
  public void extractUnknownApp() {}

  @Override
  public void extractOtherApps() {}

  @Override
  public void extractUnknownNonUtilities() {}

  /**
   * Get the list of node for the Java language by externality
   *
   * @param externality Externality of the nodes
   * @return The list of nodes
   * @throws Neo4jQueryException If the Neo4j query failed
   */
  public List<Node> getNodesByExternality(Boolean externality) throws Neo4jQueryException {
    try {
      List<String> categories = languageProperties.getObjectsInternalType();
      List<Node> nodeList = new ArrayList<>();
      String forgedRequest;
      Map<String, Object> params;
      Result res;

      // Check if the categories are empty or not
      if (categories.isEmpty()) {
        forgedRequest =
            String.format(
                "MATCH (obj:Object:`%s`) WHERE obj.Level='Java Class' "
                    + "AND obj.External=$externality RETURN obj as node",
                application);
        params = Map.of("externality", externality);

      } else {
        forgedRequest =
            String.format(
                "MATCH (obj:Object:`%s`) WHERE  obj.InternalType in $internalTypes  "
                    + "AND obj.External=$externality  RETURN obj as node",
                application);
        params = Map.of("internalTypes", categories, "externality", externality);
      }
      res = neo4jAL.executeQuery(forgedRequest, params);

      while (res.hasNext()) {
        Map<String, Object> resMap = res.next();
        Node node = (Node) resMap.get("node");
        nodeList.add(node);
      }

      neo4jAL.logInfo(
          String.format(
              "%d Java nodes were found with external property on '%s'",
              nodeList.size(), externality));

      return nodeList;
    } catch (Neo4jQueryException err) {
      neo4jAL.logError(
          String.format(
              "Failed to retrieve the list of the external nodes in the application %s",
              this.application),
          err);
      throw err;
    }
  }
}
