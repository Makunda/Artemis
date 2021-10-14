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

import com.castsoftware.artemis.config.detection.DetectionParameters;
import com.castsoftware.artemis.detector.cobol.CobolDetector;
import com.castsoftware.artemis.detector.java.JavaDetector;
import com.castsoftware.artemis.detector.net.NetDetector;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.modules.nlp.SupportedLanguage;
import com.castsoftware.artemis.neo4j.Neo4jAL;

import java.io.IOException;

public class DetectorFactory {

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
      DetectionParameters detectionParameters)
      throws IOException, Neo4jQueryException {

    ADetector aDetector;
    switch (language) {
      case COBOL:
        aDetector = new CobolDetector(neo4jAL, application, detectionParameters);
        break;
      case JAVA:
        aDetector = new JavaDetector(neo4jAL, application, detectionParameters);
        break;
      case NET:
        aDetector = new NetDetector(neo4jAL, application, detectionParameters);
        break;
      default:
        throw new IllegalArgumentException(
            String.format("The language is not currently supported %s", language.toString()));
    }
    return aDetector;
  }
}
