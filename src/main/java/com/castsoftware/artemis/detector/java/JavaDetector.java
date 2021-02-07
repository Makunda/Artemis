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

import com.castsoftware.artemis.config.UserConfiguration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.detector.ADetector;
import com.castsoftware.artemis.exceptions.google.GoogleBadResponseCodeException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.exceptions.nlp.NLPBlankInputException;
import com.castsoftware.artemis.nlp.SupportedLanguage;
import com.castsoftware.artemis.nlp.model.NLPResults;
import com.castsoftware.artemis.nlp.parser.GoogleResult;
import com.castsoftware.artemis.sof.utils.SofUtilities;
import org.neo4j.graphdb.Node;

import java.io.IOException;
import java.util.*;

public class JavaDetector extends ADetector {

  private FrameworkTree ft;

  /**
   * Detector for the Java
   *
   * @param neo4jAL Neo4j Access Layer
   * @param application Name of the application
   * @throws IOException
   * @throws Neo4jQueryException
   */
  public JavaDetector(Neo4jAL neo4jAL, String application) throws IOException, Neo4jQueryException {
    super(neo4jAL, application, SupportedLanguage.JAVA);
    this.ft = new FrameworkTree();
  }

  @Override
  public List<FrameworkNode> launch() throws IOException, Neo4jQueryException {
    // Get Dynamic parameters
    boolean onlineMode = Boolean.parseBoolean(UserConfiguration.get("artemis.onlineMode"));
    boolean learningMode = Boolean.parseBoolean(UserConfiguration.get("artemis.learning_mode"));

    // Init properties
    List<FrameworkNode> returnList = new ArrayList<>();

    try {
      FrameworkTree frameworkTree = getBreakdown();
      List<FrameworkTreeLeaf> branchStarterList =
          new ArrayList<>(frameworkTree.getRoot().getChildren());

      List<FrameworkTreeLeaf> toVisit = Collections.synchronizedList(branchStarterList);

      for (ListIterator<FrameworkTreeLeaf> itRel = toVisit.listIterator(); itRel.hasNext(); ) {
        FrameworkTreeLeaf treeLeaf = itRel.next();

        String fullName = treeLeaf.getFullName();
        neo4jAL.logInfo(
            String.format("\nNOW TREATING : %s with depth %d \n\n", fullName, treeLeaf.getDepth()));

        if (treeLeaf.getDepth() < 3) {
          // Add the children
          for (FrameworkTreeLeaf l : treeLeaf.getChildren()) itRel.add(l);
        }

        if (treeLeaf.getDepth() < 2) continue;

        try {

          if (googleParser != null) {
            GoogleResult gr = null;
            gr = googleParser.request(fullName);
            String requestResult = gr.getContent();
            NLPResults nlpResult = nlpEngine.getNLPResult(requestResult);
            neo4jAL.logInfo(
                " - Name of the package to search : "
                    + fullName
                    + "\n\t - Results : "
                    + gr.getNumberResult()
                    + "\n\t - Blacklisted : "
                    + gr.isBlacklisted()
                    + " Results : "
                    + Arrays.toString(nlpResult.getProbabilities()));
          }
        } catch (GoogleBadResponseCodeException | NLPBlankInputException e) {
          neo4jAL.logError(String.format("Failed to query node with name %s", fullName));
        }

        List<String> otherApplications =
            SofUtilities.getPresenceInOtherApplications(neo4jAL, application, fullName);
        neo4jAL.logInfo(
            String.format(
                "The package '%s' is present in %s applications",
                fullName, String.join(", ", otherApplications)));
      }

      neo4jAL.logInfo("\n\n" + "----".repeat(15) + "\n\n");
      frameworkTree.print();
      neo4jAL.logInfo("\n\n" + "----".repeat(15) + "\n\n");

      // Clear residual nodes ( Understand if its a wrapper )

      // Internal uses


      // Analyze Framework tree
      neo4jAL.logInfo("Displaying framework tree.");
      ft.print();

    } catch (Exception e) {
      neo4jAL.logError("An error occurred during the detection.", e);
    }

    // Get the NLP results on undetected

    return returnList;
  }

  @Override
  public FrameworkTree getBreakdown() {
    FrameworkTree frameworkTree = new FrameworkTree();

    // Top Bottom approach
    for (Node n : toInvestigateNodes) {
      if (!n.hasProperty(IMAGING_OBJECT_FULL_NAME)) continue;
      String fullName = (String) n.getProperty(IMAGING_OBJECT_FULL_NAME);
      String objectName = (String) n.getProperty(IMAGING_OBJECT_NAME);
      String internalType = (String) n.getProperty(IMAGING_INTERNAL_TYPE);

      frameworkTree.insert(fullName);
    }

    return frameworkTree;
  }
}
