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

package com.castsoftware.artemis.utils;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.config.NodeConfiguration;
import com.castsoftware.artemis.config.UserConfiguration;
import com.castsoftware.artemis.config.detection.LanguageConfiguration;
import com.castsoftware.artemis.config.detection.LanguageProp;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.modules.nlp.SupportedLanguage;
import com.castsoftware.artemis.neo4j.Neo4jAL;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Workspace {

  /**
   * Change the Workspace of Artemis
   *
   * @param directoryPath New directory path
   * @return
   * @throws MissingFileException
   */
  public static List<String> setWorkspacePath(Neo4jAL neo4jAL, String directoryPath)
      throws MissingFileException {
    Path newDirectory = Path.of(directoryPath);

    if (!Files.exists(newDirectory)) {
      return List.of(
          String.format(
              "'%s' is not a valid path. Make sure the target folder exists and retry.",
              directoryPath));
    }

    try {
      NodeConfiguration nc = NodeConfiguration.getInstance(neo4jAL);
      nc.updateWorkspace(newDirectory.toAbsolutePath().toString());
    } catch (Neo4jQueryException | Neo4jBadRequestException e) {
      neo4jAL.logError("Failed to save the new workspace to the Neo4j base");
    }

    Configuration.set("artemis.workspace.folder", newDirectory.toAbsolutePath().toString());

    // Validate the workspace
    List<String> outputMessages = Workspace.validateWorkspace(neo4jAL);

    // Reload User configuration
    UserConfiguration.reload(neo4jAL);

    outputMessages.add(
        String.format("Artemis workspace folder was successfully changed to '%s'.", newDirectory));
    return outputMessages;
  }

  /**
   * Validate if the workspace is valid and contains all the Artemis mandatory files
   *
   * @return List of message to be displayed
   */
  public static List<String> validateWorkspace(Neo4jAL neo4jAL) {
    List<String> messageOutputList = new ArrayList<>();

    Path workspacePath = Workspace.getWorkspacePath(neo4jAL);
    Path reportFolder =
        workspacePath.resolve(Configuration.get("artemis.reports_generator.folder"));
    Path enrichmentFolder =
        workspacePath.resolve(Configuration.get("artemis.nlp_enrichment.folder"));
    Path exportFolder = workspacePath.resolve(Configuration.get("artemis.exports.folder"));
    Path dataFolder = workspacePath.resolve(Configuration.get("artemis.install_data.folder"));
    Path installData =
        dataFolder.resolve(Configuration.get("artemis.install_data.artemis_framework_file"));
    Path headerFilePath =
        workspacePath.resolve(Configuration.get("artemis.parser.header_file.name"));
    Path confFilePath = workspacePath.resolve(Configuration.get("artemis.config.user.conf_file"));
    Path detectionConfFilePath =
        workspacePath.resolve(Configuration.get("artemis.detection.user.conf_file"));

    LanguageConfiguration lc = LanguageConfiguration.getInstance();
    Map<String, LanguageProp> languagePropMap = lc.getLanguageMap();

    // Check if the folder is valid
    if (!Files.exists(workspacePath)) {
      messageOutputList.add(
          String.format(
              "ERROR : %s does not exist. Please specify an existing directory.",
              workspacePath.toString()));
      return messageOutputList;
    }

    // Check main folders and create if necessary
    messageOutputList.addAll(checkOrCreateFolder(reportFolder, "Report folder"));

    // Check data folders and create if necessary
    messageOutputList.addAll(checkOrCreateFolder(dataFolder, "Data folder"));

    // Save the exports
    messageOutputList.addAll(checkOrCreateFolder(exportFolder, "Export folder"));

    // Enrichment folder
    messageOutputList.addAll(checkOrCreateFolder(enrichmentFolder, "Enrichment folder"));

    // Check the existent of the user agent file
    if (!Files.exists(headerFilePath)) {
      messageOutputList.add(
          String.format(
              "ERROR : Header file '%s' is missing. The Google parse will not work without this file.",
              Configuration.get("artemis.parser.header_file.name")));
    }

    // Check the existent of the user configuration file
    if (!Files.exists(confFilePath)) {
      messageOutputList.add(
          String.format(
              "ERROR : Configuration file '%s' is missing. The extension will not work properly without this file.",
              Configuration.get("artemis.config.user.conf_file")));
    }

    // Check the existent of the detection configuration file
    if (!Files.exists(detectionConfFilePath)) {
      messageOutputList.add(
          String.format(
              "ERROR : Detection configuration file '%s' is missing. The detection will use the defaults parameters.",
              Configuration.get("artemis.detection.user.conf_file")));
    }

    // Check the existent of the user configuration file
    if (!Files.exists(installData)) {
      messageOutputList.add(
          String.format(
              "ERROR :  Data initialization zip '%s' is missing. The initialization will not work without this file.",
              Configuration.get("artemis.install_data.artemis_framework_file")));
    }

    // Check languages & associated models
    Path languageFolder;
    Path modelFile;
    LanguageProp lp;
    for (Map.Entry<String, LanguageProp> en : languagePropMap.entrySet()) {
      lp = en.getValue();
      languageFolder = workspacePath.resolve(lp.getName());

      if (!Files.exists(languageFolder)) {
        try {
          Files.createDirectory(languageFolder);
          messageOutputList.add(
              String.format("%s folder was missing and has been created.", lp.getName()));
        } catch (IOException e) {
          messageOutputList.add(
              String.format(
                  "ERROR : %s folder is missing and its creation failed. The detection will not be possible for this language. Error : %s.",
                  lp.getName(), e.getMessage()));
        }
      }

      modelFile = languageFolder.resolve(lp.getModelFileName());
      if (!Files.exists(modelFile)) {
        messageOutputList.add(
            String.format(
                "ERROR : The model file is missing for language : %s. The detection will not be possible for this language.",
                lp.getName()));
      }
    }

    return messageOutputList;
  }

  /**
   * Get the current path of the current Artemis Workspace
   *
   * @return Path of the workspace
   */
  public static Path getWorkspacePath(Neo4jAL neo4jAL) {
    try {
      return NodeConfiguration.getWorkspaceNodeConf(neo4jAL);
    } catch (Neo4jQueryException | Neo4jBadRequestException e) {
      neo4jAL.logError("Failed to retrieve the workspace path set in the configuration node");
      return Path.of(Configuration.get("artemis.workspace.folder"));
    }
  }

  /**
   * Check if the folder exist. If not, create it
   *
   * @param folderPath Path of the folder to check
   * @param name Name of the folder
   * @return
   */
  private static List<String> checkOrCreateFolder(Path folderPath, String name) {
    List<String> messageOutputList = new ArrayList<>();
    // Check main folders and create if necessary
    if (!Files.exists(folderPath)) {
      try {
        Files.createDirectory(folderPath);
        messageOutputList.add(String.format("%s was missing and has been created.", name));
      } catch (IOException e) {
        messageOutputList.add(
            String.format(
                "ERROR : %s is missing and its creation failed : %s", name, e.getMessage()));
      }
    }

    return messageOutputList;
  }

  /**
   * Get the full path of the export folder
   *
   * @return
   */
  public static Path getExportFolder(Neo4jAL neo4jAL) {
    Path workspace = getWorkspacePath(neo4jAL);
    String exportFolder = Configuration.get("artemis.exports.folder");
    return workspace.resolve(exportFolder);
  }

  /**
   * Get the configuration of the detection
   *
   * @return
   */
  public static Path getUserDetectionConfigPath(Neo4jAL neo4jAL) {
    Path workspace = getWorkspacePath(neo4jAL);
    return workspace.resolve(Configuration.get("artemis.detection.user.conf_file"));
  }

  /**
   * Get the supposed path of the initialization
   *
   * @return
   */
  public static Path getInitDataZip(Neo4jAL neo4jAL) {
    Path workspace = getUserConfigPath(neo4jAL);
    Path dataFolder = workspace.resolve(Configuration.get("artemis.install_data.folder"));
    return dataFolder.resolve(Configuration.get("artemis.install_data.artemis_framework_file"));
  }

  /**
   * Path to the user configuration file
   *
   * @return
   */
  public static Path getUserConfigPath(Neo4jAL neo4jAL) {
    Path workspace = getWorkspacePath(neo4jAL);
    return workspace.resolve(Configuration.get("artemis.config.user.conf_file"));
  }

  /**
   * Get the path of the model for a specific language
   *
   * @param language
   * @return
   */
  public static Path getLanguageModelFile(Neo4jAL neo4jAL, SupportedLanguage language) {
    Path workspace = getWorkspacePath(neo4jAL);
    LanguageProp lp =
        LanguageConfiguration.getInstance().getLanguageProperties(language.toString());

    if (lp == null)
      throw new IllegalArgumentException(
          "No properties exists for language with name :".concat(language.toString()));

    Path modelFile = workspace.resolve(lp.getName());
    System.out.printf(
        "Workspace at %s and filename at %s and model file Name : %s",
        workspace.toString(), lp.getName(), lp.getModelFileName());
    return modelFile.resolve(lp.getModelFileName());
  }
}
