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

package com.castsoftware.artemis.controllers;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.config.UserConfiguration;
import com.castsoftware.artemis.controllers.api.FrameworkController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.io.Importer;
import com.castsoftware.artemis.utils.Workspace;
import org.neo4j.graphdb.Node;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class UtilsController {

  /**
   * Change the Artemis default workspace
   *
   * @param directoryPath The new Artemis workspace
   * @return
   */
  public static List<String> setArtemisDirectory(String directoryPath) throws MissingFileException {
    return Workspace.setWorkspacePath(directoryPath);
  }

  /**
   * Get the current Artemis directory
   *
   * @return
   */
  public static String getArtemisDirectory() {
    return Configuration.get("artemis.workspace.folder");
  }

  /**
   * Apply a demeter tag to the object using its parent level
   *
   * @param neo4jAL Neo4j access layer
   * @param n Node to tag
   * @param groupName Name of the group
   * @throws Neo4jQueryException
   */
  public static void applyDemeterTag(Neo4jAL neo4jAL, Node n, String groupName)
      throws Neo4jQueryException {
    String demeterPrefix = UserConfiguration.get("demeter.prefix.group_level");
    Long id = n.getId();
    String tagRequest =
        String.format(
            "MATCH (obj) WHERE ID(obj)=%1$s "
                + "SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN ['%2$s'] ELSE obj.Tags + '%2$s' END",
            id, demeterPrefix + groupName);
    neo4jAL.executeQuery(tagRequest);
  }

  /**
   * Apply the demeter level tags on the group matchin the regexes
   * @param neo4jAL Neo4j Access Mayer
   * @param regex Regular expression matching the fullname
   * @param groupName Name of the group created
   * @throws Neo4jQueryException
   */
  public static void applyDemeterTagRegexFullName(Neo4jAL neo4jAL, String regex, String groupName)
          throws Neo4jQueryException {
    String demeterPrefix = UserConfiguration.get("demeter.prefix.group_level");
    String tagRequest =
            String.format(
                    "MATCH (obj) WHERE obj.FullName=~'%1$s' "
                            + "SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN ['%2$s'] ELSE obj.Tags + '%2$s' END",
                    regex, demeterPrefix + groupName);
    neo4jAL.executeQuery(tagRequest);
  }


  public static void applyModuleTagRegexFullName(Neo4jAL neo4jAL, String regex, String groupName)
          throws Neo4jQueryException {
    String demeterPrefix = "$m_";
    String tagRequest =
            String.format(
                    "MATCH (obj) WHERE obj.FullName CONTAINS '%1$s' "
                            + "SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN ['%2$s'] ELSE obj.Tags + '%2$s' END",
                    regex, demeterPrefix + groupName);
    neo4jAL.executeQuery(tagRequest);
  }

  public static void applyArchitectureTagRegexFullName(Neo4jAL neo4jAL, String regex, String viewName, String groupName)
          throws Neo4jQueryException {
    String demeterPrefix = "$a_";
    String tagRequest =
            String.format(
                    "MATCH (obj:Object) WHERE obj.FullName CONTAINS '%1$s' "
                            + "SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN ['%2$s'] ELSE obj.Tags + '%2$s' END",
                    regex, demeterPrefix + viewName + "$" + groupName);
    neo4jAL.executeQuery(tagRequest);
  }

  public static void applyLevelTagRegexFullName(Neo4jAL neo4jAL, String regex, String groupName)
          throws Neo4jQueryException {
    String demeterPrefix = UserConfiguration.get("demeter.prefix.group_level");
    String tagRequest =
            String.format(
                    "MATCH (obj:Object) WHERE obj.FullName CONTAINS '%1$s' "
                            + "SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN ['%2$s'] ELSE obj.Tags + '%2$s' END",
                    regex, demeterPrefix + groupName);
    neo4jAL.executeQuery(tagRequest);
  }



  /**
   * Apply a demeter tag to the object using its parent level and a suffix provided
   *
   * @param neo4jAL Neo4j access Layer
   * @param n Node to tag
   * @param suffix Suffix to append to the group
   * @throws Neo4jQueryException
   */
  public static void applyDemeterParentTag(Neo4jAL neo4jAL, Node n, String suffix)
      throws Neo4jQueryException {
    String demeterPrefix = UserConfiguration.get("demeter.prefix.group_level");
    Long id = n.getId();
    String tagRequest =
        String.format(
            "MATCH (obj)<-[:Aggregates]-(l:Level5) WHERE ID(obj)=%1$s "
                + "WITH obj, '%2$s' + l.Name + '%3$s' as tagName "
                + "SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN [tagName] ELSE obj.Tags + tagName END",
            id, demeterPrefix, suffix);
    neo4jAL.executeQuery(tagRequest);
  }

  /**
   * Switch online mode for Artemis detection
   *
   * @param active New state
   * @return The new state of the online mode
   * @throws MissingFileException
   */
  public static Boolean setOnlineMode(Boolean active) throws MissingFileException {
    UserConfiguration.set("artemis.onlineMode", active.toString());
    return Boolean.parseBoolean(UserConfiguration.get("artemis.onlineMode"));
  }

  /**
   * Get the value of the Online mode of Artemis
   *
   * @return
   */
  public static Boolean getOnlineMode() {
    return Boolean.parseBoolean(UserConfiguration.get("artemis.onlineMode"));
  }

  /**
   * Switch repository crawl mode for Artemis detection
   *
   * @param active New state
   * @return The new state of the repository mode
   * @throws MissingFileException
   */
  public static Boolean setRepositoryMode(Boolean active) throws MissingFileException {
    UserConfiguration.set("artemis.repository_search", active.toString());
    return Boolean.parseBoolean(UserConfiguration.get("artemis.repository_search"));
  }

  /**
   * Get the value of the repository parse parameter
   *
   * @return
   */
  public static Boolean getRepositoryMode() {
    return Boolean.parseBoolean(UserConfiguration.get("artemis.repository_search"));
  }

  /**
   * Set the value of the Persistent mode. In persistent mode the Framework detector will save the
   * entries to the Neo4j database.
   *
   * @param active Value of the persistent mode
   * @return The new value of the Persistent mode
   * @throws MissingFileException
   */
  public static Boolean setPersistentMode(Boolean active) throws MissingFileException {
    UserConfiguration.set("artemis.persistent_mode", active.toString());
    return Boolean.parseBoolean(UserConfiguration.get("artemis.persistent_mode"));
  }

  /**
   * Get the value of the Persistent mode parameter
   *
   * @return
   */
  public static Boolean getPersistentMode() {
    return Boolean.parseBoolean(UserConfiguration.get("artemis.persistent_mode"));
  }

  /**
   * Install the extension
   *
   * @param neo4jAL Neo4j acces Layer
   * @param workspacePath Path of the workspace
   * @return The list of message to follow the different step.
   * @throws MissingFileException
   */
  public static List<String> install(Neo4jAL neo4jAL, String workspacePath)
      throws MissingFileException, Neo4jQueryException {
    // Set the workspace path
    List<String> returnList = new ArrayList<>();
    returnList.addAll(Workspace.setWorkspacePath(workspacePath));

    Path initDataZip = Workspace.getInitDataZip();

    // Import list of frameworks
    if (Files.exists(initDataZip)) {
      try {
        returnList.add("Initialisation data were discovered.");
        Importer importer = new Importer(neo4jAL);
        importer.load(initDataZip.toString());
        returnList.add("Initialisation was successful !");
      } catch (Exception | ProcedureException e) {
        returnList.add(
            "The import of the data failed. If the Demeter extension is not present, make sure you installed the 'Friendly exporter'.");
        returnList.add(
            "The import of the data failed for the following reason: " + e.getLocalizedMessage());
      }

    } else {
      returnList.add("The Initialisation was skipped due to missing files");
    }

    // Reformat framework in base to the new version
    Long reformatted = FrameworkController.reformatFrameworks(neo4jAL);
    returnList.add(
        String.format(
            "%d frameworks were updated and reformatted during the installation.", reformatted));

    return returnList;
  }
}
