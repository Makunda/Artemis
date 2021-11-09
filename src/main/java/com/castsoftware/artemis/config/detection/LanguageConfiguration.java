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
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.global.SupportedLanguage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import kong.unirest.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LanguageConfiguration {

  private static final String CONFIG_FILE_NAME = "language_conf.json";

  private static final LanguageConfiguration instance = new LanguageConfiguration();

  private final Map<String, LanguageProp> languageMap = new HashMap();

  private LanguageConfiguration() {
    try (InputStream inputStream =
        Configuration.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {

      if (inputStream == null) {
        throw new MissingFileException(
            "No file 'artemis.properties' was found.",
            "resources/procedure.properties",
            "CONFxLOAD1");
      }

      String inputStr;
      Gson gson = new Gson();
      StringBuilder responseStrBuilder = new StringBuilder();
      BufferedReader streamReader =
          new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
      Type languagePropType = new TypeToken<List<String>>() {}.getType();

      // Convert input stream to Json
      while ((inputStr = streamReader.readLine()) != null) responseStrBuilder.append(inputStr);
      JSONObject jsonConfig = new JSONObject(responseStrBuilder.toString());

      // Iterate over Json keys and feed languageMap
      Iterator<String> it = jsonConfig.keys();
      while (it.hasNext()) {
        String key = it.next();
        JSONObject j = jsonConfig.getJSONObject(key);
        List<String> repoList = new ArrayList<>();
        List<String> objectTypeList = new ArrayList<>();
        List<String> keywordsList = new ArrayList<>();

        Iterator<Object> repoIterator = j.getJSONArray("repository_search").iterator();
        while (repoIterator.hasNext()) {
          Object o = repoIterator.next();
          repoList.add(o.toString());
        }

        Iterator<Object> objectTypeIterator = j.getJSONArray("objects_internal_type").iterator();
        while (objectTypeIterator.hasNext()) {
          Object o = objectTypeIterator.next();
          objectTypeList.add(o.toString());
        }

        Iterator<Object> keywordIterator = j.getJSONArray("keywords").iterator();
        while (keywordIterator.hasNext()) {
          Object o = keywordIterator.next();
          keywordsList.add(o.toString());
        }

        LanguageProp lp =
            new LanguageProp(
                j.getString("name"),
                j.getBoolean("online_search"),
                j.getBoolean("interaction_detector"),
                j.getString("package_delimiter"),
                repoList,
                objectTypeList,
                keywordsList,
                j.getString("nlp_model"));

        languageMap.put(key, lp);
        // Print the JSON detected
        System.out.println("LANGUAGE PROP : Configuration loaded for : " + lp.getName());
      }

    } catch (IOException | MissingFileException ex) {
      System.err.println(ex.getMessage());
      System.exit(-1);
    }
  }

  /** @return Actual instance of Language configuration */
  public static LanguageConfiguration getInstance() {
    return instance;
  }

  public LanguageProp getLanguageProperties(SupportedLanguage language) {
    return getLanguageProperties(language.toString());
  }

  /**
   * Get the properties for a specific language
   *
   * @param language
   * @return The associated language properties, or null if not found ( language doesn't exist)
   */
  public LanguageProp getLanguageProperties(String language) {
    language = language.toUpperCase();
    if (!checkLanguageExistence(language)) return null;
    return languageMap.get(language);
  }

  /**
   * Check the existence of a Language in the configuration
   *
   * @param language Language to search in the configuration
   * @return True if the language exist, False otherwise
   */
  public boolean checkLanguageExistence(String language) {
    language = language.toUpperCase();
    return languageMap.containsKey(language);
  }

  /**
   * Return the Map of the languages
   *
   * @return
   */
  public Map<String, LanguageProp> getLanguageMap() {
    return languageMap;
  }
}
