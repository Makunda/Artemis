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

package com.castsoftware.artemis.detector.plainAnalyzers.java;

import com.castsoftware.artemis.config.detection.DetectionParameters;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.detector.plainAnalyzers.ADetector;
import com.castsoftware.artemis.detector.statisticalAnalyzers.java.JavaStatisticalAnalyzer;
import com.castsoftware.artemis.detector.utils.DetectorNodesUtil;
import com.castsoftware.artemis.detector.utils.trees.java.JavaFrameworkTree;
import com.castsoftware.artemis.detector.utils.trees.java.JavaFrameworkTreeLeaf;
import com.castsoftware.artemis.detector.utils.DetectorTypeMapper;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.global.SupportedLanguage;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaFramework;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaImagingFramework;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaPattern;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import com.castsoftware.artemis.utils.SetUtils;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/** Java detector */
public class JavaDetector extends ADetector {

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
  }

  /**
   * Create Framework tree based on External Classes
   *
   * @return The framework tree
   * @throws Neo4jQueryException
   */
  @Override
  public JavaFrameworkTree getExternalBreakdown() throws Neo4jQueryException {
    List<Node> nodeList = this.getNodesByExternality(true);
    List<String> acceptedLevels = List.of("Java Class", "Missing Java Class");

    // Filter nodes based to make sure :
    nodeList.removeIf(n -> !n.hasProperty("FullName")); // Node must have a full name

    nodeList.removeIf(
        n ->
            !n.hasProperty("Level")
                || !acceptedLevels.contains(n.getProperty("Level").toString())
    ); // They have a level

    // Create a framework tree
    String fullName;
    JavaFrameworkTree ft = new JavaFrameworkTree(languageProperties);
    for (Node n : nodeList) {
      fullName = (String) n.getProperty("FullName").toString();
      ft.insert(fullName, n);
    }

    return ft;
  }

  /**
   * Create a Framework tree based on the Internal Breakdown
   *
   * @return A ne Java framework tree
   * @throws Neo4jQueryException
   */
  @Override
  public JavaFrameworkTree getInternalBreakdown() throws Neo4jQueryException {
    List<Node> nodeList = this.getNodesByExternality(false);

    // Filter nodes based to make sure :
    nodeList.removeIf(n -> !n.hasProperty("FullName")); // They have a name
    nodeList.removeIf(
        n ->
            !n.hasProperty("Level")
                || !n.getProperty("Level").toString().equals("Java Class")); // They have a level

    // Create a framework tree
    String fullName;
    JavaFrameworkTree ft = new JavaFrameworkTree(languageProperties);
    for (Node n : nodeList) {
      fullName = (String) n.getProperty("FullName").toString();
      ft.insert(fullName, n);
    }

    return ft;
  }

  /***
   * Extract known utilities in the application
   * @return The List of Framework detected
   * @throws IOException IO Exception
   * @throws Neo4jQueryException
   */
  @Override
  public List<FrameworkNode> extractUtilities() throws Neo4jQueryException {
    neo4jAL.logInfo("Now extract known utilities for Java");
    // Init properties

    neo4jAL.logInfo("Extract External utilities");
    // Build a tree based on the nodes to investigate
    JavaFrameworkTree externals = this.getExternalBreakdown();
    externals.print();

    return analyzeFrameworkTree(externals, true);
  }

  /**
   * Handle a new pattern situation
   * @param frameworkTreeLeaf Framework leaf
   * @param external Externality
   * @param parentFramework Parent framework
   */
  private  Set<FrameworkNode> handleUnknownPattern(JavaFrameworkTreeLeaf frameworkTreeLeaf, Boolean external, PythiaImagingFramework parentFramework) {
      Set<FrameworkNode> returnSet = new HashSet<>();
    try {
      if(parentFramework == null) return returnSet; // Error

      // If not framework found on  pythia, take parent otherwise save it

      // Flag with parent
      neo4jAL.logInfo("In loop wth present parent ");
      if(parentFramework.getRoot()) {
        // Flag as independent framework and save on pythia
        this.saveFrameworkLeafOnPythia(frameworkTreeLeaf);

        // Modify parent to adapt it to the new leaf
        parentFramework.setLevel5asAPI(frameworkTreeLeaf.getName());
        parentFramework.setPattern(frameworkTreeLeaf.getFullName(), languageProperties.getPackageDelimiter());

        // Add to the return set
        FrameworkNode fn = DetectorTypeMapper.imagingFrameworkToFrameworkNode(
                neo4jAL, parentFramework, true);
        returnSet.add(fn);

      }
      // Flag nodes
      this.flagNodesWithImagingFramework(parentFramework, external);

      //this.saveFrameworkLeafOnPythia(frameworkTreeLeaf);

    } catch (Exception err) {
      // Ignore
      neo4jAL.logError(String.format("Recursive parse stopped due to an error. Error : %s",  err.getMessage()));
    }
      return returnSet;
  }

  /**
   * Parse the framework tree
   * @param frameworkTreeLeaf Framework leaf to explore
   * @return
   */
  private Set<FrameworkNode> recursiveParsing(JavaFrameworkTreeLeaf frameworkTreeLeaf, Boolean external, int depth, PythiaImagingFramework parentFramework) {
    // Parse tree leaf and find on pythia
    neo4jAL.logInfo("-".repeat(depth) + String.format("> Treating [%s] ", frameworkTreeLeaf.getFullName()));
    Set<FrameworkNode> returnSet = new HashSet<>();

    // Skip organisations packages
    if(depth < 2) {
      Set<FrameworkNode> tempSet;
      for (JavaFrameworkTreeLeaf child : frameworkTreeLeaf.getChildren()) {
        tempSet = this.recursiveParsing(child, external, depth + 1, parentFramework);
        returnSet.addAll(tempSet);
      }
      return returnSet;
    }

    // if found a framework continue to check children
    Optional<PythiaImagingFramework> pythiaFramework = this.findFrameworkOnPythia(frameworkTreeLeaf.getFullName()); // find on pythia by name and language
    if (pythiaFramework.isEmpty()) {
      // No pythia framework found, handle the unknowns
      if(parentFramework != null)  returnSet.addAll(this.handleUnknownPattern(frameworkTreeLeaf, external, parentFramework));
      else this.saveFrameworkLeafOnPythia(frameworkTreeLeaf);
    } else {
      // Add to return list
      FrameworkNode fn = DetectorTypeMapper.imagingFrameworkToFrameworkNode(
              neo4jAL, pythiaFramework.get(), true);
      returnSet.add(fn);

      // A framework has been found, flag the nodes an pursue
      this.flagNodesWithImagingFramework(pythiaFramework.get(), external);

      // Continue the recursive search
      Set<FrameworkNode> tempSet;
      for (JavaFrameworkTreeLeaf child : frameworkTreeLeaf.getChildren()) {
        tempSet = this.recursiveParsing(child, external, depth+ 1, pythiaFramework.get());
        returnSet = SetUtils.mergeSet(returnSet, tempSet);
      }
    }

    return returnSet;
  }


  /**
   * Analyze a tree of Java Classes
   *
   * @param tree Tree to analyze
   * @param external Type of investigation ( internal / external )
   * @return The list of Framework found
   */
  private List<FrameworkNode> analyzeFrameworkTree(JavaFrameworkTree tree, Boolean external) {
    // Initialize the list
    Set<FrameworkNode> frameworkNodeList = new HashSet<>();
    for(JavaFrameworkTreeLeaf ftl : tree.getRoot().getChildren()) {
      frameworkNodeList.addAll(recursiveParsing(ftl, external,  1,  null));
    }

    // New list
    return new ArrayList<>(frameworkNodeList);
  }



  /**
   * Save a Framework Leaf on Pythia
   *
   * @param ftl Framework Tree leaf to save
   */
  private PythiaFramework saveFrameworkLeafOnPythia(JavaFrameworkTreeLeaf ftl) {
    PythiaFramework pf = DetectorTypeMapper.frameworkLeafToPythia(ftl, this.pythiaLanguage);
    PythiaPattern pp = DetectorTypeMapper.fromFrameworkLeafToPattern(ftl, this.pythiaLanguage);
    this.saveFrameworkOnPythia(pf, Collections.singletonList(pp));
    return pf;
  }



  /**
   * Flag nodes matching a certain pattern with an Imaging framework
   * @param pythiaFramework
   * @param external
   */
  private void flagNodesWithImagingFramework(PythiaImagingFramework pythiaFramework, Boolean external) {
    DetectorNodesUtil.flagNodesWithImagingFramework(neo4jAL, application, pythiaFramework, external);
  }

  @Override
  public void postLaunch() {
    // Launch statistical detector for Java
    try {
      JavaStatisticalAnalyzer statisticalAnalyzer = new JavaStatisticalAnalyzer(neo4jAL, this.application, language);
      statisticalAnalyzer.flagCore();
    } catch (Neo4jQueryException e) {
      neo4jAL.logError("Failed to extract the external core of the application.", e);
    }
  }

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
