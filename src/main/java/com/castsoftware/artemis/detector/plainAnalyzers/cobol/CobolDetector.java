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

package com.castsoftware.artemis.detector.plainAnalyzers.cobol;

import com.castsoftware.artemis.config.detection.DetectionParameters;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.detector.plainAnalyzers.ADetector;
import com.castsoftware.artemis.detector.utils.DetectionCategory;
import com.castsoftware.artemis.detector.utils.trees.cobol.CobolFrameworkTree;
import com.castsoftware.artemis.detector.utils.trees.ATree;
import com.castsoftware.artemis.detector.utils.DetectorTypeMapper;
import com.castsoftware.artemis.detector.utils.DetectorPropertyUtil;
import com.castsoftware.artemis.exceptions.google.GoogleBadResponseCodeException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.exceptions.nlp.NLPBlankInputException;
import com.castsoftware.artemis.global.SupportedLanguage;
import com.castsoftware.artemis.modules.nlp.model.NLPResults;
import com.castsoftware.artemis.modules.nlp.parser.GoogleParser;
import com.castsoftware.artemis.modules.nlp.parser.GoogleResult;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaFramework;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaImagingFramework;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaPattern;
import com.castsoftware.artemis.modules.sof.SystemOfFramework;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.TransactionTerminatedException;

import java.io.IOException;
import java.util.*;

/** Detector for COBOL Internal Detection : OK Find framework locally : OK Pythia : OK */
public class CobolDetector extends ADetector {


  public CobolDetector(Neo4jAL neo4jAL, String application, DetectionParameters detectionParameters)
      throws IOException, Neo4jQueryException {
    super(neo4jAL, application, SupportedLanguage.COBOL, detectionParameters);
  }

  /**
   * Create a Cobol Tree using cobol programs names
   *
   * @param nodeList List of nodes
   * @return
   */
  public CobolFrameworkTree createTree(List<Node> nodeList) {
    CobolFrameworkTree frameworkTree = new CobolFrameworkTree(languageProperties);

    // Top Bottom approach
    String fullName;
    ListIterator<Node> listIterator = nodeList.listIterator();
    while (listIterator.hasNext()) {
      Node n = listIterator.next();

      // Get cobol class
      if (!n.hasProperty("Type") || !n.getProperty("Type").equals("Cobol Program")) {
        continue;
      }

      if (!n.hasProperty(IMAGING_OBJECT_NAME)) continue;
      fullName = (String) n.getProperty(IMAGING_OBJECT_NAME);
      frameworkTree.insert(fullName, n);
    }

    return frameworkTree;
  }

  @Override
  public ATree getExternalBreakdown() {
    return createTree(toInvestigateNodes);
  }

  @Override
  public ATree getInternalBreakdown() {
    return null;
  }

  /**
   * Find a cobol utility on Pythia
   *
   * @param name Name of the utility to search
   * @return The Framework node or null
   */
  private Optional<FrameworkNode> findUtilityOnPythia(String name) {

    // If not activated, return not found
    if (!activatedPythia) return Optional.empty();

    Optional<PythiaImagingFramework> framework =
            this.findFrameworkOnPythia(name); // Find the framework

    if(framework.isEmpty()) return Optional.empty(); // Framework not found

    // Framework found, return framework node
    FrameworkNode fn = DetectorTypeMapper.imagingFrameworkToFrameworkNode(neo4jAL, framework.get(), false);
    return Optional.of(fn);
  }

  /**
   * Process a Candidate node
   * @param n Node to investigate
   * @return The Type detected
   */
  private Optional<FrameworkType> processNode(Node n) {

    // Ignore object without a name property
    if (!n.hasProperty(IMAGING_OBJECT_NAME)) return Optional.empty();

    // Get properties
    String objectName = (String) n.getProperty(IMAGING_OBJECT_NAME);
    String internalType = (String) n.getProperty(IMAGING_INTERNAL_TYPE);

    // Check if the framework is already known
    Optional<FrameworkNode> fb = Optional.empty();

    // Find the Framework on the local Database
    try {
      fb = FrameworkNode.findFrameworkByName(neo4jAL, objectName);
    } catch (Neo4jQueryException | Neo4jBadNodeFormatException e) {
       neo4jAL.logError(String.format("Failed to find framework with name '%s'. Produced an exception.", objectName), e);
    }

    if(fb.isEmpty()) {
      // Check on Pythia
      fb = this.findUtilityOnPythia(objectName);
    }

    // If the Framework is not known and the connection to google still possible, query google
    if (fb.isEmpty()) {
      fb = this.googleSearch(objectName, internalType);
    }

    // Add the framework to the list of it was detected
    if (fb.isEmpty()) return Optional.empty();

    // The node exists
    FrameworkNode fn = fb.get();

    // If flag option is set, apply a demeter tag to the nodes considered as framework
    if (fn.getFrameworkType() == FrameworkType.FRAMEWORK) {
      try {
        tagNodeWithFramework(n, fn);
      } catch (Neo4jQueryException e) {
        neo4jAL.logError(String.format("Failed to flag utility [%s] after the detection.", objectName), e);
      }
    }

    // Increment the number of detection and add it to the result lists
    frameworkNodeList.add(fn);
    this.reportGenerator.addFrameworkBean(fn);
    return Optional.of(fn.getFrameworkType());
  }

