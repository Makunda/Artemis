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

package com.castsoftware.artemis.detector.cobol;

import com.castsoftware.artemis.config.UserConfiguration;
import com.castsoftware.artemis.controllers.UtilsController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.detector.ADetector;
import com.castsoftware.artemis.exceptions.google.GoogleBadResponseCodeException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.exceptions.nlp.NLPBlankInputException;
import com.castsoftware.artemis.sof.famililes.FamiliesFinder;
import com.castsoftware.artemis.sof.famililes.FamilyGroup;
import com.castsoftware.artemis.nlp.SupportedLanguage;
import com.castsoftware.artemis.nlp.model.NLPResults;
import com.castsoftware.artemis.nlp.parser.GoogleResult;
import com.castsoftware.artemis.sof.SystemOfFramework;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.TransactionTerminatedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Detector for COBOL */
public class CobolDetector extends ADetector {

  /**
   * Get the list of internal frameworks by matching the names
   *
   * @param neo4jAL Neo4j Access Layer
   * @param candidates Candidates nodes for grouping
   * @throws Neo4jQueryException
   */
  public static void getInternalFramework(Neo4jAL neo4jAL, List<Node> candidates)
      throws Neo4jQueryException {
    FamiliesFinder finder = new FamiliesFinder(neo4jAL, candidates);
    List<FamilyGroup> fg = finder.findFamilies();

    for (FamilyGroup f : fg) {
      neo4jAL.logInfo(
          String.format(
              "Found a %d objects family with prefix '%s'.",
              f.getFamilySize(), f.getCommonPrefix()));
      f.addDemeterTag(neo4jAL);
    }
  }

  public void createSystemOfFrameworks(List<FrameworkNode> frameworkNodeList) {
    SystemOfFramework sof = new SystemOfFramework(this.neo4jAL, SupportedLanguage.COBOL, this.application, frameworkNodeList);
    sof.run();
  }

  /** Launch the detection */
  @Override
  public List<FrameworkNode> launch() throws IOException, Neo4jQueryException {

    int numTreated = 0;

    // Get configuration
    boolean onlineMode = Boolean.parseBoolean(UserConfiguration.get("artemis.onlineMode"));
    boolean learningMode = Boolean.parseBoolean(UserConfiguration.get("artemis.learning_mode"));

    neo4jAL.logInfo(String.format("Launching artemis detection for Cobol."));
    neo4jAL.logInfo(
        String.format("Investigation launched against %d objects.", toInvestigateNodes.size()));

    List<Node> notDetected = new ArrayList<>();
     // Init the save

    try {
      for (Node n : toInvestigateNodes) {
        // Ignore object without a name property
        if (!n.hasProperty(IMAGING_OBJECT_NAME)) continue;
        String objectName = (String) n.getProperty(IMAGING_OBJECT_NAME);
        String internalType = (String) n.getProperty(IMAGING_INTERNAL_TYPE);

        try {
          // Check if the framework is already known
          FrameworkNode fb = FrameworkNode.findFrameworkByName(neo4jAL, objectName);
          if (fb != null) {
            neo4jAL.logInfo(
                String.format(
                    "The object with name '%s' is already known by Artemis as a '%s'.",
                    objectName, fb.getFrameworkType()));
          }

          // If the Framework is not known and the connection to google still possible, launch the
          // Repositories &
          // the NLP Detection
          // Parse repositories
          // Parse NLP
          if (fb == null && googleParser != null && onlineMode && languageProperties.getOnlineSearch()) {
            GoogleResult gr = googleParser.request(objectName);
            String requestResult = gr.getContent();
            neo4jAL.logInfo(
                    " - Name of the package to search : "
                            + objectName
                            + "\n\t - Results : "
                            + gr.getNumberResult()
                            + "\n\t - Blacklisted : "
                            + gr.isBlacklisted());
            NLPResults nlpResult = nlpEngine.getNLPResult(requestResult);

            // Apply a malus on Node with name containing number, exclude it

            fb = saveFrameworkResult(objectName, nlpResult, internalType);

            if (learningMode) {
              nlpSaver.writeNLPResult(nlpResult.getCategory(), requestResult);
            }
          }

          // Add the framework to the list of it was detected
          if (fb != null) {
            // If flag option is set, apply a demeter tag to the nodes considered as framework
            if (fb.getFrameworkType() == FrameworkType.FRAMEWORK) {
              String cat = fb.getCategory();
              UtilsController.applyDemeterTag(neo4jAL, n, cat);
            } else {
              notDetected.add(n);
            }

            // Increment the number of detection and add it to the result lists
            // fb.incrementNumberDetection();
            frameworkNodeList.add(fb);
            this.reportGenerator.addFrameworkBean(fb);
          }

          numTreated++;
          if (numTreated % 100 == 0) {
            neo4jAL.logInfo(
                String.format(
                    "Investigation on going. Treating node %d/%d.",
                    numTreated, toInvestigateNodes.size()));
          }

        } catch (NLPBlankInputException | Neo4jQueryException | Neo4jBadNodeFormatException e) {
          String message =
              String.format(
                  "The object with name '%s' produced an error during execution.", objectName);
          neo4jAL.logError(message, e);
        } catch (GoogleBadResponseCodeException | IOException e) {
          neo4jAL.logError("Fatal error, the communication with Google API was refused.", e);
          googleParser = null; // remove the google parser
        }
      }


    } catch (TransactionTerminatedException e) {
      neo4jAL.logError("The detection was interrupted. Saving the results...", e);
    } finally {
      reportGenerator.generate(); // generate the report
      nlpSaver.close();
    }

    // Launch internal framework detector on remaining nodes
    if (languageProperties.getInteractionDetector()) {
      //getInternalFramework(neo4jAL, notDetected);
      createSystemOfFrameworks(frameworkNodeList);
    }


    return frameworkNodeList;
  }

  public CobolDetector(Neo4jAL neo4jAL, String application)
      throws IOException, Neo4jQueryException {
    super(neo4jAL, application, SupportedLanguage.COBOL);
  }
}
