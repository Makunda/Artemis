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

package com.castsoftware.artemis.config;

import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.file.MissingFileException;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

/** Singleton in charge of the communication with the configuration file (Read Only) */
public class Configuration {

  private static Properties PROPERTIES = loadConfiguration();

  /**
   * Test if the property is present in the Configuration Node. Then test if the property is present
   * in the UserConfiguration and then in the Classic configuration.
   *
   * @param property key
   * @return Return the value as String, null if it matches no key
   */
  public static String getBestOfAllWorlds(Neo4jAL neo4jAL, String property) {
    // Add the Node configuration
    if (UserConfiguration.has(neo4jAL, property) && UserConfiguration.get(neo4jAL, property) != null)
      return UserConfiguration.get(neo4jAL, property);
    return Configuration.get(property);
  }

  /**
   * Check if a key is present in the Properties
   *
   * @param key
   * @return
   */
  public static Boolean has(String key) {
    PROPERTIES = loadConfiguration();
    return PROPERTIES.contains(key);
  }

  /**
   * Get the corresponding value for the specified key as a String
   *
   * @param key
   * @see this.getAsObject to get the value as an object
   * @return <code>String</code> value for the key as a String
   */
  public static String get(String key) {
    PROPERTIES = loadConfiguration();
    return PROPERTIES.get(key).toString();
  }

  /**
   * Get the corresponding value for the specified key as an object
   *
   * @param key
   * @return <Object>String</code> value for the key as a string
   */
  public static Object getAsObject(String key) {
    return PROPERTIES.get(key);
  }

  /**
   * Set the corresponding value for the specified key
   *
   * @param key
   * @param value
   */
  public static Object set(String key, String value) throws MissingFileException {
    PROPERTIES.setProperty(key, value);
    return PROPERTIES.get(key);
  }


  private static Properties loadConfiguration() {
    try (InputStream input =
        Configuration.class.getClassLoader().getResourceAsStream("artemis.properties")) {

      Properties prop = new Properties();

      if (input == null) {
        throw new MissingFileException(
            "No file 'artemis.properties' was found.",
            "resources/procedure.properties",
            "CONFxLOAD1");
      }

      // load a properties file from class path, inside static method
      prop.load(input);
      return prop;
    } catch (IOException | MissingFileException ex) {
      System.err.println(ex.getMessage());
    }
    return null;
  }
}
