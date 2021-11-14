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

package com.castsoftware.artemis.modules.pythia.models.api;

import com.google.gson.*;
import kong.unirest.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Pythia Interface for framework creation */
public class PythiaFramework extends PythiaObject implements JsonDeserializer<PythiaFramework> {
  public String name;

  public String level5;
  public String level4;
  public String level3;
  public String level2;
  public String level1;

  public String description;
  public String location;

  public List<String> tags;

  public Boolean isRoot;

  public String detectionData;

  public PythiaFramework(
      String name, String level5, String description, String location, String detectionData) {
    this.name = name;
    this.level5 = level5;
    this.description = description;
    this.location = location;
    this.detectionData = detectionData;
    this.isRoot = false;
    this.tags = new ArrayList<>();
  }

  /**
   * Bulk set the levels of the framework
   * @param level5 Level 5
   * @param level4 Level 4
   * @param level3 Level 3
   * @param level2 Level 2
   * @param level1 Level 1
   */
  public void setLevels(String level5, String level4, String level3, String level2, String level1) {
    this.level1 = level1;
    this.level2 = level2;
    this.level3 = level3;
    this.level4 = level4;
    this.level5 = level5;
  }

  public void setLevel5(String level5) {
    this.level5 = level5;
  }

  public void setLevel4(String level4) {
    this.level4 = level4;
  }

  public void setLevel3(String level3) {
    this.level3 = level3;
  }

  public void setLevel2(String level2) {
    this.level2 = level2;
  }

  public void setLevel1(String level1) {
    this.level1 = level1;
  }

  public String getLevel5() {
    return (level5 != null) ? level5 : this.name;
  }

  public String getLevel5(String prefix) {
    return (level5 != null) ? level5 : prefix + this.name;
  }

  public String getLevel4() {
    return level4;
  }

  public String getLevel3() {
    return level3;
  }

  public String getLevel2() {
    return level2;
  }

  public String getLevel1() {
    return level1;
  }

  public void setIsRoot(Boolean value) {
    this.isRoot = value;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getLocation() {
    return location;
  }


  public List<String> getTags() {
    return tags;
  }

  public Boolean getRoot() {
    return isRoot;
  }

  public String getDetectionData() {
    return detectionData;
  }

  /**
   * Return the framework as a JSON
   *
   * @return JSON formatted framework
   */
  @Override
  public JSONObject toJson() {
    JSONObject object = new JSONObject();

    // Default parameters
    object.put("name", name);
    object.put("level5", level5);
    object.put("description", description);
    object.put("location", location);
    object.put("detectionData", detectionData);

    // Optional parameters
    if(level4 != null) object.put("level4", level4);
    if(level3 != null) object.put("level3", level3);
    if(level2 != null) object.put("level2", level2);
    if(level1 != null) object.put("level1", level1);

    return object;
  }


  @Override
  public PythiaFramework deserialize(
      JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext)
      throws JsonParseException {

    JsonObject root = jsonElement.getAsJsonObject();

    // Get elements in the JSON
    String name = root.get("name").getAsString();
    String description = root.get("description").getAsString();
    String location = root.get("location").getAsString();
    String detectionData = root.get("detectionData").getAsString();
    Boolean isRoot = root.get("isRoot").getAsBoolean();

    // Taxonomy
    String level5 = root.get("level5").getAsString();
    String level4 = root.get("level4").getAsString();
    String level3 = root.get("level3").getAsString();
    String level2 = root.get("level2").getAsString();
    String level1 = root.get("level1").getAsString();

    // Create the framework an assign the variables
    PythiaFramework pf =
        new PythiaFramework(name, level5, description, location, detectionData);

    pf.setIsRoot(isRoot);
    pf.setLevel4(level4);
    pf.setLevel3(level3);
    pf.setLevel2(level2);
    pf.setLevel1(level1);

    return pf;
  }
}