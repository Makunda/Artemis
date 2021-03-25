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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * { 'OnlineMode': true, 'RepositoryMode': false, 'PythiaMode': true, 'to_exclude': [] }
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetectionProp {

  public static DetectionProp deserializeOrDefault(String parameters)
          throws IOException {

    if(parameters.isBlank()) return null;

    try {
      ObjectMapper objectMapper = new ObjectMapper();
      return objectMapper.readValue(parameters, DetectionProp.class);
    } catch (IOException e) {
      System.err.printf(
              "Failed to deserialize the configuration provided. Error : %s ",
              e.getMessage());

      return null;
    }
  }


  private List<String> patternFullNameToExclude = new ArrayList<>();
  private List<String> patternObjectType = new ArrayList<>();

  public DetectionProp() {}

  @JsonProperty("OnlineMode")
  public Boolean onlineMode ;

  @JsonProperty("RepositoryMode")
  public Boolean repositoryMode;

  @JsonProperty("PythiaMode")
  public Boolean pythiaMode;

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

  public void setPatternFullNameToExclude(List<String> patternFullNameToExclude) {
    this.patternFullNameToExclude = patternFullNameToExclude;
  }

  public List<String> getPatternObjectType() {
    return patternObjectType;
  }

  public void setPatternObjectType(List<String> patternObjectType) {
    this.patternObjectType = patternObjectType;
  }

  public Boolean getOnlineMode() {
    return onlineMode;
  }

  public void setOnlineMode(Boolean onlineMode) {
    this.onlineMode = onlineMode;
  }

  public Boolean getRepositoryMode() {
    return repositoryMode;
  }

  public void setRepositoryMode(Boolean repositoryMode) {
    this.repositoryMode = repositoryMode;
  }

  public Boolean getPythiaMode() {
    return pythiaMode;
  }

  public void setPythiaMode(Boolean pythiaMode) {
    this.pythiaMode = pythiaMode;
  }
}
