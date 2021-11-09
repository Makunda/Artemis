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
import com.castsoftware.artemis.detector.utils.DetectionCategory;
import com.castsoftware.artemis.exceptions.file.MissingFileException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ConfigurationController {

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
   * Get the property storing the CAST Taxonomy
   * @return
   * @throws MissingFileException
   */
  public static String getNodeTaxonomyProperty() throws MissingFileException {
    return Configuration.get("artemis.node.taxonomy");
  }


  /**
   * Get the property of the node detection
   *
   * @return
   * @throws MissingFileException
   */
  public static String getNodeNameProperty() throws MissingFileException {
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
