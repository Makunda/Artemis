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
import com.castsoftware.artemis.detector.java.utils.FrameworkTree;
import com.castsoftware.artemis.detector.java.utils.FrameworkTreeLeaf;
import com.castsoftware.artemis.detector.utils.DetectorTypeMapper;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.modules.nlp.SupportedLanguage;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaFramework;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

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
 * @return The framework tree
 * @throws Neo4jQueryException
 */
  @Override
  public FrameworkTree getExternalBreakdown() throws Neo4jQueryException {
    List<Node> nodeList = this.getNodesByExternality(true);

    // Filter nodes based to make sure :
    nodeList.removeIf(n -> !n.hasProperty("FullName") ); // They have a name
    nodeList.removeIf(n -> !n.hasProperty("Level") || !n.getProperty("Level").toString().equals("Java Class")); // They have a level

    // Create a framework tree
    String fullName;
    FrameworkTree ft = new FrameworkTree();
    for(Node n : nodeList){
    	fullName = (String) n.getProperty("FullName").toString();
    	ft.insert(fullName, n);
	}

    return ft;
  }

	/**
	 * Create a Framework tree based on the Internal Breakdown
	 * @return
	 * @throws Neo4jQueryException
	 */
  @Override
  public FrameworkTree getInternalBreakdown() throws Neo4jQueryException {
	  List<Node> nodeList = this.getNodesByExternality(false);

	  // Filter nodes based to make sure :
	  nodeList.removeIf(n -> !n.hasProperty("FullName") ); // They have a name
	  nodeList.removeIf(n -> !n.hasProperty("Level") || !n.getProperty("Level").toString().equals("Java Class")); // They have a level

	  // Create a framework tree
	  String fullName;
	  FrameworkTree ft = new FrameworkTree();
	  for(Node n : nodeList){
		  fullName = (String) n.getProperty("FullName").toString();
		  ft.insert(fullName, n);
	  }

	  return ft;
  }

	/**
	 * Analyze a tree of Java Classes
	 * @param tree Tree to analyze
	 * @param type Type of investigation ( internal / external )
	 * @return The list of Framework found
	 */
	private List<FrameworkNode> analyzeFrameworkTree(FrameworkTree tree, String type) {
		// Initialize the list
		Queue<FrameworkTreeLeaf> queue = new ConcurrentLinkedQueue<>();
		queue.addAll(tree.getRoot().getChildren());

		// Iterate over the Tree
		for (FrameworkTreeLeaf frameworkTreeLeaf : queue) {
			neo4jAL.logInfo(String.format("Exploring leaf: %s ", frameworkTreeLeaf.getName()));
			boolean addChildren = true;

			switch (frameworkTreeLeaf.getDepth()) {
				case 0: break; // Root ?
				case 1: break; // Organisation
				case 2: // Company
				case 3: // Packages
					// Add package
					// Add Package
					PythiaFramework pf = DetectorTypeMapper.frameworkLeafToPythia(frameworkTreeLeaf, this.language.toString());
					this.savePythiaFramework(pf);
					// If too many children do not add children
					break;
				default:
					// Beyond 3 stop
					addChildren = false;
					break;

			}
			if(addChildren) queue.addAll(frameworkTreeLeaf.getChildren()); // Add the children
		}
		return new ArrayList<>();
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
	  listFramework.addAll(analyzeFrameworkTree(externals, "external"));


	  return listFramework;
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
