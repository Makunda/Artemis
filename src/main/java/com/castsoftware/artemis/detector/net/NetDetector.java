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

import com.castsoftware.artemis.config.detection.DetectionProp;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.detector.ADetector;
import com.castsoftware.artemis.detector.ATree;
import com.castsoftware.artemis.detector.java.FrameworkTree;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.nlp.SupportedLanguage;
import org.neo4j.graphdb.Node;

import java.io.IOException;
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
  public NetDetector(Neo4jAL neo4jAL, String application)
      throws IOException, Neo4jQueryException {
    super(neo4jAL, application, SupportedLanguage.NET);
  }

  @Override
  public ATree getExternalBreakdown() {
    FrameworkTree frameworkTree = new FrameworkTree();

    // Top Bottom approach
    ListIterator<Node> listIterator = toInvestigateNodes.listIterator();
    while (listIterator.hasNext()) {
      Node n = listIterator.next();
      // Get node in Java Classes
      if (!n.hasProperty("Level") ||
              (!((String) n.getProperty("Level")).equals("C# Class") &&
              !((String) n.getProperty("Level")).equals(".NET Class"))) continue;

      if (!n.hasProperty(IMAGING_OBJECT_FULL_NAME)) continue;
      String fullName = (String) n.getProperty(IMAGING_OBJECT_FULL_NAME);

      frameworkTree.insert(fullName);
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
      if (!n.hasProperty("Level") ||
              (!((String) n.getProperty("Level")).equals("C# Class") &&
                      !((String) n.getProperty("Level")).equals(".NET Class"))) continue;

      if (!n.hasProperty(IMAGING_OBJECT_FULL_NAME)) continue;
      String fullName = (String) n.getProperty(IMAGING_OBJECT_FULL_NAME);

      frameworkTree.insert(fullName);
    }

    return frameworkTree;
  }

  @Override
  public List<FrameworkNode> extractUtilities() throws IOException, Neo4jQueryException {
    return null;
  }

  @Override
  public void extractUnknownApp() {}

  @Override
  public void extractOtherApps() {}

  @Override
  public void extractUnknownNonUtilities() {}
}
