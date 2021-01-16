package com.castsoftware.artemis.nlp.model;

import com.castsoftware.artemis.config.Configuration;

/**
 * Default result structure for NLP operations
 */
public class NLPResults {

    private static final Double MIN_CONFIDENCE_GAP = Double.parseDouble(Configuration.get("artemis.properties.nlp.minimum_confidence_gap"));
    public static final String IS_FRAMEWORK_CATEGORY = Configuration.get("nlp.category.is_framework");

    private NLPCategory category;
    private NLPConfidence confidence;
    private double probability;
    private double[] probabilities;

    // Getters

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
}
