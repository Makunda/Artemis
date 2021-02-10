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
import com.castsoftware.artemis.config.LanguageConfiguration;
import com.castsoftware.artemis.config.LanguageProp;
import com.castsoftware.artemis.config.UserConfiguration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.detector.cobol.CobolDetector;
import com.castsoftware.artemis.detector.java.JavaDetector;
import com.castsoftware.artemis.detector.net.NetDetector;
import com.castsoftware.artemis.exceptions.dataset.InvalidDatasetException;
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
  protected PythiaCom pythiaCom;

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
  public ADetector(Neo4jAL neo4jAL, String application, SupportedLanguage language)
      throws IOException, Neo4jQueryException {
    this.neo4jAL = neo4jAL;
    this.application = application;
    this.toInvestigateNodes = new ArrayList<>();
    this.nlpSaver = new NLPSaver(application);
    this.pythiaCom = PythiaCom.getInstance(neo4jAL);

    // Shuffle nodes to avoid being bust by the google bot detector
    Collections.shuffle(this.toInvestigateNodes);

    // Make sure the nlp is trained, train it otherwise
    this.nlpEngine = new NLPEngine(neo4jAL.getLogger(), language);

    Path modelFile = this.nlpEngine.checkIfModelExists();
    if (!Files.exists(modelFile)) {
      this.nlpEngine.train();
    }

    this.reportGenerator = new ReportGenerator(application);
    this.googleParser = new GoogleParser(neo4jAL.getLogger());
    this.frameworkNodeList = new ArrayList<>();

    LanguageConfiguration lc = LanguageConfiguration.getInstance();
    this.languageProperties = lc.getLanguageProperties(language.toString());

    getNodes();
  }

  public abstract List<FrameworkNode> launch() throws IOException, Neo4jQueryException;
  public abstract ATree getBreakdown();


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
    boolean persistentMode = Boolean.parseBoolean(UserConfiguration.get("artemis.persistent_mode"));

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
    fb.setInternalType(internalType);

    // Save the Node to the local database
    if (persistentMode) {
      fb.createNode();
    }

    // If the Oracle communication is up, send the framework to the oracle
    if (pythiaCom.isConnected() && fb.getFrameworkType() == FrameworkType.FRAMEWORK) {
      try {
        pythiaCom.addFramework(fb);
      } catch (Exception e) {
        neo4jAL.logError("Failed to send the framework to the oracle.", e);
      }
    } else {
      pythiaCom.getStatus();
    }

    return fb;
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
              "MATCH (obj:%s:`%s`) WHERE  obj.Type CONTAINS '%s' AND obj.External=true RETURN obj as node",
              IMAGING_OBJECT_LABEL, application, languageProperties.getName());
      res = neo4jAL.executeQuery(forgedRequest);

      while (res.hasNext()) {
        Map<String, Object> resMap = res.next();
        Node node = (Node) resMap.get("node");
        toInvestigateNodes.add(node);
      }
    } else {
      for (String type : categories) {
        String forgedRequest =
            String.format(
                "MATCH (obj:%s:`%s`) WHERE  obj.InternalType='%s' AND obj.External=true RETURN obj as node",
                IMAGING_OBJECT_LABEL, application, type);
        res = neo4jAL.executeQuery(forgedRequest);

        while (res.hasNext()) {
          Map<String, Object> resMap = res.next();
          Node node = (Node) resMap.get("node");
          toInvestigateNodes.add(node);
        }
      }
    }
  }

  /**
   * Get the detector based on the language and the application
   * @param neo4jAL Neo4j Access Layer
   * @param application Name of the application
   * @param language Language of the detector
   * @return
   * @throws IOException
   * @throws Neo4jQueryException
   */
  public static ADetector getDetector(
          Neo4jAL neo4jAL, String application, SupportedLanguage language)
          throws IOException, Neo4jQueryException {

    ADetector aDetector;
    switch (language) {
      case COBOL:
        aDetector = new CobolDetector(neo4jAL, application);
        break;
      case JAVA:
        aDetector = new JavaDetector(neo4jAL, application);
        break;
      case NET:
        aDetector = new NetDetector(neo4jAL, application);
        break;
      default:
        throw new IllegalArgumentException(
                String.format("The language is not currently supported %s", language.toString()));
    }
    return aDetector;
  }


}
