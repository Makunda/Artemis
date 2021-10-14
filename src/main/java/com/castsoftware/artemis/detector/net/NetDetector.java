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

package com.castsoftware.artemis.detector.net;

import com.castsoftware.artemis.config.detection.DetectionParameters;
import com.castsoftware.artemis.controllers.api.FrameworkController;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.detector.ADetector;
import com.castsoftware.artemis.detector.java.utils.FrameworkTree;
import com.castsoftware.artemis.detector.utils.ATree;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.modules.nlp.SupportedLanguage;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import org.neo4j.graphdb.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class NetDetector extends ADetector {

  private FrameworkTree externalTree;
  private FrameworkTree internalTree;

  /**
   * Detector constructor
   *
   * @param neo4jAL Neo4j Access Layer
   * @param application Name of the application
   * @throws IOException
   * @throws Neo4jQueryException
   */
  public NetDetector(Neo4jAL neo4jAL, String application, DetectionParameters detectionParameters)
      throws IOException, Neo4jQueryException {
    super(neo4jAL, application, SupportedLanguage.NET, detectionParameters);
  }

  @Override
  public ATree getExternalBreakdown() {
    FrameworkTree frameworkTree = new FrameworkTree();

    // Top Bottom approach
    ListIterator<Node> listIterator = toInvestigateNodes.listIterator();
    while (listIterator.hasNext()) {
      Node n = listIterator.next();
      // Get node in C# or .NET Classes
      if (!n.hasProperty("Level")
          || (!n.getProperty("Level").equals("C# Class")
              && !n.getProperty("Level").equals(".NET Class"))) continue;

      if (!n.hasProperty(IMAGING_OBJECT_FULL_NAME)) continue;
      String fullName = (String) n.getProperty(IMAGING_OBJECT_FULL_NAME);

      neo4jAL.logInfo("Inserting " + fullName);
      frameworkTree.insert(fullName, n);
    }

    return frameworkTree;
  }

  @Override
  public ATree getInternalBreakdown() throws Neo4jQueryException {
    FrameworkTree frameworkTree = new FrameworkTree();

    // Top Bottom approach
    ListIterator<Node> listIterator = getInternalNodes().listIterator();
    while (listIterator.hasNext()) {
      Node n = listIterator.next();
      // Get node in .NET Class Classes
      if (!n.hasProperty("Level")
          || (!n.getProperty("Level").equals("C# Class")
              && !n.getProperty("Level").equals(".NET Class"))) continue;

      if (!n.hasProperty(IMAGING_OBJECT_FULL_NAME)) continue;
      if (!n.hasProperty(IMAGING_INTERNAL_TYPE)) continue;

      String fullName = (String) n.getProperty(IMAGING_OBJECT_FULL_NAME);
      String internalType = (String) n.getProperty(IMAGING_OBJECT_FULL_NAME);

      frameworkTree.insert(fullName, n);
    }

    return frameworkTree;
  }

  /**
   * Extract part of the application considered as utility.
   *
   * @return
   * @throws IOException
   * @throws Neo4jQueryException
   */
  @Override
  public List<FrameworkNode> extractUtilities() throws IOException, Neo4jQueryException {

    List<String> detectedFrameworkPattern = new ArrayList<>();
    List<FrameworkNode> frameworkNodeList = new ArrayList<>();

    // parse list of nodes
    ListIterator<Node> listIterator = toInvestigateNodes.listIterator();
    while (listIterator.hasNext()) {
      Node n = listIterator.next();

      if (!n.hasProperty(IMAGING_OBJECT_FULL_NAME)) continue;
      if (!n.hasProperty(IMAGING_INTERNAL_TYPE)) continue;

      String fullName = (String) n.getProperty(IMAGING_OBJECT_FULL_NAME);
      String internalType = (String) n.getProperty(IMAGING_INTERNAL_TYPE);

      try {
        // Add to framework list
        FrameworkNode fn = findFrameworkNode(fullName, internalType);

        if (fn == null) continue;

        neo4jAL.logInfo(
            String.format(
                "Matching framework : %s for node with name %s", fn.getPattern(), fullName));
        // Apply properties on node
        tagNodeWithFramework(n, fn);

        // Add framework to the list
        if (!detectedFrameworkPattern.contains(fn.getPattern())) {
          frameworkNodeList.add(fn);
          detectedFrameworkPattern.add(fn.getPattern());
        }

        // Remove from investigation List
        listIterator.remove();

      } catch (Exception | Neo4jBadNodeFormatException e) {
        neo4jAL.logError(
            String.format(
                "An exception was thrown trying to find a framework for node with name : %s [%s]",
                fullName, internalType),
            e);
      }
    }

    return frameworkNodeList;
  }

  /**
   * Find the framework node that match the best the object
   *
   * @param fullName FullName of the object to match
   * @param internalType Internal Type of the object
   * @return The Framework node is found, null otherwise
   */
  private FrameworkNode findFrameworkNode(String fullName, String internalType)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {
    return FrameworkController.findMatchingFrameworkByType(neo4jAL, fullName, internalType);
  }

  @Override
  public void extractUnknownApp() {}

  @Override
  public void extractOtherApps() {}

  @Override
  public void extractUnknownNonUtilities() {}
}
