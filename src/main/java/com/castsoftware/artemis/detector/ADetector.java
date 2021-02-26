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

package com.castsoftware.artemis.detector;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.config.detection.DetectionProp;
import com.castsoftware.artemis.config.detection.LanguageConfiguration;
import com.castsoftware.artemis.config.detection.LanguageProp;
import com.castsoftware.artemis.controllers.ApplicationController;
import com.castsoftware.artemis.controllers.UtilsController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.detector.cobol.CobolDetector;
import com.castsoftware.artemis.detector.java.JavaDetector;
import com.castsoftware.artemis.detector.net.NetDetector;
import com.castsoftware.artemis.exceptions.dataset.InvalidDatasetException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.nlp.SupportedLanguage;
import com.castsoftware.artemis.nlp.model.NLPCategory;
import com.castsoftware.artemis.nlp.model.NLPConfidence;
import com.castsoftware.artemis.nlp.model.NLPEngine;
import com.castsoftware.artemis.nlp.model.NLPResults;
import com.castsoftware.artemis.nlp.parser.GoogleParser;
import com.castsoftware.artemis.nlp.saver.NLPSaver;
import com.castsoftware.artemis.pythia.PythiaCom;
import com.castsoftware.artemis.reports.ReportGenerator;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class ADetector {
  // Imaging Properties
  protected static final String ARTEMIS_SEARCH_PREFIX =
      Configuration.get("artemis.tag.prefix_search");
  protected static final String IMAGING_OBJECT_LABEL =
      Configuration.get("imaging.node.object.label");
  protected static final String IMAGING_OBJECT_TAGS =
      Configuration.get("imaging.link.object_property.tags");
  protected static final String IMAGING_OBJECT_NAME = Configuration.get("imaging.node.object.name");
  protected static final String IMAGING_OBJECT_FULL_NAME =
      Configuration.get("imaging.node.object.fullName");
  protected static final String IMAGING_APPLICATION_LABEL =
      Configuration.get("imaging.application.label");
  protected static final String IMAGING_INTERNAL_TYPE =
      Configuration.get("imaging.application.InternalType");

  // Member of the detector
  protected Neo4jAL neo4jAL;
  protected String application;
  protected List<Node> toInvestigateNodes;
  protected ReportGenerator reportGenerator;
  protected List<FrameworkNode> frameworkNodeList;
  protected NLPEngine nlpEngine;
  protected NLPSaver nlpSaver;

  /** Pythia communication * */
  protected PythiaCom pythiaCom;

  protected boolean isPythiaUp = false;
  protected DetectionProp detectionProp;
  protected GoogleParser googleParser;
  protected LanguageProp languageProperties;
  private List<FrameworkNode> pythiaFrameworks;

  /**
   * Detector constructor
   *
   * @param neo4jAL Neo4j Access Layer
   * @param application Name of the application
   * @param language Language
   * @throws IOException
   * @throws Neo4jQueryException
   */
  public ADetector(
      Neo4jAL neo4jAL,
      String application,
      SupportedLanguage language,
      DetectionProp detectionProperties)
      throws IOException, Neo4jQueryException {
    this.neo4jAL = neo4jAL;
    this.application = application;
    this.detectionProp = detectionProperties;

    neo4jAL.logInfo(
        String.format(
            "[1/9] The instantiation of %s detector started.\nThis operation can take up to one minute.",
            language.toString()));

    neo4jAL.logInfo("[2/9] Retrieving the list of candidates nodes...");
    // To investigate nodes
    this.toInvestigateNodes = new ArrayList<>();
    // Shuffle nodes to avoid being bust by the google bot detector
    Collections.shuffle(this.toInvestigateNodes);

    // Pythia Initialization
    neo4jAL.logInfo("[3/9] Try to reach Pythia...");
    this.pythiaCom = PythiaCom.getInstance(neo4jAL);
    this.pythiaFrameworks = new ArrayList<>();
    this.isPythiaUp = this.pythiaCom.getConnected();
    if (!this.isPythiaUp) {
      neo4jAL.logInfo(String.format("[4/9] Failed to reach Pythia."));
    } else {
      neo4jAL.logInfo("[4/9] Connection to Pythia successful.");
    }

    // NLP
    // Make sure the nlp is trained, train it otherwise
    neo4jAL.logInfo(String.format("[5/9] Starting the NLP¨Engine for %s...", language.toString()));
    this.nlpSaver = new NLPSaver(neo4jAL, application, language.toString());
    this.nlpEngine = new NLPEngine(neo4jAL, language);

    Path modelFile = this.nlpEngine.checkIfModelExists();
    if (!Files.exists(modelFile)) {
      this.nlpEngine.train();
    }

    neo4jAL.logInfo("[6/9] Starting the report generator...");
    this.reportGenerator = new ReportGenerator(application);
    neo4jAL.logInfo("[7/9] Starting the Google crawler...");
    this.googleParser = new GoogleParser(neo4jAL);
    this.frameworkNodeList = new ArrayList<>();

    // Configuration
    neo4jAL.logInfo("[8/9] Retrieve information relative to the language...");
    LanguageConfiguration lc = LanguageConfiguration.getInstance();
    this.languageProperties = lc.getLanguageProperties(language.toString());

    getNodes();
    neo4jAL.logInfo("[9/9] The instantiation is successful !");
  }

  /**
   * Get candidates nodes for the detection
   *
   * @throws Neo4jQueryException
   */
  public void getNodes() throws Neo4jQueryException {
    List<String> categories = languageProperties.getObjectsInternalType();
    Result res;

    if (categories.isEmpty()) {
      String forgedRequest =
          String.format(
              "MATCH (obj:Object:`%s`) WHERE  obj.Type in '%s' AND obj.External=true RETURN obj as node",
              application, languageProperties.getName());
      res = neo4jAL.executeQuery(forgedRequest);

      while (res.hasNext()) {
        Map<String, Object> resMap = res.next();
        Node node = (Node) resMap.get("node");
        toInvestigateNodes.add(node);
      }
    } else {
      String forgedRequest =
          String.format(
              "MATCH (obj:Object:`%s`) WHERE  obj.InternalType in $internalTypes AND obj.External=true RETURN obj as node",
              application);
      Map<String, Object> params = Map.of("internalTypes", categories);
      res = neo4jAL.executeQuery(forgedRequest, params);

      while (res.hasNext()) {
        Map<String, Object> resMap = res.next();
        Node node = (Node) resMap.get("node");
        toInvestigateNodes.add(node);
      }
    }
  }

  /**
   * Get the detector based on the language and the application
   *
   * @param neo4jAL Neo4j Access Layer
   * @param application Name of the application
   * @param language Language of the detector
   * @return
   * @throws IOException
   * @throws Neo4jQueryException
   */
  public static ADetector getDetector(
      Neo4jAL neo4jAL,
      String application,
      SupportedLanguage language,
      DetectionProp detectionProperties)
      throws IOException, Neo4jQueryException {

    ADetector aDetector;
    switch (language) {
      case COBOL:
        aDetector = new CobolDetector(neo4jAL, application, detectionProperties);
        break;
      case JAVA:
        aDetector = new JavaDetector(neo4jAL, application, detectionProperties);
        break;
      case NET:
        aDetector = new NetDetector(neo4jAL, application, detectionProperties);
        break;
      default:
        throw new IllegalArgumentException(
            String.format("The language is not currently supported %s", language.toString()));
    }
    return aDetector;
  }

  public abstract ATree getExternalBreakdown();

  /**
   * Apply a the Artemis detection property on a node
   *
   * @param n
   */
  public void applyNodeProperty(Node n, DetectionCategory detectedAs) {
    String artemisProperty = Configuration.get("artemis.node.detection");
    n.setProperty(artemisProperty, detectedAs.toString());
  }

  /**
   * Apply a category to the node
   *
   * @param n
   * @param category
   */
  public void applyCategory(Node n, String category) throws Neo4jQueryException {
    String artemisProperty = Configuration.get("artemis.node.category");
    n.setProperty(artemisProperty, category);
  }

  /**
   * Apply a description to the node
   *
   * @param n
   * @param description
   */
  public void applyDescriptionProperty(Node n, String description) throws Neo4jQueryException {
    String propertyName = Configuration.get("artemis.sub_node.description.property");
    String req =
        "MERGE (o:ObjectProperty { Description : $DescName }) WITH o as subProperty "
            + "MATCH (n) WHERE ID(n)=$IdNode MERGE (subProperty)<-[r:Property]-(n) SET r.value=$DescValue";
    Map<String, Object> params =
        Map.of("DescName", propertyName, "IdNode", n.getId(), "DescValue", description);

    neo4jAL.executeQuery(req, params);
  }

  public void applyOtherApplicationsProperty(Node n, String description)
      throws Neo4jQueryException {
    String propertyName = Configuration.get("artemis.sub_node.in_other_apps.property");
    String req =
        "MERGE (o:ObjectProperty { Description : $DescName }) WITH o as subProperty "
            + "MATCH (n) WHERE ID(n)=$IdNode MERGE (subProperty)<-[r:Property]-(n) SET r.value=$DescValue";
    Map<String, Object> params =
        Map.of("DescName", propertyName, "IdNode", n.getId(), "DescValue", description);

    neo4jAL.executeQuery(req, params);
  }

  /**
   * Apply tag based on the configuration
   *
   * @param n Node to flag
   * @param groupName Name of the group to extract
   * @param arrangementParameters Parameters of the arrangement ( containing the destinations of the
   *     grouping )
   */
  public void applyDemeterTags(Node n, String groupName, List<String> arrangementParameters) {
    if (arrangementParameters.contains("level")) {
      try {
        UtilsController.applyDemeterLevelTag(neo4jAL, n, groupName);
      } catch (Neo4jQueryException e) {
        neo4jAL.logError(
            String.format("Failed to apply demeter level tag on node with Id: %s", n.getId()), e);
      }
    }

    if (arrangementParameters.contains("view")) {
      // Not implemented yet
    }

    if (arrangementParameters.contains("architecture")) {
      try {
        UtilsController.applyDemeterArchitectureTag(neo4jAL, n, groupName);
      } catch (Neo4jQueryException e) {
        neo4jAL.logError(
            String.format(
                "Failed to apply demeter architecture tag on node with Id: %s", n.getId()),
            e);
      }
    }

    if (arrangementParameters.contains("module")) {
      try {
        UtilsController.applyDemeterModuleTag(neo4jAL, n, groupName);
      } catch (Neo4jQueryException e) {
        neo4jAL.logError(
            String.format("Failed to apply demeter module tag on node with Id: %s", n.getId()), e);
      }
    }
  }

  protected boolean isNameExcluded(Node n) {
    if (!n.hasProperty("FullName")) return true;
    String fullName = (String) n.getProperty("FullName");

    for (String regex : detectionProp.getPatternFullNameToExclude()) {
      if (fullName.matches(regex)) return true;
    }

    return false;
  }

  protected boolean isInternalTypeExcluded(Node n) {
    if (!n.hasProperty("InternalType")) return true;
    String internalType = (String) n.getProperty("InternalType");

    for (String regex : detectionProp.getPatternObjectTypeToExclude()) {
      if (internalType.matches(regex)) return true;
    }

    return false;
  }

  /**
   * Launch the detection in the Application
   *
   * @return
   * @throws IOException
   * @throws Neo4jQueryException
   * @throws Neo4jBadRequestException
   */
  public final List<FrameworkNode> launch()
      throws IOException, Neo4jQueryException, Neo4jBadRequestException {

    printConfig();

    // Detection flow
    List<FrameworkNode> frameworkNodes = extractUtilities();
    extractUnknownApp();
    extractOtherApps(); // Search in internal classes
    extractUnknownNonUtilities();

    // Explode fullNames + Communities

    uploadResultToPythia();

    // Add the language detected to the application
    ApplicationController.addLanguage(neo4jAL, application, languageProperties.getName());

    return frameworkNodes;
  }

  public void printConfig() {
    neo4jAL.logInfo(
        "_________________________________________________________________________________");
    neo4jAL.logInfo("| -----------------  Artemis parameters  ----------------------- ");
    neo4jAL.logInfo(
        String.format(
            "| Detection launched on application                      : %s ", application));
    neo4jAL.logInfo(
        String.format(
            "| Language of the detection is                           : %s ",
            languageProperties.getName()));
    neo4jAL.logInfo(
        String.format(
            "| Online Mode (Google search, repository, etc..) set on  : %s ", getOnlineMode()));
    neo4jAL.logInfo(
        String.format(
            "| Persistent Mode set on                                 : %s ", getPersistentMode()));
    neo4jAL.logInfo(
        String.format(
            "| Learning mode set on                                   : %s ", getLearningMode()));
    neo4jAL.logInfo(
        String.format(
            "| Candidates for the detection (before filtering)        : %s ",
            toInvestigateNodes.size()));
    neo4jAL.logInfo("| ----------------  Detection parameters  ---------------------- ");
    neo4jAL.logInfo(
        String.format(
            "| Known Utilities will be extracted to                   : %s ",
            String.join(",", detectionProp.getKnownUtilities())));
    neo4jAL.logInfo(
        String.format(
            "| Known non-utilities will be extracted to               : %s ",
            String.join(",", detectionProp.getKnownNotUtilities())));
    neo4jAL.logInfo(
        String.format(
            "| Utilities in other applications will be extracted to   : %s ",
            String.join(",", detectionProp.getInOtherApplication())));
    neo4jAL.logInfo(
        String.format(
            "| Potentially missing code will be extracted to          : %s ",
            String.join(",", detectionProp.getPotentiallyMissing())));
    neo4jAL.logInfo(
        String.format(
            "| Unknown utilities will be extracted to                 : %s ",
            String.join(",", detectionProp.getUnknownUtilities())));
    neo4jAL.logInfo(
        String.format(
            "| Unknown non-utilities will be extracted to             : %s ",
            String.join(",", detectionProp.getUnknownNonUtilities())));
    neo4jAL.logInfo("| ----------------  Exclusions parameters  --------------------- ");
    neo4jAL.logInfo(
        String.format(
            "| Exclusion on fullName                                  : %s ",
            String.join(",", detectionProp.getPatternFullNameToExclude())));
    neo4jAL.logInfo(
        String.format(
            "| Exclusion on object type                               : %s ",
            String.join(",", detectionProp.getPatternObjectTypeToExclude())));
    neo4jAL.logInfo(
        "__________________________________________________________________________________");
  }

  /**
   * Extract utilities
   *
   * @return List of findings
   */
  public abstract List<FrameworkNode> extractUtilities() throws IOException, Neo4jQueryException;

  /** Extract unknown non utilities */
  public abstract void extractUnknownApp();

  /** Extract unknown non utilities */
  public abstract void extractOtherApps();

  /** Extract unknown non utilities */
  public abstract void extractUnknownNonUtilities();

  /** Upload the finding to Pythia */
  private void uploadResultToPythia() {
    if (!isPythiaUp) {
      neo4jAL.logInfo("Pythia is unreachable. The upload of the result was skipped.");
      return;
    }
    ;

    // Filter the frameworks discovered by Pythia
    for (FrameworkNode fn : frameworkNodeList) {
      if (!pythiaFrameworks.contains(fn)) { // Upload only new ones
        pythiaCom.sendFramework(fn);
      }
    }
  }

  public boolean getOnlineMode() {
    String config = Configuration.getBestOfAllWorlds(neo4jAL, "artemis.onlineMode"); // Get configuration
    if (config != null && config.equals("true")) return true;
    return false;
  }

  public boolean getPersistentMode() {
    return Boolean.parseBoolean(Configuration.get("artemis.persistent_mode"));
  }

  public boolean getLearningMode() {
    return Boolean.parseBoolean(Configuration.get("artemis.learning_mode"));
  }

  /**
   * Parse pythia to find if the object matches the name and the internal type of an existing
   * framework
   *
   * @param name
   * @param internalType
   * @return
   */
  protected FrameworkNode findFrameworkOnPythia(String name, String internalType) {
    if (!isPythiaUp) return null;

    FrameworkNode fn = pythiaCom.findFramework(name, internalType);

    if (fn != null) {
      pythiaFrameworks.add(fn);
    }

    return fn;
  }

  /**
   * Save NLP Results to the Artemis Database. The target database will be decided depending on the
   * value of
   *
   * @param name Name of the object to save
   * @param results Results of the NLP Engine
   * @throws InvalidDatasetException
   */
  protected FrameworkNode saveFrameworkResult(String name, NLPResults results, String internalType)
      throws Neo4jQueryException {

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    Date date = Calendar.getInstance().getTime();
    String strDate = dateFormat.format(date);

    FrameworkType fType = null;

    if (results.getConfidence()
        == NLPConfidence.NOT_CONFIDENT) { // If the confidence score is not high enough it
      // will be added on the to investigate dt
      fType = FrameworkType.TO_INVESTIGATE;
    } else if (results.getCategory() == NLPCategory.FRAMEWORK) { // Detected as a framework
      fType = FrameworkType.FRAMEWORK;
    } else { // Detected as a not framework
      fType = FrameworkType.NOT_FRAMEWORK;
    }

    // Retrieve highest detection score
    double[] prob = results.getProbabilities();
    double detectionScore = 0.0;
    if (prob.length > 0) {
      detectionScore = prob[0];
    }

    FrameworkNode fb =
        new FrameworkNode(
            neo4jAL,
            name,
            strDate,
            "No location discovered",
            "",
            1L,
            detectionScore,
            new Date().getTime());
    fb.setFrameworkType(fType);
    fb.setInternalTypes(languageProperties.getObjectsInternalType());

    // Save the Node to the local database
    if (getPersistentMode()) {
      fb.createNode();
    }

    return fb;
  }
}
