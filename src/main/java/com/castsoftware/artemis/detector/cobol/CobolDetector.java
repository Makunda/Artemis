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

import com.castsoftware.artemis.config.detection.DetectionProp;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.detector.ADetector;
import com.castsoftware.artemis.detector.ATree;
import com.castsoftware.artemis.detector.DetectionCategory;
import com.castsoftware.artemis.detector.java.FrameworkTreeLeaf;
import com.castsoftware.artemis.exceptions.google.GoogleBadResponseCodeException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.exceptions.nlp.NLPBlankInputException;
import com.castsoftware.artemis.nlp.SupportedLanguage;
import com.castsoftware.artemis.nlp.model.NLPResults;
import com.castsoftware.artemis.nlp.parser.GoogleParser;
import com.castsoftware.artemis.nlp.parser.GoogleResult;
import com.castsoftware.artemis.sof.SystemOfFramework;
import com.castsoftware.artemis.sof.famililes.FamiliesFinder;
import com.castsoftware.artemis.sof.famililes.FamilyGroup;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.TransactionTerminatedException;

import java.io.IOException;
import java.util.*;

/** Detector for COBOL Internal Detection : OK Find framework locally : OK Pythia : OK */
public class CobolDetector extends ADetector {

  private final List<Node> unknownNonUtilities = new ArrayList<>();

