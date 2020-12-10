package com.castsoftware.artemis.results;

import java.util.List;

public class MixingParameterResult {

    public Double mixingParameter;
    public List<String> recommended_algorithm;

    public MixingParameterResult(Double mixingParameter, List<String> recommended_algorithm) {
        this.mixingParameter = mixingParameter;
        this.recommended_algorithm = recommended_algorithm;
    }
}
