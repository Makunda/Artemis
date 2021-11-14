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

package com.castsoftware.artemis.detector.plainAnalyzers;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.config.detection.DetectionParameters;
import com.castsoftware.artemis.config.detection.LanguageConfiguration;
import com.castsoftware.artemis.config.detection.LanguageProp;
import com.castsoftware.artemis.controllers.ApplicationController;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.detector.utils.DetectionCategory;
import com.castsoftware.artemis.detector.utils.trees.ATree;
import com.castsoftware.artemis.detector.utils.DetectorNodesUtil;
import com.castsoftware.artemis.detector.utils.DetectorPropertyUtil;
import com.castsoftware.artemis.exceptions.dataset.InvalidDatasetException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.global.SupportedLanguage;
import com.castsoftware.artemis.modules.nlp.model.NLPCategory;
import com.castsoftware.artemis.modules.nlp.model.NLPConfidence;
import com.castsoftware.artemis.modules.nlp.model.NLPEngine;
import com.castsoftware.artemis.modules.nlp.model.NLPResults;
import com.castsoftware.artemis.modules.nlp.parser.GoogleParser;
import com.castsoftware.artemis.modules.nlp.saver.NLPSaver;
import com.castsoftware.artemis.modules.pythia.Pythia;
import com.castsoftware.artemis.modules.pythia.exceptions.PythiaException;
import com.castsoftware.artemis.modules.pythia.exceptions.PythiaResponse;
import com.castsoftware.artemis.modules.pythia.models.api.*;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import com.castsoftware.artemis.reports.ReportGenerator;
import org.neo4j.graphdb.Node;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class ADetector {
  // Imaging Properties
  protected static final String IMAGING_OBJECT_TAGS =
      Configuration.get("imaging.link.object_property.tags");
  protected static final String IMAGING_OBJECT_NAME = Configuration.get("imaging.node.object.name");
  protected static final String IMAGING_OBJECT_FULL_NAME =
      Configuration.get("imaging.node.object.fullName");
  protected static final String IMAGING_APPLICATION_LABEL =
      Configuration.get("imaging.application.label");
  protected static final String IMAGING_INTERNAL_TYPE =
      Configuration.get("imaging.application.InternalType");
  protected  static final String IMAGING_DEFAULT_FRAMEWORK_CAT =  "Business Logic";

  // Detector parameters
  protected Neo4jAL neo4jAL;
  protected String application;
  protected SupportedLanguage language;
  protected DetectionParameters detectionParameters;

  //
  protected List<Node> toInvestigateNodes;
  protected ReportGenerator reportGenerator;
  private List<FrameworkNode> frameworkNodeList = new ArrayList<>();;

  // NLP
  protected NLPEngine nlpEngine;
  protected NLPSaver nlpSaver;

  /** Pythia communication * */
  protected boolean activatedPythia = false;

  protected Pythia pythiaController;
  protected PythiaLanguage pythiaLanguage;

  protected GoogleParser googleParser;
  protected LanguageProp languageProperties;

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
      DetectionParameters parameters)
      throws IOException, Neo4jQueryException {

    // Initialize the controllers
    this.language = language;
    this.neo4jAL = neo4jAL;
    this.application = application;
    this.detectionParameters = parameters;
    this.pythiaController = new Pythia(parameters.getPythiaURL(), parameters.getPythiaToken());

    neo4jAL.logInfo(
        String.format(
            "[1/9] The instantiation of %s detector started.\nThis operation can take up to one minute.",
            language.toString()));

    neo4jAL.logInfo("[2/9] Retrieving the list of candidates nodes...");
    // To investigate nodes
    this.toInvestigateNodes = new ArrayList<>();


    // Check pythia
    neo4jAL.logInfo("[3/9] Check pythia...");
    this.initPythia();

    // NLP : Make sure the nlp is trained, train it otherwise
    neo4jAL.logInfo(String.format("[5/9] Starting the NLP¨Engine for %s...", language.toString()));
    this.initNLP();

    // Initiate the Report generator
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

  /** Initialize the Pythia connection */
  public void initPythia() {
    try {
      // If Pythia is not activated, log and return
      if (!this.detectionParameters.getPythiaMode()) {
        neo4jAL.logInfo("Pythia is deactivated. Skipping.");
        return;
      }

      // Test Pythia communication
      try {
        neo4jAL.logInfo("Pythia :: Pythia is activated. Initializing.");
        String status = this.pythiaController.getStatus();
        neo4jAL.logInfo(String.format("Successful communication with Pythia. Status : %s", status));
      } catch (PythiaException | PythiaResponse e) {
        neo4jAL.logError("Pythia :: Failed to reach Pythia. Turning off Pythia mode.", e);
        this.activatedPythia = false;
        return;
      }

      // Get Pythia Language
      String language = this.language.toString();

      // Get the for the detection language if activated
      try {
        neo4jAL.logInfo("Pythia :: Looking for a language.");

        // Find the language
        pythiaLanguage = pythiaController.findLanguage(language);

        // Make sure the result is not empty
        if (pythiaLanguage == null) {
          throw new Exception("Pythia language is null");
        }

        // Notify in the report
        neo4jAL.logInfo(
            String.format(
                "Pythia :: Pythia found a corresponding language for the detector. Language : %s",
                pythiaLanguage.toJson().toString()));
      } catch (Exception | PythiaResponse | PythiaException pythiaResponse) {
        neo4jAL.logError(
            String.format("Pythia :: Failed to find '%s' language on pythia .", language),
            pythiaResponse);
        this.activatedPythia = false;
        return;
      }

      // If everything is a success return true
      this.activatedPythia = true;
    } catch (Exception e) {
      this.neo4jAL.logError("Pythia :: An error occurred during the initialisation of pythia.", e);
      this.activatedPythia = false;
    }
  }

  /**
   * Initialize the NLP Engine
   *
   * @throws IOException
   */
  public void initNLP() throws IOException {
    this.nlpSaver = new NLPSaver(neo4jAL, application, language.toString());
    this.nlpEngine = new NLPEngine(neo4jAL, language);

    Path modelFile = this.nlpEngine.checkIfModelExists();
    if (!Files.exists(modelFile)) {
      neo4jAL.logInfo(
          String.format("[5-2/9] Training the NLP¨Engine for %s...", language.toString()));
      this.nlpEngine.train();
    } else {
      neo4jAL.logInfo(
          String.format("[5-2/9] NLP¨Engine already trained for %s...", language.toString()));
    }
  }

  /**
   * Get candidates nodes for the detection
   *
   * @throws Neo4jQueryException
   */
  public void getNodes() throws Neo4jQueryException {
    toInvestigateNodes = DetectorNodesUtil.getExternalObjects(neo4jAL, languageProperties, application);
  }

  /**
   * Get full list of internal candidates
   *
   * @return List of internal candidate
   * @throws Neo4jQueryException
   */
  public List<Node> getInternalNodes() throws Neo4jQueryException {
    return DetectorNodesUtil.getInternalObjects(neo4jAL, languageProperties, application);
  }

  public abstract ATree getExternalBreakdown() throws Neo4jQueryException;

  public abstract ATree getInternalBreakdown() throws Neo4jQueryException;

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
   * Flag a node using its related framework
   *
   * @param n Node to tag
   * @param frameworkNode Framework to apply
   * @throws Neo4jQueryException
   */
  public void tagNodeWithFramework(Node n, FrameworkNode frameworkNode) throws Neo4jQueryException {
    String category = IMAGING_DEFAULT_FRAMEWORK_CAT;

    DetectorPropertyUtil.applyDetectionProperty(n, frameworkNode.getFrameworkType().toDetectionCategory());
    DetectorPropertyUtil.applyCategory(n, category);
    DetectorPropertyUtil.applyFrameworkName(neo4jAL, n, frameworkNode.getName());
    DetectorPropertyUtil.applyDescriptionProperty(neo4jAL, n, frameworkNode.getDescription());
  }

  /**
   * Apply a Pythia framework on a group of nodes
   *
   * @param n Node to tagDetection for pattern
   * @param frameworkNode Framework node
   * @throws Neo4jQueryException
   */
  public void tagNodeWithPythia(Node n, PythiaFramework frameworkNode) throws Neo4jQueryException {
    // Verify Category

    String defaultTaxonomy = Configuration.get("artemis.node.default.taxonomy");
    String taxonomy = String.format("%1$s##%2$s##%2$s", defaultTaxonomy, frameworkNode.getLevel5());

    // Apply properties
    DetectorPropertyUtil.applyDetectionProperty(n, DetectionCategory.KNOWN_UTILITY);
    DetectorPropertyUtil.applyTaxonomyProperty(n, taxonomy);
    DetectorPropertyUtil.applyFrameworkName(neo4jAL, n, frameworkNode.getName());
    DetectorPropertyUtil.applyDescriptionProperty(neo4jAL, n, frameworkNode.getDescription());
  }



  /**
   * Check if the name of the framework has been exlcuded
   * @param n Node to investigate
   * @return
   */
  protected boolean isNameExcluded(Node n) {
    if (detectionParameters == null) return false;

    if (!n.hasProperty("FullName")) return true;
    String fullName = (String) n.getProperty("FullName");

    for (String regex : detectionParameters.getPatternFullNameToExclude()) {
      if (fullName.matches(regex)) return true;
    }

    return false;
  }

  protected boolean isInternalTypeExcluded(Node n) {
    if (detectionParameters == null) return false;

    if (!n.hasProperty("InternalType")) return true;
    String internalType = (String) n.getProperty("InternalType");

    return false;
  }

  /***
   * Add a Framework to result list
   * @param fn Framework to add
   */
  protected void addFrameworkToResults(FrameworkNode fn) {
    this.frameworkNodeList.add(fn);
  }

  /**
   * Pre launch actions
   */
  protected void preLaunch() {}

  protected void postLaunch() {}

  /**
   * Launch the detection in the Application
   *
   * @return The list of detected frameworks
   * @throws IOException Workspace not found
   * @throws Neo4jQueryException Bad Neo4j query
   * @throws Neo4jBadRequestException Bad Neo4j request
   */
  public final List<FrameworkNode> launch()
      throws IOException, Neo4jQueryException, Neo4jBadRequestException {
    // Initialize the Framework list
    this.frameworkNodeList = new ArrayList<>();

    // Launch before
    preLaunch();

    // Print the configuration of the analysis
    printConfig();

    // Detection flow
    extractFrameworks();


    // Add the language detected to the application
    neo4jAL.logInfo("Add the language to the Application controller.");
    ApplicationController.addLanguage(neo4jAL, application, languageProperties.getName());

    neo4jAL.logInfo(
        String.format(
            "%d entries (valid detection and not valid ones) were found during the analysis of application %s",
            this.frameworkNodeList.size(), application));

    // Launch after
    try {
      postLaunch();
    } catch (Exception e) {
      neo4jAL.logError("Failed to launched the post analysis operations.");
      neo4jAL.logError("Post launch methods failed and threw an exception.", e);
    }

    return this.frameworkNodeList;
  }

  /** Print the configuration of the current detection */
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
        String.format("| Pythia Mode (Search on Pythia etc..) set on  : %s ", getPythiaMode()));
    neo4jAL.logInfo(
        String.format(
            "| Online Mode (Google search, Duckduck go etc..) set on  : %s ", getOnlineMode()));
    neo4jAL.logInfo(
        String.format("| Repository Mode (Maven, npm, etc..) set on  : %s ", getRepositoryMode()));
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
            "| Overload online                                   : %s ",
            detectionParameters.getOnlineMode()));

    neo4jAL.logInfo(
        "__________________________________________________________________________________");
  }

  /**
   * Extract utilities
   *
   * @return List of findings
   */
  public abstract void extractFrameworks() throws IOException, Neo4jQueryException;

  /**
   * Get pythia Mode
   *
   * @return
   */
  public boolean getPythiaMode() {
    return this.activatedPythia;
  }

  /**
   * Get Online mode
   *
   * @return
   */
  public boolean getOnlineMode() {
    return detectionParameters.getOnlineMode();
  }

  /**
   * Get the Repository mode
   *
   * @return
   */
  public boolean getRepositoryMode() {
    return detectionParameters.getRepositoryMode();
  }

  public boolean getPersistentMode() {
    return Boolean.parseBoolean(Configuration.get("artemis.persistent_mode"));
  }

  public boolean getLearningMode() {
    return Boolean.parseBoolean(Configuration.get("artemis.learning_mode"));
  }

  /** Extract unknown non utilities */
  public void extractOtherApps() {}

  /**
   * Save NLP Results to the Artemis Database. The target database will be decided depending on the
   * value of
   *
   * @param name Name of the object to save
   * @param results Results of the NLP Engine
   * @throws InvalidDatasetException
   */
  protected FrameworkNode saveNLPFrameworkResult(
      String name, NLPResults results, String internalType) throws Neo4jQueryException {

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

    // Todo Adapt this section for Java / NET
    Boolean isRegex = false;

    FrameworkNode fb =
        new FrameworkNode(
            neo4jAL,
            name,
            name,
            isRegex,
            strDate,
            "No location discovered",
            "",
            1L,
            detectionScore,
            new Date().getTime());
    fb.setFrameworkType(fType);
    fb.setInternalTypes(languageProperties.getObjectsInternalType());
    return fb;
  }

  // Pythia function
  /**
   * Save a Framework on Pythia
   *
   * @param pf Framework to save
   */
  protected void saveFrameworkOnPythia(PythiaFramework pf, List<PythiaPattern> patterns) {
    neo4jAL.logInfo(String.format("Sending Framework '%s' to Pythia.", pf.name));

    // If Pythia activated
    if (activatedPythia) {
      try {
        this.pythiaController.createFramework(pf, patterns);
      } catch (PythiaException | PythiaResponse e) {
        neo4jAL.logError(String.format("Failed to upload framework %s to pythia.", pf.name));
      }
    }
  }

  /**
   * Find a Framework on pythia with a similar pattern
   *
   * @param pattern Pattern to search
   * @return Optional returning the pattern
   */
  protected final Optional<PythiaImagingFramework> findFrameworkOnPythia(String pattern) {
    // If Pythia activated
    if (activatedPythia) {
      try {
        PythiaImagingFramework pf =
            this.pythiaController.findFrameworkByPattern(pattern, this.pythiaLanguage.name);

        if (pf == null) return Optional.empty();
        return Optional.of(pf); // Return the framework
      } catch (PythiaException | PythiaResponse e) {
        neo4jAL.logError(
            String.format("Failed to find a framework using pattern '%s' on pythia. Error: %s", pattern, e));
        return Optional.empty();
      }
    } else {
      // Not activated or error
      neo4jAL.logError("Failed to find a framework pythia communication is not active.");
    }

    return Optional.empty();
  }

  /**
   * Save a framework to the database and Pythia if activated
   *
   * @param frameworkNode Node to save
   */
  protected void persistFramework(FrameworkNode frameworkNode) {
    // Persist on Neo4j Database
    if (getPersistentMode()) {
      try {
        frameworkNode.createNode();
      } catch (Neo4jQueryException ignored) {
        // Ignored
      }
    }
  }
}
