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

package com.castsoftware.artemis.detector.plainAnalyzers.net;

import com.castsoftware.artemis.config.detection.DetectionParameters;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.detector.plainAnalyzers.ADetector;
import com.castsoftware.artemis.detector.plainAnalyzers.DetectorUtil;
import com.castsoftware.artemis.detector.statisticalAnalyzers.net.NetStatisticalAnalyzer;
import com.castsoftware.artemis.detector.utils.DetectionCategory;
import com.castsoftware.artemis.detector.utils.DetectorNodesUtil;
import com.castsoftware.artemis.detector.utils.DetectorPropertyUtil;
import com.castsoftware.artemis.detector.utils.trees.ALeaf;
import com.castsoftware.artemis.detector.utils.trees.TreeFactory;
import com.castsoftware.artemis.detector.utils.DetectorTypeMapper;
import com.castsoftware.artemis.detector.utils.trees.net.NetFrameworkTree;
import com.castsoftware.artemis.detector.utils.trees.net.NetFrameworkTreeLeaf;
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

/** NET detector */
public class NetDetector extends ADetector {

  /**
   * Detector for the Net
   *
   * @param neo4jAL Neo4j Access Layer
   * @param application Name of the application
   * @throws IOException If the Detector was not able to read the workspace
   * @throws Neo4jQueryException If the initialization produced an error on the neo4j database
   */
  public NetDetector(Neo4jAL neo4jAL, String application, DetectionParameters detectionParameters)
          throws Neo4jQueryException, IOException {
    super(neo4jAL, application, SupportedLanguage.NET, detectionParameters);
  }

  /**
   * Create Framework tree based on External Classes
   *
   * @return The framework tree
   * @throws Neo4jQueryException
   */
  @Override
  public NetFrameworkTree getExternalBreakdown() throws Neo4jQueryException {
    List<Node> nodeList = this.getNodesByExternality(true);

    // Create a framework tree
    NetFrameworkTree ft = new NetFrameworkTree(languageProperties);
    ft.recursiveObjectsInsert(nodeList);
    ft.print();

    return ft;
  }

  /**
   * Create a Framework tree based on the Internal Breakdown
   *
   * @return A new NET framework tree
   * @throws Neo4jQueryException
   */
  @Override
  public NetFrameworkTree getInternalBreakdown() throws Neo4jQueryException {
    List<Node> nodeList = this.getNodesByExternality(false);

    // Filter nodes based to make sure :

    // Create a framework tree
    NetFrameworkTree ft = new NetFrameworkTree(languageProperties);
    ft.recursiveObjectsInsert(nodeList);

    return ft;
  }

  /***
   * Extract known utilities in the application
   * @return The List of Framework detected
   * @throws Neo4jQueryException
   */
  @Override
  public void extractFrameworks() throws Neo4jQueryException {
    neo4jAL.logInfo("Now extract known utilities for Net");
    // Init properties

    neo4jAL.logInfo("Extract External utilities");
    // Build a tree based on the nodes to investigate
    NetFrameworkTree externals = this.getExternalBreakdown();
    externals.print();

    List<FrameworkNode> frameworkNodes =  analyzeFrameworkTree(externals, true);
    frameworkNodes.forEach(this::addFrameworkToResults); // Add to final results
  }

