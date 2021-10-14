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

package com.castsoftware.artemis.config.detection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Parameter class for the analysis { 'OnlineMode': true, 'RepositoryMode': false, 'PythiaURL':
 * String, 'PythiaToken': String, 'to_exclude': [] }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetectionParameters {

  @JsonProperty(value = "OnlineMode", defaultValue = "True")
  public Boolean onlineMode;
  @JsonProperty(value = "RepositoryMode", defaultValue = "True")
  public Boolean repositoryMode;
  @JsonProperty(value = "PythiaURL", defaultValue = "")
  public String pythiaURL;
  @JsonProperty(value = "PythiaToken", defaultValue = "")
  public String pythiaToken;
  public Boolean pythiaMode;
  private List<String> patternFullNameToExclude = new ArrayList<>();
  private List<String> patternObjectType = new ArrayList<>();

  public DetectionParameters() {}

  /**
   * Deserialize the parameters
   *
   * @param parameters Parameters as a string to deserialize
   * @return A DetectionParameters initialized with the parameter, or null if the process failed
   */
  public static Optional<DetectionParameters> deserializeOrDefault(String parameters) {
    // Provided an empty configuration
    if (parameters.isBlank()) return Optional.empty();

    try {
      // Deserialize
      ObjectMapper objectMapper = new ObjectMapper();
      DetectionParameters dp = objectMapper.readValue(parameters, DetectionParameters.class);

      // Check pythia parameters and activate it if set up
      dp.pythiaMode = (!dp.pythiaToken.isBlank() && !dp.pythiaURL.isBlank());

      return Optional.of(dp);
    } catch (IOException e) {
      System.err.printf(
          "Failed to deserialize the configuration provided. Error : %s ", e.getMessage());

      return Optional.empty();
    }
  }

  @JsonProperty("to_exclude")
  private void unpackToExclude(Map<String, Object> arrangement) {
    try {
      patternFullNameToExclude = (List<String>) arrangement.get("regex_object_fullName");
    } catch (ClassCastException e) {
      System.err.println("Failed to get the value of to_exclude.regex_object_fullName");
    }

    try {
      patternObjectType = (List<String>) arrangement.get("regex_object_type");
    } catch (ClassCastException e) {
      System.err.println("Failed to get the value of to_exclude.regex_object_type");
    }
  }

  public List<String> getPatternFullNameToExclude() {
    return patternFullNameToExclude;
  }

  public List<String> getPatternObjectType() {
    return patternObjectType;
  }

  public Boolean getOnlineMode() {
    return onlineMode;
  }

  /**
   * Get the repository crawling activation status
   *
   * @return True if activated false otherwise
   */
  public Boolean getRepositoryMode() {
    return repositoryMode;
  }

  // Pythia parameters

  /**
   * Get the Pythia activation status
   *
   * @return True if activated false otherwise
   */
  public Boolean getPythiaMode() {
    return pythiaMode;
  }

  /**
   * Get the URL of Pythia
   *
   * @return the URL
   */
  public String getPythiaURL() {
    return pythiaURL;
  }

  /**
   * Get the Access Token of pythia
   *
   * @return The token
   */
  public String getPythiaToken() {
    return pythiaToken;
  }
}