  /**
   * Search for results on google
   * @param objectName Name of the object
   * @param internalType Type of the object
   * @return Optional returning a framework node
   */
  private Optional<FrameworkNode> googleSearch(String objectName, String internalType) {
    // Start the google Search
    neo4jAL.logInfo(String.format("Requesting on google : %s", objectName));

    // Check the configuration
    if(!getOnlineMode() || googleParser == null || !languageProperties.getOnlineSearch()) {
      neo4jAL.logInfo("Requesting on google has been cancelled due to the configuration.");
      return Optional.empty();
    }

    // Request on google
    try {
      GoogleResult gr = googleParser.request(objectName);
      String requestResult = gr.getContent();
      NLPResults nlpResult = nlpEngine.getNLPResult(requestResult);
      neo4jAL.logInfo(
              String.format("Results for %s : %s.", objectName, nlpResult.toString()));

      FrameworkNode fn  = saveNLPFrameworkResult(objectName, nlpResult, internalType);
      fn.updateDetectionData(requestResult);
      fn.updateLocation(GoogleParser.getBestUrl(languageProperties, gr.getUrls()));

      if (getLearningMode()) {
        nlpSaver.writeNLPResult(nlpResult.getCategory(), requestResult);
      }

      // Send the framework on Pythia if it's a Framework
      this.saveOnPythia(fn);

      // Persist the Framework locally
      this.persistFramework(fn);

      return Optional.of(fn);
    } catch (NLPBlankInputException | IOException | Neo4jQueryException  e) {
      neo4jAL.logError(String.format("Failed to query Google with Object name '%s'.", objectName), e);
      return Optional.empty();
    } catch ( GoogleBadResponseCodeException e) {
      // Fatal error, the Google refused the connection
      this.googleParser = null;
      neo4jAL.logError("The Google API now refused the connection due to too many request. Good luck.", e);
      return Optional.empty();
    }
  }



  /**
   * Save a COBOL Framework node on pythia
   * @param fn
   */
  private void saveOnPythia(FrameworkNode fn) {
    if(!activatedPythia) return; // Pythia is not activated
    if(fn.getFrameworkType() != FrameworkType.FRAMEWORK) return; // Not a Framework

    try {
      PythiaFramework pf = DetectorTypeMapper.artemisFrameworkToPythia(fn, pythiaLanguage);
      PythiaPattern pp = new PythiaPattern(pythiaLanguage, fn.getPattern(), false);
      saveFrameworkOnPythia(pf, Collections.singletonList(pp));
    } catch (Exception e) {
      neo4jAL.logError("Failed to save the cobol utility on Pythia.", e);
    }
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

    neo4jAL.logInfo("Launching artemis detection for Cobol.");
    neo4jAL.logInfo(
        String.format("Investigation launched against %d objects.", toInvestigateNodes.size()));

    ListIterator<Node> listIterator = toInvestigateNodes.listIterator();
    try {
      Optional<FrameworkType> frameworkType;
      while (listIterator.hasNext()) {

        Node n = listIterator.next();
        frameworkType = this.processNode(n);

        // If a framework has been found, remove it from the list
        if(frameworkType.isPresent() && frameworkType.get() == FrameworkType.FRAMEWORK) listIterator.remove();

        numTreated++;

        if (numTreated % 100 == 0) {
          neo4jAL.logInfo(String.format("Investigation on going. Treating node %d/%d.",numTreated, toInvestigateNodes.size()));
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
      neo4jAL.logInfo("Extracting potential missing code.");
      ListIterator<Node> itNode = toInvestigateNodes.listIterator();
      int count = 0;
      while (itNode.hasNext()) {

        if (count % 100 == 0)
          neo4jAL.logInfo(
              String.format(
                  "Investigation on going. Treating node %d/%d.",
                  count, toInvestigateNodes.size()));

        count++;
        Node n = itNode.next();
        if (!n.hasProperty("Name")) continue;

        String name = (String) n.getProperty("Name");

        // The name match the nGram
        if (name.startsWith(corePrefix)) {
          DetectorPropertyUtil.applyNodeProperty(n, DetectionCategory.MISSING_CODE);
          // applyDemeterTags(n, "Missing code", detectionProp.getPotentiallyMissing());
          itNode.remove();
        }
      }

    } catch (Neo4jQueryException e) {
      neo4jAL.logError("Failed to retrieve the core of the application.", e);
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
        DetectorPropertyUtil.applyNodeProperty(en.getKey(), DetectionCategory.IN_OTHERS_APPLICATIONS);
        applyOtherApplicationsProperty(en.getKey(), groupName);
        // applyDemeterTags(
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
      DetectorPropertyUtil.applyNodeProperty(n, DetectionCategory.UNKNOWN_NOT_UTILITY);
      // applyDemeterTags(n, "Unknown not utility ", detectionProp.getUnknownNonUtilities());
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

      // TODO : Continue here and verify the length before any operation
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

    int errors = 0;
    while (itNode.hasNext()) {
      Node n = itNode.next();
      try {

        if (!n.hasProperty("FullName")) {
          itNode.remove(); // Remove the node
          errors++;
          continue;
        };

        String name = (String) n.getProperty("FullName");
        String internalType = (String) n.getProperty("InternalType");

        if (!name.contains("Unknown") || internalType.equals("false")) {
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
      } catch (Exception ignored) {
        itNode.remove(); // Remove the node
        errors++;
      }
    }

    neo4jAL.logInfo(
        String.format(
            "Filtering : %d nodes were removed. %d nodes remaining. Produced [%d] errors (skipped).",
            removed, toInvestigateNodes.size(), errors));
  }

  public void createSystemOfFrameworks(List<FrameworkNode> frameworkNodeList) {
    SystemOfFramework sof =
        new SystemOfFramework(
            this.neo4jAL, SupportedLanguage.COBOL, this.application, frameworkNodeList);
    sof.run();
  }
}
