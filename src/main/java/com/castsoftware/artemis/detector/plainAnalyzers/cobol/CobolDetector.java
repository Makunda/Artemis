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
import com.castsoftware.artemis.detector.statisticalAnalyzers.cobol.CobolStatisticalAnalyzer;
import com.castsoftware.artemis.detector.utils.DetectionCategory;
import com.castsoftware.artemis.detector.utils.DetectorNodesUtil;
import com.castsoftware.artemis.detector.utils.trees.ALeaf;
import com.castsoftware.artemis.detector.utils.trees.TreeFactory;
import com.castsoftware.artemis.detector.utils.trees.cobol.CobolFrameworkTree;
import com.castsoftware.artemis.detector.utils.trees.ATree;
import com.castsoftware.artemis.detector.utils.DetectorTypeMapper;
import com.castsoftware.artemis.detector.utils.DetectorPropertyUtil;
import com.castsoftware.artemis.detector.utils.trees.net.NetFrameworkTreeLeaf;
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

  // Global parameters
  private int savedOnPythia = 0;

  public CobolDetector(Neo4jAL neo4jAL, String application, DetectionParameters detectionParameters)
      throws IOException, Neo4jQueryException {
    super(neo4jAL, application, SupportedLanguage.COBOL, detectionParameters);
  }

  /**
   * Get the list of external nodes in the Cobol application
   * @return
   * @throws Neo4jQueryException
   */
  private List<Node> getCobolExternalNodes() throws Neo4jQueryException {
    String detectionProperty = DetectorPropertyUtil.getDetectionProperty();
    String additionalFilter = String.format(" AND ( obj.FullName STARTS WITH '[Unknown\\\\' OR EXISTS(obj.%s)) ", detectionProperty);
    return DetectorNodesUtil.getExternalObjects(neo4jAL, languageProperties, application, additionalFilter);
  }

  @Override
  public ATree getExternalBreakdown() throws Neo4jQueryException {
    // Get the list of object to treat
    List<Node> nodeList = this.getCobolExternalNodes();
    // Create a treeD
    return TreeFactory.createCobolTree(languageProperties, nodeList);
  }

  @Override
  public ATree getInternalBreakdown() throws Neo4jQueryException {
    // Get the list of object to treat
    List<Node> nodeList = DetectorNodesUtil.getInternalObjects(neo4jAL, languageProperties, application);

    // Create a treeD
    return TreeFactory.createCobolTree(languageProperties, nodeList);
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
    this.addFrameworkToResults(fn);
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
   * @param fn Framework Node
   */
  private void saveOnPythia(FrameworkNode fn) {
    if(!activatedPythia) return; // Pythia is not activated
    if(fn.getFrameworkType() != FrameworkType.FRAMEWORK) return; // Not a Framework

    try {
      PythiaFramework pf = DetectorTypeMapper.artemisFrameworkToPythia(fn, pythiaLanguage);
      PythiaPattern pp = new PythiaPattern(pythiaLanguage, fn.getPattern(), false);
      saveFrameworkOnPythia(pf, Collections.singletonList(pp));
      neo4jAL.logError(String.format("[%s] has been saved on Pythia", fn.getPattern()));
      savedOnPythia ++;
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
  public void extractFrameworks() throws IOException, Neo4jQueryException {

    int numTreated = 0;
    List<Node> toInvestigate = this.getCobolExternalNodes();
    ListIterator<Node> listIterator = toInvestigate.listIterator();

    neo4jAL.logInfo("Launching artemis detection for Cobol.");
    neo4jAL.logInfo(
            String.format("Investigation launched against %d objects.", toInvestigate.size()));

    try {
      Optional<FrameworkType> frameworkType;
      while (listIterator.hasNext()) {

        Node n = listIterator.next();
        frameworkType = this.processNode(n);

        // If a framework has been found, remove it from the list
        if(frameworkType.isPresent() && frameworkType.get() == FrameworkType.FRAMEWORK) listIterator.remove();

        numTreated++;

        if (numTreated % 100 == 0) {
          neo4jAL.logInfo(String.format("Investigation on going. Treating node %d/%d.",numTreated, toInvestigate.size()));
        }
      }

    } catch (TransactionTerminatedException e) {
      neo4jAL.logError("The detection was interrupted. Saving the results...", e);
    } finally {
      reportGenerator.generate(neo4jAL); // generate the report
      nlpSaver.close();
    }

  }

  public void extractUnknownNonUtilities() {
    ListIterator<Node> itNode = toInvestigateNodes.listIterator();
    while (itNode.hasNext()) {
      Node n = itNode.next();
      DetectorPropertyUtil.applyDetectionProperty(n, DetectionCategory.UNKNOWN_NOT_UTILITY);
      // applyDemeterTags(n, "Unknown not utility ", detectionProp.getUnknownNonUtilities());
      itNode.remove();
    }
  }

  @Override
  protected void postLaunch() {
    super.postLaunch();
    this.discoverMissingCode();
    // this.extractUnknownNonUtilities();
  }

  /** Get the name of the core of the application */
  public void discoverMissingCode() {
    try {
      neo4jAL.logInfo(String.format("Getting the core of '%s'", application));


      CobolStatisticalAnalyzer statisticalAnalyzer = new CobolStatisticalAnalyzer(neo4jAL, application, language);
      statisticalAnalyzer.flagCore();

      List<FrameworkNode> results = statisticalAnalyzer.getResults();
      results.forEach(this::addFrameworkToResults);

    } catch (Exception | Neo4jQueryException e) {
      neo4jAL.logError("Statistical analyzer failed to run.", e);
    }
  }

}
