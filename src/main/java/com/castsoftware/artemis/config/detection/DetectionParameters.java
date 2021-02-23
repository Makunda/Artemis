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

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.nlp.SupportedLanguage;
import com.castsoftware.artemis.utils.Workspace;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public class DetectionParameters {

  private static final String CONFIG_FILE_NAME = "detection_default_conf.json";
  private static DetectionParameters INSTANCE = null;

  private DetectionProp defaultParameters;

  private DetectionParameters() throws IOException, MissingFileException {
    try (InputStream inputStream =
        Configuration.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {

      if (inputStream == null) {
        throw new MissingFileException(
            String.format("No file '%s' was found.", CONFIG_FILE_NAME),
            String.format("resources/%s", CONFIG_FILE_NAME),
            "CONFxLOAD1");
      }

      String buff;
      StringBuilder responseStrBuilder = new StringBuilder();
      BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

      while ((buff = streamReader.readLine()) != null) responseStrBuilder.append(buff);


      ObjectMapper objectMapper = new ObjectMapper();
      defaultParameters = objectMapper.readValue(responseStrBuilder.toString(), DetectionProp.class);

    } catch (IOException | MissingFileException | NullPointerException e) {
        System.err.printf("Failed to read the default detection parameters at 'resources/%s' . Error : %s%n", CONFIG_FILE_NAME, e.getMessage());
        System.out.println(e.getLocalizedMessage());
        throw e;
      }
    }

    /** @return Actual instance of Detection configuration */
  public static DetectionParameters getInstance() throws IOException, MissingFileException {
    if(INSTANCE == null) INSTANCE = new DetectionParameters();
    return INSTANCE;
  }

  /**
   * Get the defaults detections parameters in the resources
   * @return The Default parameters configuration
   */
  public DetectionProp getDefaultParameters() {
    return defaultParameters;
  }

  /**
   * Verify if a provided set of parameters is valid or not
   * @param parameters Parameters to deserialize
   * @return
   */
  public static boolean isParametersValid(String parameters) {
      try {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.readValue(parameters, DetectionProp.class);
        return true;
      } catch (IOException e) {
        return false;
      }
  }

  /**
   * Try to instantiate the parameters provided or return the default parameters
   * @param parameters Parameters to deserialize
   * @return
   */
  public static DetectionProp deserializeOrDefault(String parameters) throws IOException, MissingFileException {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      return objectMapper.readValue(parameters, DetectionProp.class);
    } catch (IOException e) {
      System.err.printf("Failed to deserialize the configuration provided. Will use the default configuration. Error : %s ", e.getMessage());
      return getInstance().getDefaultParameters();
    }
  }

  /**
   * Get user configuration or system's default one
   * @return
   * @throws IOException
   * @throws MissingFileException
   */
  public static DetectionProp getUserOrDefault(Neo4jAL neo4jAL) throws IOException, MissingFileException {

    Path configPath = Workspace.getUserDetectionConfigPath(neo4jAL);
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      return objectMapper.readValue(configPath.toFile(), DetectionProp.class);

    } catch (IOException  | NullPointerException e) {
      System.err.printf("Failed to read the default detection parameters at '%s' will use default . Error : %s%n", configPath.toString(), e.getMessage());
      System.out.println(e.getLocalizedMessage());
      return getInstance().getDefaultParameters();
    }

  }


}
