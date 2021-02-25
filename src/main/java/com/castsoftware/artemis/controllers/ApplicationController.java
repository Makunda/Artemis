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

import com.castsoftware.artemis.config.detection.LanguageConfiguration;
import com.castsoftware.artemis.config.detection.LanguageProp;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.nlp.SupportedLanguage;
import org.neo4j.graphdb.Result;

import java.util.*;
import java.util.stream.Collectors;

public class ApplicationController {

  private static String SCANNED_LANGUAGES_PROPERTY = "ScannedLanguages";

  /**
   * Add a language detected to an application (if the language isn't already present)
   *
   * @param neo4jAL Neo4j Access Layer
   * @param application Name of the application concerned by this adding
   * @param language Language to add
   * @throws Neo4jQueryException
   */
  public static void addLanguage(Neo4jAL neo4jAL, String application, String language)
      throws Neo4jQueryException {
    if (!SupportedLanguage.has(language)) return; // ignore if the language is not supported

    SupportedLanguage supLang = SupportedLanguage.getLanguage(language);
    String req =
        String.format(
            String.format(
                "MATCH (o:Application) WHERE o.Name=$Name "
                    + "SET o.%1$s = CASE WHEN NOT EXISTS(o.%1$s) THEN [$Language]  WHEN NOT ($Language in o.%1$s) THEN o.%1$s + $Language END",
                SCANNED_LANGUAGES_PROPERTY),
            SCANNED_LANGUAGES_PROPERTY);

    Map<String, Object> params = Map.of("Name", application, "Language", supLang.toString());
    neo4jAL.executeQuery(req, params);
  }

  /**
   * Reset the languages scanned for one application
   *
   * @param neo4jAL
   * @param application
   * @throws Neo4jQueryException
   */
  public static void resetLanguages(Neo4jAL neo4jAL, String application)
      throws Neo4jQueryException {
    String req =
        String.format(
            "MATCH (a:Application) WHERE a.Name=$Name SET a.%s = [] ", SCANNED_LANGUAGES_PROPERTY);
    Map<String, Object> params = Map.of("Name", application);

    neo4jAL.executeQuery(req, params);
  }

  /**
   * Get the complete list of candidate applications for a scan
   *
   * @param neo4jAL
   * @return
   * @throws Neo4jQueryException
   */
  public static Map<String, List<SupportedLanguage>> getAllCandidates(Neo4jAL neo4jAL)
      throws Neo4jQueryException {
    // Get the list of the application
    String reqApp = "MATCH (a:Application) RETURN a.Name as name";
    Result resApp = neo4jAL.executeQuery(reqApp);

    // Parse the list of application and extract the languages
    Map<String, List<SupportedLanguage>> appLanguageMap = new HashMap<>();
    while (resApp.hasNext()) {
      String app = (String) resApp.next().get("name");
      List<SupportedLanguage> languages = getCandidatesLanguages(neo4jAL, app);

      if (!languages.isEmpty()) {
        appLanguageMap.put(app, languages);
      }
    }

    return appLanguageMap;
  }

  /**
   * Get the Candidate languages for a detection in an application
   *
   * @param neo4jAL Neo4j Access Layer
   * @param application Name of the application to parse
   * @return The list of languages languages for a detection
   * @throws Neo4jQueryException
   */
  public static List<SupportedLanguage> getCandidatesLanguages(Neo4jAL neo4jAL, String application)
      throws Neo4jQueryException {

    String reqTypeCheck =
        String.format(
            "MATCH (o:`%s`:Object) WHERE o.InternalType IN $internalTypes RETURN o as detection LIMIT 1",
            application);

    // Get languages in the application
    Map<String, Object> params;
    Map<String, LanguageProp> map = LanguageConfiguration.getInstance().getLanguageMap();

    List<String> languageList = new ArrayList<>();

    // Parse the languages and check for Object Types detected
    for (LanguageProp lp : map.values()) {
      params = Map.of("internalTypes", lp.getObjectsInternalType());
      Result res = neo4jAL.executeQuery(reqTypeCheck, params);

      if (res.hasNext()) {
        languageList.add(lp.getName());
      }
    }

    // Compare languages detected with the languages already detected in the application, and filter
    List<SupportedLanguage> scannedLanguages = getScannedLanguages(neo4jAL, application);

    return languageList.stream()
        .map(SupportedLanguage::getLanguage)
        .filter(x -> !scannedLanguages.contains(x))
        .collect(Collectors.toList());
  }

  /**
   * Get languages scanned in an application
   *
   * @param neo4jAL Neo4j Access Layer
   * @param application Name of the application
   * @return
   * @throws Neo4jQueryException
   */
  public static List<SupportedLanguage> getScannedLanguages(Neo4jAL neo4jAL, String application)
      throws Neo4jQueryException {
    String req =
        String.format(
            "MATCH (a:Application) WHERE a.Name=$Name RETURN a.%s as languages",
            SCANNED_LANGUAGES_PROPERTY);
    Map<String, Object> params = Map.of("Name", application);

    Result res = neo4jAL.executeQuery(req, params);
    if (res.hasNext()) {
      String[] languages = (String[]) res.next().get("languages");
      if (languages != null) {
        return Arrays.stream(languages)
            .map(SupportedLanguage::getLanguage)
            .collect(Collectors.toList());
      }
    }

    return new ArrayList<>(); // Default value
  }
}
