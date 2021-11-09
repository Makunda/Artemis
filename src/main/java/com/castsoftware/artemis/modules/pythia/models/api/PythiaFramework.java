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
  public String imagingName;

  public String description;
  public String location;

  public List<String> tags;

  public Boolean isRoot;

  public String detectionData;

  public PythiaFramework(
      String name, String imagingName, String description, String location, String detectionData) {
    this.name = name;
    this.imagingName = imagingName;
    this.description = description;
    this.location = location;
    this.detectionData = detectionData;
    this.isRoot = false;
    this.tags = new ArrayList<>();
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

  public String getImagingName() {
    return imagingName;
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
    object.put("name", name);
    object.put("imagingName", imagingName);
    object.put("description", description);
    object.put("location", location);
    object.put("detectionData", detectionData);
    return object;
  }

  @Override
  public PythiaFramework deserialize(
      JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext)
      throws JsonParseException {

    JsonObject root = jsonElement.getAsJsonObject();

    // Get elements in the JSON
    String name = root.get("name").getAsString();
    String imagingName = root.get("imagingName").getAsString();
    String description = root.get("description").getAsString();
    String location = root.get("location").getAsString();
    String detectionData = root.get("detectionData").getAsString();
    Boolean isRoot = root.get("isRoot").getAsBoolean();

    // Create the framework an assign the variables
    PythiaFramework pf =
        new PythiaFramework(name, imagingName, description, location, detectionData);
    pf.setIsRoot(isRoot);

    return pf;
  }
	}