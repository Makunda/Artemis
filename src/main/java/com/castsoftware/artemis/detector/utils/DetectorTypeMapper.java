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

package com.castsoftware.artemis.detector.utils;

import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.detector.java.utils.FrameworkTreeLeaf;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaFramework;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaPattern;

/** Class handling the type conversion between Detector and Pythia */
public class DetectorTypeMapper {

  /**
   * Transform a Artemis Framework in a Pythia Framework
   *
   * @param frameworkNode Framework node to transform
   * @param language Language of the detection
   * @return A Pythia Framework
   */
  public static PythiaFramework artemisFrameworkToPythia(
      FrameworkNode frameworkNode, String language) {
    // Create pythia pattern
    PythiaPattern pattern =
        new PythiaPattern(language, frameworkNode.getPattern(), frameworkNode.getIsRegex());
    PythiaPattern[] patterns = {pattern};

    // Create pythia framework
    return new PythiaFramework(
        frameworkNode.getName(),
        frameworkNode.getDescription(),
        frameworkNode.getLocation(),
        patterns,
        frameworkNode.getDetectionData());
  }

  /**
   * Convert a Framework leaf to a Pythia Framework
   * @param frameworkLeaf Framework leaf to convert
   * @param language Language
   * @return The Pythia Framework
   */
  public static PythiaFramework frameworkLeafToPythia(
          FrameworkTreeLeaf frameworkLeaf, String language) {
    // Create pythia pattern
    PythiaPattern pattern =
            new PythiaPattern(language, frameworkLeaf.fullName + "\\.*", true);
    PythiaPattern[] patterns = {pattern};

    // Create pythia framework
    return new PythiaFramework(
            frameworkLeaf.getFullName(),
            "",
            "Statistical detection",
            patterns,
            "");
  }
}
