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

import com.castsoftware.artemis.config.detection.DetectionParameters;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.detector.plainAnalyzers.ADetector;
import com.castsoftware.artemis.detector.plainAnalyzers.DetectorFactory;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.global.SupportedLanguage;
import com.castsoftware.artemis.modules.nlp.model.NLPEngine;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import com.castsoftware.artemis.results.FrameworkResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DetectionController {

  /**
   * Train the NLP engine of Artemis
   *
   * @param neo4jAL Neo4j Access Layer
   * @throws IOException If the training file was not found
   */
  public static void trainArtemis(Neo4jAL neo4jAL) throws IOException {
    NLPEngine nlpEngine = new NLPEngine(neo4jAL, SupportedLanguage.ALL);
    nlpEngine.train();
  }

  /**
   * Launch the Artemis Detection against the specified application
   *
   * @param neo4jAL Neo4J Access Layer
   * @param application Application used during the detection
   * @param language Specify the language of the application to pick the correct dt
   * @return The list of detected frameworks
   * @throws Neo4jQueryException Neo4j query failed
   * @throws IOException Workspace hasn't been properly set
   */
  public static List<FrameworkResult> launchDetection(
      Neo4jAL neo4jAL, String application, String language, String detectionPropAsJson)
      throws Neo4jQueryException, Exception, Neo4jBadRequestException {

    Optional<DetectionParameters> detectionProp;

    try {
      detectionProp = DetectionParameters.deserializeOrDefault(detectionPropAsJson);
    } catch (IOException error) {
      neo4jAL.logError("Failed to deserialized the parameters that have been passed.", error);
      throw new Exception("Invalid parameters provided");
    }

    // Detection parameters no found
    if (detectionProp.isEmpty()) {
      throw new Exception("No parameters has been passed. You must pass detection parameters.");
    }

    // Launch with parameter
    neo4jAL.logInfo(
        String.format("Launching detection with parameters : %s ", detectionPropAsJson));
    List<FrameworkNode> frameworkList =
        getFrameworkList(
            neo4jAL, application, SupportedLanguage.getLanguage(language), detectionProp.get());

    List<FrameworkResult> resultList = new ArrayList<>();
    // Convert the framework detected to Framework Results
    for (FrameworkNode fn : frameworkList) {
      FrameworkResult fr = new FrameworkResult(fn);
      resultList.add(fr);
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
   * @throws IOException If the workspace has been set properly
   * @throws Neo4jQueryException Neo4j error
   */
  private static List<FrameworkNode> getFrameworkList(
      Neo4jAL neo4jAL,
      String application,
      SupportedLanguage language,
      DetectionParameters detectionParameters)
      throws IOException, Neo4jQueryException, Neo4jBadRequestException {

    ADetector aDetector =
        DetectorFactory.getDetector(neo4jAL, application, language, detectionParameters);

    return aDetector.launch();
  }
}
