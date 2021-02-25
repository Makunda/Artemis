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

package com.castsoftware.artemis.controllers.api;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.detector.DetectionCategory;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.pythia.PythiaCom;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ConfigurationController {

  /**
   * Get the URI in the configuration of Pythia
   *
   * @param neo4jAL
   * @return
   */
  public static String getURIPythia(Neo4jAL neo4jAL) {
    String uri = PythiaCom.getInstance(neo4jAL).getUri();
    if (uri == null) return "";
    return uri;
  }

  /**
   * Set the URI in the configuration of Pythia
   *
   * @param neo4jAL
   * @param uri the new URI
   * @return
   */
  public static String setURIPythia(Neo4jAL neo4jAL, String uri) throws MissingFileException {
    String newUri = PythiaCom.getInstance(neo4jAL).setUri(uri);
    if (newUri == null) return "";
    return newUri;
  }

  /**
   * Set the Token in the configuration of Pythia
   *
   * @param neo4jAL
   * @param token the new access token
   * @return
   */
  public static Boolean setTokenPythia(Neo4jAL neo4jAL, String token) throws MissingFileException {
    Boolean changed = PythiaCom.getInstance(neo4jAL).setToken(token);
    return changed;
  }

  /**
   * Check if the Token in the configuration of Pythia is present
   *
   * @param neo4jAL = * @return
   */
  public static Boolean getTokenPythia(Neo4jAL neo4jAL) throws MissingFileException {
    Boolean present = PythiaCom.getInstance(neo4jAL).isTokenPresent();
    return present;
  }

  /**
   * Get the property of the node detection
   *
   * @return
   * @throws MissingFileException
   */
  public static String getNodeDetectionProperty() throws MissingFileException {
    return Configuration.get("artemis.node.detection");
  }

  /**
   * Get the name of category property applied on the nodes
   *
   * @return
   * @throws MissingFileException
   */
  public static String getNodeCategoryProperty() throws MissingFileException {
    return Configuration.get("artemis.node.category");
  }

  /**
   * Get the different values of the detection property
   *
   * @return
   * @throws MissingFileException
   */
  public static List<String> getListDetectionValues() throws MissingFileException {
    return Arrays.stream(DetectionCategory.values())
        .map(Objects::toString)
        .collect(Collectors.toList());
  }
}
