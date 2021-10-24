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
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.detector.java.utils.FrameworkTreeLeaf;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaFramework;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaLanguage;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaPattern;
import com.castsoftware.artemis.neo4j.Neo4jAL;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
      FrameworkNode frameworkNode, PythiaLanguage language) {
    // Create pythia pattern
    PythiaPattern pattern =
        new PythiaPattern(language, frameworkNode.getPattern(), frameworkNode.getIsRegex());
    PythiaPattern[] patterns = {pattern};

    // Create pythia framework
    return new PythiaFramework(
        frameworkNode.getName(),
        frameworkNode.getName(),
        frameworkNode.getDescription(),
        frameworkNode.getLocation(),
        frameworkNode.getDetectionData());
  }

  /**
   * Convert a Tree leaf in a Framework node
   * @param neo4jAL Neo4j access layer
   * @param ftl Leaf
   * @return
   */
  public static FrameworkNode fromFrameworkLeafToFrameworkNode(
      Neo4jAL neo4jAL, FrameworkTreeLeaf ftl) {
    FrameworkNode fb =
        new FrameworkNode(
            neo4jAL,
            ftl.getFullName(),
            ftl.getFullName(),
            true,
            new SimpleDateFormat("dd-MM-yyyy").format(new Date()),
            "Local detection",
            ftl.getFullName(),
            1L,
            1.,
            new Date().getTime());
    fb.setFrameworkType(FrameworkType.FRAMEWORK);
    return fb;
  }

  /**
   * Convert a Framework leaf to a Pythia Framework
   *
   * @param frameworkLeaf Framework leaf to convert
   * @param language Language
   * @return The Pythia Framework
   */
  public static PythiaFramework frameworkLeafToPythia(
      FrameworkTreeLeaf frameworkLeaf, PythiaLanguage language) {

    // Generate Imaging name
    String imagingName = getImagingNameFromLeaf(frameworkLeaf);

    // Create pythia pattern
    PythiaPattern pattern = new PythiaPattern(language, frameworkLeaf.fullName, true);
    PythiaPattern[] patterns = {pattern};

    // Create pythia framework
    return new PythiaFramework(
        frameworkLeaf.getFullName(), imagingName, "", "Local Artemis", "");
  }

  /**
   * Transform a framework leaf to
   * @param neo4jAL Neo4j Access layer
   * @param pf Pythia Framework
   * @return The Framework node
   */
  public static FrameworkNode pythiaFrameworkToFrameworkNode(
          Neo4jAL neo4jAL, PythiaFramework pf, String pattern, Boolean isRegex) {
    // Get date
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    Date date = Calendar.getInstance().getTime();
    String strDate = dateFormat.format(date);

    // Generate Imaging name
    FrameworkNode fn = new FrameworkNode(neo4jAL,
            pf.name,
            pattern,
            isRegex,
            strDate,
            pf.location,
            pf.description,
            0L
            );
    fn.setDetectionData(pf.detectionData);
    fn.setFrameworkType(FrameworkType.FRAMEWORK);
    return fn;
  }

  /**
   * Estimate the imaging name from a leaf
   *
   * @param frameworkLeaf Leaf to create
   * @return The name of the leaf
   */
  private static String getImagingNameFromLeaf(FrameworkTreeLeaf frameworkLeaf) {
    try {
      String imagingName = "API";

      String[] split = frameworkLeaf.fullName.split("\\."); // Split on package name

      if (split.length == 0 || split.length == 1)
        return imagingName + frameworkLeaf.fullName; // Cannot split
      if (split.length == 2)
        return imagingName + " " + capitalizeFirstLetter(split[1]); // Only the Company name
      return imagingName + " " + capitalizeFirstLetter(split[1]) + " " + split[2];
    } catch (Exception ignored) {
      return frameworkLeaf.getFullName();
    }
  }

  /**
   * Capitalize the first letter
   *
   * @param name Name to capitalize
   * @return Capitalized name
   */
  private static String capitalizeFirstLetter(String name) {
    if (name == null || name.isBlank()) return "";

    try {
      return name.substring(0, 1).toUpperCase() + name.substring(1);
    } catch (Exception e) {
      return name;
    }
  }

  /**
   * Create a pattern from a Framework leaf
   * @param ltf Leaf to use for creation
   * @param pl Pythia language
   * @return The pythia pattern
   */
  public static PythiaPattern fromFrameworkLeafToPattern(FrameworkTreeLeaf ltf, PythiaLanguage pl) {
    return new PythiaPattern(pl, ltf.getFullName(), true);
  }
}