  public CobolDetector(Neo4jAL neo4jAL, String application, DetectionProp detectionProperties)
      throws IOException, Neo4jQueryException {
    super(neo4jAL, application, SupportedLanguage.COBOL, detectionProperties);
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

  /**
   * Process the external candidates
   *
   * @throws IOException
   */
  @Override
  public List<FrameworkNode> extractUtilities() throws IOException {
    filterNodes();

    int numTreated = 0;

    neo4jAL.logInfo(String.format("Launching artemis detection for Cobol."));
    neo4jAL.logInfo(
        String.format("Investigation launched against %d objects.", toInvestigateNodes.size()));

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
          FrameworkNode fb =
              FrameworkNode.findFrameworkByNameAndType(neo4jAL, objectName, internalType);
          if (fb != null) {
            neo4jAL.logInfo(
                String.format(
                    "The object with name '%s' is already known by Artemis as a '%s'.",
                    objectName, fb.getFrameworkType()));
          } else {
            if (getPythiaMode() && isPythiaUp) fb = findFrameworkOnPythia(objectName, internalType); // Check on pythia
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

            fb = saveFrameworkResult(objectName, nlpResult, internalType);
            fb.updateDetectionData(requestResult);
            fb.updateLocation(GoogleParser.getBestUrl(languageProperties, gr.getUrls()));

            if (getLearningMode()) {
              nlpSaver.writeNLPResult(nlpResult.getCategory(), requestResult);
            }
          }

          // Add the framework to the list of it was detected
          if (fb != null) {
            // If flag option is set, apply a demeter tag to the nodes considered as framework
            if (fb.getFrameworkType() == FrameworkType.FRAMEWORK) {
              String cat = fb.getCategory();
              String description = fb.getDescription();
              applyNodeProperty(n, DetectionCategory.KNOWN_UTILITY);
              applyDescriptionProperty(n, description);
              applyCategory(n, cat);
              //applyDemeterTags(n, cat, detectionProp.getKnownUtilities());

              listIterator.remove(); // Remove the node from the to investigate list
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
      reportGenerator.generate(neo4jAL); // generate the report
      nlpSaver.close();
    }

    return frameworkNodeList;
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
          applyNodeProperty(n, DetectionCategory.MISSING_CODE);
          //applyDemeterTags(n, "Missing code", detectionProp.getPotentiallyMissing());
          itNode.remove();
        }
      }

    } catch (Neo4jQueryException e) {
      neo4jAL.logError("Failed to retrieve the core of the application.", e);
      return;
    }
  }

  @Override
  public void extractOtherApps() {

    ListIterator<Node> itNode = toInvestigateNodes.listIterator();
    Map<Node, List<String>> nodeListMap = new HashMap<>();

    while (itNode.hasNext()) {
      try {

        Node n = itNode.next();
        if (!n.hasProperty("Name") || !n.hasProperty("InternalType")) continue;

        String name = (String) n.getProperty("Name");
        String internalType = (String) n.getProperty("InternalType");

        String req =
            "MATCH (o:Object) WHERE NOT $appName in LABELS(o) AND o.Name=$nodeName AND o.InternalType=$internalType AND o.External=False "
                + "RETURN [ x in LABELS(o) WHERE NOT x='Object'][0] as app";
        Map<String, Object> params =
            Map.of("appName", application, "nodeName", name, "internalType", internalType);

        Result res = neo4jAL.executeQuery(req, params);
        if (res.hasNext()) {
          List<String> appList = new ArrayList<>();
          while (res.hasNext()) {
            appList.add((String) res.next().get("app"));
          }
          nodeListMap.put(n, appList);
          itNode.remove();
        }

      } catch (Neo4jQueryException e) {
        neo4jAL.logError(
            String.format("Failed to extract node with name %s to  Unknown other applications", e));
      }
    }

    for (Map.Entry<Node, List<String>> en : nodeListMap.entrySet()) {
      String groupName = String.format("[%s]", String.join(", ", en.getValue()));
      try {
        applyNodeProperty(en.getKey(), DetectionCategory.IN_OTHERS_APPLICATIONS);
        applyOtherApplicationsProperty(en.getKey(), groupName);
        //applyDemeterTags(
        //    en.getKey(), "In applications " + groupName, detectionProp.getInOtherApplication());
      } catch (Neo4jQueryException e) {
        neo4jAL.logError("Failed to create a 'other application' property", e);
      }
    }
  }

  @Override
  public void extractUnknownNonUtilities() {
    ListIterator<Node> itNode = toInvestigateNodes.listIterator();
    while (itNode.hasNext()) {
      Node n = itNode.next();
      applyNodeProperty(n, DetectionCategory.UNKNOWN_NOT_UTILITY);
      //applyDemeterTags(n, "Unknown not utility ", detectionProp.getUnknownNonUtilities());
      itNode.remove();
    }
  }

  /** Get the name of the core of the application */
  public String getCoreApplication() throws Neo4jQueryException {
    neo4jAL.logInfo("Getting the core of");
    String req =
        String.format(
            "MATCH (o:Object:`%s`) WHERE o.InternalType in $internalTypes AND o.External=False "
                + "RETURN o.Name as name",
            application);
    Map<String, Object> params =
        Map.of("internalTypes", languageProperties.getObjectsInternalType().toArray(new String[0]));

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

    neo4jAL.logInfo("Suspected application core under : " + corePrefix);

    return corePrefix;
  }

  /** Filter and look only for unknowns utilities */
  private void filterNodes() {
    int removed = 0;
    ListIterator<Node> itNode = toInvestigateNodes.listIterator();

    while (itNode.hasNext()) {
      Node n = itNode.next();
      if (!n.hasProperty("FullName")) continue;

      String name = (String) n.getProperty("FullName");
      String internalType = (String) n.getProperty("InternalType");

      if (!name.contains("Unknown")) {
        // If the name is flag as an utility , extract it, if not in is own category
        itNode.remove();
        removed++;
      }

      if (isNameExcluded(n)) {
        // If the name match unauthorized regex
        itNode.remove();
        removed++;
      }

      if (isInternalTypeExcluded(n)) {
        // If the name match unauthorized regex
        itNode.remove();
        removed++;
      }
    }

    neo4jAL.logInfo(
        String.format(
            "Filtering : %d nodes were removed. %d nodes remaining.",
            removed, toInvestigateNodes.size()));
  }

  public void createSystemOfFrameworks(List<FrameworkNode> frameworkNodeList) {
    SystemOfFramework sof =
        new SystemOfFramework(
            this.neo4jAL, SupportedLanguage.COBOL, this.application, frameworkNodeList);
    sof.run();
  }
}
