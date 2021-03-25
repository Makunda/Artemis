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

package com.castsoftware.artemis.controllers;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.config.detection.DetectionProp;
import com.castsoftware.artemis.config.detection.LanguageConfiguration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.detector.ADetector;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.exceptions.nlp.NLPIncorrectConfigurationException;
import com.castsoftware.artemis.nlp.SupportedLanguage;
import com.castsoftware.artemis.nlp.model.NLPEngine;
import com.castsoftware.artemis.results.FrameworkResult;
import org.neo4j.graphdb.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DetectionController {
  // Artemis properties
  private static final String ARTEMIS_SEARCH_PREFIX =
      Configuration.get("artemis.tag.prefix_search");
  private static final String IMAGING_OBJECT_LABEL = Configuration.get("imaging.node.object.label");
  private static final String IMAGING_OBJECT_TAGS =
      Configuration.get("imaging.link.object_property.tags");
  private static final String IMAGING_OBJECT_NAME = Configuration.get("imaging.node.object.name");
  private static final String IMAGING_APPLICATION_LABEL =
      Configuration.get("imaging.application.label");

  //
  private static LanguageConfiguration languageConfiguration = LanguageConfiguration.getInstance();

  /**
   * Train the NLP engine of Artemis
   *
   * @param neo4jAL Neo4j Access Layer
   * @throws IOException
   * @throws NLPIncorrectConfigurationException
   */
  public static void trainArtemis(Neo4jAL neo4jAL)
      throws IOException, NLPIncorrectConfigurationException {
    NLPEngine nlpEngine = new NLPEngine(neo4jAL, SupportedLanguage.ALL);
    nlpEngine.train();
  }

  /**
   * Launch a detection on all application present in the database
   *
   * @param neo4jAL Neo4j Access Layer
   * @param language Language used for the detection
   */
  @Deprecated
  public static List<FrameworkResult> launchBulkDetection(
      Neo4jAL neo4jAL, String language, Boolean flagNodes)
      throws Neo4jQueryException, IOException, Neo4jBadRequestException, MissingFileException {
    List<FrameworkResult> resultList = new ArrayList<>();

    // Application
    String reqAppName =
        "MATCH (o:Object) RETURN DISTINCT [ x IN LABELS(o) WHERE NOT x='Object' ][0] as appName";
    Result res = neo4jAL.executeQuery(reqAppName);

    List<String> appNameList = new ArrayList<>();
    while (res.hasNext()) {
      appNameList.add((String) res.next().get("appName"));
    }

    neo4jAL.logInfo(
        String.format(
            "Launching the analysis on applications : %s", String.join(", ", appNameList)));


    for (String appName : appNameList) {
      resultList.addAll(
          getFrameworkList(neo4jAL, appName, SupportedLanguage.getLanguage(language))
              .stream()
              .map(FrameworkResult::new)
              .collect(Collectors.toList()));
    }

    return resultList;
  }

  /**
   * Get the list of detected framework inside the provided list of node
   *
   * @param neo4jAL Neo4j access Layer
   * @param application Name of the application
   * @param language Language of the detector
   * @return The list of the framework detected
   * @throws IOException
   * @throws Neo4jQueryException
   */
  private static List<FrameworkNode> getFrameworkList(
      Neo4jAL neo4jAL, String application, SupportedLanguage language, DetectionProp detectionProp)
      throws IOException, Neo4jQueryException, Neo4jBadRequestException, MissingFileException {

    ADetector aDetector = ADetector.getDetector(neo4jAL, application, language);
    aDetector.applyDetectionParameters(detectionProp); // Overload
    if(detectionProp != null) {
      neo4jAL.logInfo("Parameters applied : " + detectionProp);
    } else {
      neo4jAL.logInfo("Overload parameters are null");
    }
    return aDetector.launch();
  }

  private static List<FrameworkNode> getFrameworkList(
          Neo4jAL neo4jAL, String application, SupportedLanguage language)
          throws IOException, Neo4jQueryException, Neo4jBadRequestException, MissingFileException {

    ADetector aDetector = ADetector.getDetector(neo4jAL, application, language);

    return aDetector.launch();
  }


  /**
   * Launch the Artemis Detection against the specified application
   *
   * @param neo4jAL Neo4J Access Layer
   * @param application Application used during the detection
   * @param language Specify the language of the application to pick the correct dt
   * @return The list of detected frameworks
   * @throws Neo4jQueryException
   * @throws IOException
   */
  public static List<FrameworkResult> launchDetection(
      Neo4jAL neo4jAL, String application, String language, String detectionPropAsJson)
      throws Neo4jQueryException, IOException, Neo4jBadRequestException, MissingFileException {

    List<FrameworkNode> frameworkList = null;

    // Handle parameter overload
    if(!detectionPropAsJson.isBlank()) {
      neo4jAL.logInfo(String.format("Launching detection with parameters : %s ", detectionPropAsJson));
      DetectionProp detectionProp;
      detectionProp = DetectionProp.deserializeOrDefault(detectionPropAsJson);

      frameworkList = getFrameworkList(
              neo4jAL, application, SupportedLanguage.getLanguage(language), detectionProp);

    } else {
      frameworkList = getFrameworkList(
              neo4jAL, application, SupportedLanguage.getLanguage(language));
    }

    List<FrameworkResult> resultList = new ArrayList<>();
    // Convert the framework detected to Framework Results
    for (FrameworkNode fn : frameworkList) {
      FrameworkResult fr = new FrameworkResult(fn);
      resultList.add(fr);
    }

    return resultList;
  }
}
