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

import com.castsoftware.artemis.controllers.UtilsController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.detector.ADetector;
import com.castsoftware.artemis.detector.ATree;
import com.castsoftware.artemis.exceptions.google.GoogleBadResponseCodeException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.exceptions.nlp.NLPBlankInputException;
import com.castsoftware.artemis.nlp.SupportedLanguage;
import com.castsoftware.artemis.nlp.model.NLPResults;
import com.castsoftware.artemis.nlp.parser.GoogleResult;
import com.castsoftware.artemis.sof.SystemOfFramework;
import com.castsoftware.artemis.sof.famililes.FamiliesFinder;
import com.castsoftware.artemis.sof.famililes.FamilyGroup;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.TransactionTerminatedException;

import java.io.IOException;
import java.util.*;

/** Detector for COBOL */
public class CobolDetector extends ADetector {

  private final List<Node> unknownNonUtilities = new ArrayList<>();
  private final List<Node> otherApps = new ArrayList<>();
  private final List<Node> unknownApp = new ArrayList<>();

  public CobolDetector(Neo4jAL neo4jAL, String application)
      throws IOException, Neo4jQueryException {
    super(neo4jAL, application, SupportedLanguage.COBOL);
  }

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

  @Override
  public ATree getExternalBreakdown() {
    return null;
  }

  @Override
  public void extractUnknownNonUtilities() {
    ListIterator<Node> itNode = toInvestigateNodes.listIterator();
    while (itNode.hasNext()) {
      Node n = itNode.next();
      try {
        UtilsController.applyDemeterParentTag(neo4jAL, n, ": Unknowns Non Utilities");
      } catch (Neo4jQueryException e) {
        neo4jAL.logError(
            String.format("Failed to extract node with name %s to Unknown Non Utilities", e));
      }
      itNode.remove();
    }
  }

  @Override
  public void extractOtherApps() {

    ListIterator<Node> itNode = toInvestigateNodes.listIterator();
    while (itNode.hasNext()) {
      try {

        Node n = itNode.next();
        if (!n.hasProperty("Name") || !n.hasProperty("InternalType")) continue;

        String name = (String) n.getProperty("Name");
        String internalType = (String) n.getProperty("InternalType");

        String req =
            "MATCH (o:Object) WHERE NOT $appName in LABELS(o) AND o.Name=$nodeName AND o.InternalType=$internalType "
                + "RETURN [ x in LABELS(o) WHERE NOT x='Object'][0] as app";
        Map<String, Object> params =
            Map.of("appName", application, "nodeName", name, "internalType", internalType);

        Result res = neo4jAL.executeQuery(req, params);
        if (res.hasNext()) {
          UtilsController.applyDemeterParentTag(neo4jAL, n, " : Unknowns other Applications");
          itNode.remove();
        }

      } catch (Neo4jQueryException e) {
        neo4jAL.logError(
            String.format("Failed to extract node with name %s to  Unknown other applications", e));
      }
    }
  }

  @Override
  public void extractUnknownApp() {
    try {
      String corePrefix = getCoreApplication();
      if (corePrefix.isBlank()) return;

      // If the object match the Ngram
      ListIterator<Node> itNode = toInvestigateNodes.listIterator();
      while (itNode.hasNext()) {
        Node n = itNode.next();
        if (!n.hasProperty("Name")) continue;

        String name = (String) n.getProperty("Name");

        // The name match the nGram
        if (name.startsWith(corePrefix)) {
          UtilsController.applyDemeterTag(neo4jAL, n, "Unknown app");
          itNode.remove();
        }
      }

    } catch (Neo4jQueryException e) {
      neo4jAL.logError("Failed to retrieve the core of the application.", e);
      return;
    }
  }

  /** Get the name of the core of the application */
  public String getCoreApplication() throws Neo4jQueryException {
    String req =
        String.format(
            "MATCH (o:Object:`%s`) WHERE o.InternalType in $internalType AND o.External=False "
                + "RETURN o.Name as name",
            application);
    Map<String, Object> params =
        Map.of("internalType", languageProperties.getObjectsInternalType());

    Result result = neo4jAL.executeQuery(req, params);
    Map<String, Integer> mapName = new HashMap<>();

    int nGram = 3;
    while (result.hasNext()) {
      String name = (String) result.next().get("name");

      String gram = name.substring(0, nGram);
      if (!mapName.containsKey(gram)) mapName.put(gram, 0);
      mapName.computeIfPresent(gram, (key, val) -> val + 1);
    }

    // Get the core name of the application
    Integer max = 0;
    String corePrefix = "";
    for (Map.Entry<String, Integer> en : mapName.entrySet()) {
      if (max < en.getValue()) corePrefix = en.getKey();
    }

    return corePrefix;
  }

  /**
   * Process the external candidates
   *
   * @throws IOException
   */
  public List<FrameworkNode> treatExternals() throws IOException {
    int numTreated = 0;

    neo4jAL.logInfo(String.format("Launching artemis detection for Cobol."));
    neo4jAL.logInfo(
        String.format("Investigation launched against %d objects.", toInvestigateNodes.size()));

    List<Node> notDetected = new ArrayList<>();
    // Init the save

    ListIterator<Node> listIterator = toInvestigateNodes.listIterator();
    try {
      while (listIterator.hasNext()) {

        Node n = listIterator.next();
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
          if (fb == null
              && googleParser != null
              && getOnlineMode()
              && languageProperties.getOnlineSearch()) {

            GoogleResult gr = googleParser.request(objectName);
            String requestResult = gr.getContent();
            NLPResults nlpResult = nlpEngine.getNLPResult(requestResult);

            // Apply a malus on Node with name containing number, exclude it

            fb = saveFrameworkResult(objectName, nlpResult, internalType);

            if (getLearningMode()) {
              nlpSaver.writeNLPResult(nlpResult.getCategory(), requestResult);
            }
          }

          // Add the framework to the list of it was detected
          if (fb != null) {
            // If flag option is set, apply a demeter tag to the nodes considered as framework
            if (fb.getFrameworkType() == FrameworkType.FRAMEWORK) {
              String cat = fb.getCategory();
              UtilsController.applyDemeterTag(neo4jAL, n, cat);
              listIterator.remove(); // Remove the node from the to investigate list
            } else {
              unknownNonUtilities.add(n);
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

    return frameworkNodeList;
  }

  public void createSystemOfFrameworks(List<FrameworkNode> frameworkNodeList) {
    SystemOfFramework sof =
        new SystemOfFramework(
            this.neo4jAL, SupportedLanguage.COBOL, this.application, frameworkNodeList);
    sof.run();
  }

  /** Launch the detection */
  @Override
  public List<FrameworkNode> launch() throws IOException, Neo4jQueryException {

    List<FrameworkNode> frameworkNodes = treatExternals();
    extractUnknownApp();
    extractOtherApps();
    extractUnknownNonUtilities();

    // Launch internal framework detector on remaining nodes
    if (languageProperties.getInteractionDetector()) {
      // getInternalFramework(neo4jAL, notDetected);
      createSystemOfFrameworks(frameworkNodeList);
    }

    return frameworkNodeList;
  }
}
