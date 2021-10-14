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

package com.castsoftware.artemis.modules.nlp.model;

import com.castsoftware.artemis.config.Configuration;

import java.util.Arrays;

/** Default result structure for NLP operations */
public class NLPResults {

  public static final String IS_FRAMEWORK_CATEGORY = Configuration.get("nlp.category.is_framework");
  private static final Double MIN_CONFIDENCE_GAP =
      Double.parseDouble(Configuration.get("artemis.properties.nlp.minimum_confidence_gap"));
  private NLPCategory category;
  private NLPConfidence confidence;
  private final double probability;
  private final double[] probabilities;

  // Getters

  public NLPResults(String category, double[] probabilities) {
    this.probabilities = probabilities;

    this.probability = probabilities[0];

    // Bind the result to a NLPResult
    this.category = NLPCategory.NOT_FRAMEWORK;
    if (category.equals(IS_FRAMEWORK_CATEGORY)) {
      this.category = NLPCategory.FRAMEWORK;
    }

    // Get the level of confidence base on the user preferences
    this.confidence = NLPConfidence.CONFIDENT;
    // Check if the gap between detection is lower than the minimum gap of confidence
    if (probabilities.length >= 2) {
      for (int i = 0; i < probabilities.length - 2; i++) {
        double gap = probabilities[i + 1] - probabilities[i];
        if (gap < MIN_CONFIDENCE_GAP) {
          this.confidence = NLPConfidence.NOT_CONFIDENT;
        }
      }
    }
  }

  public NLPCategory getCategory() {
    return category;
  }

  public NLPConfidence getConfidence() {
    return confidence;
  }

  public double[] getProbabilities() {
    return probabilities;
  }

  public double getProbability() {
    return probability;
  }

  @Override
  public String toString() {
    return "NLPResults{"
        + "category="
        + category
        + ", confidence="
        + confidence
        + ", probability="
        + probability
        + ", probabilities="
        + Arrays.toString(probabilities)
        + '}';
  }
}