  /**
   * Handle a new pattern situation
   * @param frameworkTreeLeaf Framework leaf
   * @param external Externality
   * @param parentFramework Parent framework
   */
  private  Set<FrameworkNode> handleUnknownPattern(NetFrameworkTreeLeaf frameworkTreeLeaf, Boolean external, PythiaImagingFramework parentFramework) {
    Set<FrameworkNode> returnSet = new HashSet<>();
    try {
      if(parentFramework == null) return returnSet; // Error

      // If not framework found on  pythia, take parent otherwise save it

      // Flag with parent
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
  private Set<FrameworkNode> recursiveParsing(NetFrameworkTreeLeaf frameworkTreeLeaf, Boolean external, int depth, PythiaImagingFramework parentFramework) {
    // Parse tree leaf and find on pythia
    neo4jAL.logInfo("-".repeat(depth) + String.format("> Treating [%s] ", frameworkTreeLeaf.getFullName()));
    Set<FrameworkNode> returnSet = new HashSet<>();

    // if found a framework continue to check children
    Optional<PythiaImagingFramework> pythiaFramework = this.findFrameworkOnPythia(frameworkTreeLeaf.getFullName()); // find on pythia by name and language
    if (pythiaFramework.isEmpty()) {
      if (parentFramework != null)  {
          // Add handle pattern if already deep in the tree
          returnSet.addAll(this.handleUnknownPattern(frameworkTreeLeaf, external, parentFramework));
      } else {
        if(depth < 2) {
          // Continue to investigate low-depth nodes
          Set<FrameworkNode> tempSet;
          for (NetFrameworkTreeLeaf child : frameworkTreeLeaf.getChildren()) {
            tempSet = this.recursiveParsing(child, external, depth+ 1, null);
            returnSet = SetUtils.mergeSet(returnSet, tempSet);
          }
        }
        // Save the framework on pythia if not known
        this.saveFrameworkLeafOnPythia(frameworkTreeLeaf);
      }
    } else {
      // Add to return list
      FrameworkNode fn = DetectorTypeMapper.imagingFrameworkToFrameworkNode(
              neo4jAL, pythiaFramework.get(), true);
      returnSet.add(fn);

      // A framework has been found, flag the nodes an pursue
      this.flagNodesWithImagingFramework(pythiaFramework.get(), external);

      // Continue the recursive search
      Set<FrameworkNode> tempSet;
      for (NetFrameworkTreeLeaf child : frameworkTreeLeaf.getChildren()) {
        tempSet = this.recursiveParsing(child, external, depth+ 1, pythiaFramework.get());
        returnSet = SetUtils.mergeSet(returnSet, tempSet);
      }
    }

    return returnSet;
  }


  /**
   * Analyze a tree of Net Classes
   *
   * @param tree Tree to analyze
   * @param external Type of investigation ( internal / external )
   * @return The list of Framework found
   */
  private List<FrameworkNode> analyzeFrameworkTree(NetFrameworkTree tree, Boolean external) {
    // Initialize the list
    Set<FrameworkNode> frameworkNodeList = new HashSet<>();
    for(NetFrameworkTreeLeaf ftl : tree.getRoot().getChildren()) {
      frameworkNodeList.addAll(recursiveParsing(ftl, external,  1,  null));
    }

    return new ArrayList<>(frameworkNodeList);
  }



  /**
   * Save a Framework Leaf on Pythia
   *
   * @param ftl Framework Tree leaf to save
   */
  private PythiaFramework saveFrameworkLeafOnPythia(NetFrameworkTreeLeaf ftl) {
    PythiaFramework pf = DetectorTypeMapper.frameworkLeafToPythia(ftl, this.language);
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
    neo4jAL.logInfo(String.format("Starting post launch operations in application '%s'.", application));
    // Launch statistical detector for NET
    try {
      neo4jAL.logInfo("Starting the statistical engine.");
      NetStatisticalAnalyzer statisticalAnalyzer = new NetStatisticalAnalyzer(neo4jAL, this.application, language);
      statisticalAnalyzer.flagCore();
      List<FrameworkNode> results = statisticalAnalyzer.getResults();
      results.forEach(this::addFrameworkToResults);
    } catch (Neo4jQueryException e) {
      neo4jAL.logError("Failed to extract the external core of the application.", e);
    }

    // Investigate missing items
    neo4jAL.logInfo("Starting the extraction of missed items.");
    this.extractFlaggedExternalNet();
  }

  /**
   * Extract all the nodes that were not discovered by the extension. Return and flag them
   */
  public void extractFlaggedExternalNet() {
    try {
      neo4jAL.logInfo("Missed item extraction launched.");

      // Get the list of nodes that were missed during the 1st analysis
      // Initialize the parameters
      List<String> levels = List.of("VB.NET Class", "C# Class", ".NET Class", "Missing DotNet Class");
      String filterLevel = DetectorUtil.buildLevelFilter("obj", levels);

      String detectionProperty = DetectorPropertyUtil.getDetectionProperty();
      List<String> categories = languageProperties.getObjectsInternalType();
      String selectMissed  =
              String.format(
                      "MATCH (obj:Object:`%s`) WHERE obj.InternalType in $internalTypes  " +
                              "AND obj.External=true  " +
                              "AND ( " + filterLevel + " ) AND NOT EXISTS(obj.%s) " +
                              "RETURN DISTINCT obj as node",
                      application, detectionProperty);
      Map<String, Object>  params = Map.of("internalTypes", categories);;
      Result res  = neo4jAL.executeQuery(selectMissed, params);
      List<Node> nodeList = new ArrayList<>();

      while (res.hasNext()) {
        Map<String, Object> resMap = res.next();
        Node node = (Node) resMap.get("node");
        nodeList.add(node);
      }

      neo4jAL.logInfo(String.format("%d nodes will be investigated.", nodeList.size()));

      int numberDetected = 0;

      // Create a tree and slice the leve 2 to get the name of the missing packages
      NetFrameworkTree frameworkTreeLeaf = TreeFactory.createNetTree(this.languageProperties, nodeList);
      List<ALeaf> leafList =  frameworkTreeLeaf.getSliceByDepth(0); // Get the 2 package list


      // Convert leaf to Framework Node and push to results as to Investigate
      for(ALeaf x : leafList) {
        // Create Framework fro {m the leaf and set the default paramters
        FrameworkNode fn = DetectorTypeMapper.fromFrameworkLeafToFrameworkNode(neo4jAL, x);
        fn.setFrameworkType(FrameworkType.TO_INVESTIGATE);
        String level4 = x.getName();
        String level5 = String.format("API %s", level4);

        try {
          // Get nodes under the leaf, and flag them as missing code
          List<Node> nodes = DetectorNodesUtil.getNodesByPatternAndExternality(neo4jAL, application, x.getFullName(), true);
          for (Node n : nodes) {
            DetectorNodesUtil.tagNodeWithFramework(neo4jAL, n, DetectionCategory.MISSING_CODE, level4, level5, x.getName(), "");
          }

          // Send to list of framework
          this.addFrameworkToResults(fn);
          numberDetected ++;

          neo4jAL.logInfo(String.format("Flagged the branch [%s]. %d nodes has been extracted as 'missing code'", x.getFullName(), nodes.size()));
        } catch (Neo4jQueryException e) {
          neo4jAL.logError(String.format("Failed to flag branch with pattern [%s].", x.getFullName()));
        }
      }

      neo4jAL.logInfo(String.format("%d branches missed during the analysis have been extracted.", numberDetected));

    } catch (Exception | Neo4jQueryException err) {
      neo4jAL.logError("Failed to extract missed items.", err);
    }
  }

  /**
   * Get the list of node for the Net language by externality
   *
   * @param externality Externality of the nodes
   * @return The list of nodes
   * @throws Neo4jQueryException If the Neo4j query failed
   */
  public List<Node> getNodesByExternality(Boolean externality) throws Neo4jQueryException {
    try {
      // Return list
      List<Node> nodeList = new ArrayList<>();

      // Initialize the parameters
      List<String> levels = List.of("VB.NET Class", "C# Class", ".NET Class");
      String filterLevel = DetectorUtil.buildLevelFilter("obj", levels);
      String detectionProperty = DetectorPropertyUtil.getDetectionProperty();
      List<String> categories = languageProperties.getObjectsInternalType();

      // Build the request  & associate parameters
      String forgedRequest  =
              String.format(
                      "MATCH (obj:Object:`%s`) WHERE obj.InternalType in $internalTypes  "
                              + "AND obj.External=$externality  " +
                              "AND ( " + filterLevel +  " OR EXISTS(obj.%s)) " +
                              "RETURN DISTINCT obj as node",
                      application, detectionProperty);
      Map<String, Object>  params = Map.of("internalTypes", categories, "externality", externality);;
      Result res  = neo4jAL.executeQuery(forgedRequest, params);

      // Transform results to list
      while (res.hasNext()) {
        Map<String, Object> resMap = res.next();
        Node node = (Node) resMap.get("node");
        nodeList.add(node);
      }

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
