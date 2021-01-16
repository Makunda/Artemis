package com.castsoftware.artemis.config;

import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.utils.Workspace;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

/**
 * Retrieve the Configuration of the user config file
 */
public class UserConfiguration {

    private static Properties PROPERTIES = loadConfiguration();

    /**
     * Get the corresponding value for the specified key as a String
     * If the configuration file doesn't exist, returns Null
     * @param key
     * @see this.getAsObject to get the value as an object
     * @return <code>String</code> value for the key as a String
     */
    public static String get(String key) {
        if(PROPERTIES == null){
            return null;
        }

        return PROPERTIES.get(key).toString();
    }

    /**
     * Get the corresponding value for the specified key as an object.
     * If the configuration file doesn't exist, returns Null
     * @param key
     * @return <Object>String</code> value for the key as a string
     */
    public static Object getAsObject(String key) {
        if(PROPERTIES == null){
            return null;
        }

        return PROPERTIES.get(key);
    }

    /**
     * Check if the properties file if valid
     * @return
     */
    public static Boolean isLoaded() {
        return PROPERTIES != null;
    }

    public static Set<Object> getKeySet() {
        return PROPERTIES.keySet();
    }

    /**
     * Reload the configuration from the file
     * @return
     */
    public static Properties reload() {
        PROPERTIES = loadConfiguration();
        return PROPERTIES;
    }

    /**
     * Load the user configuration file
     * @return The list properties found in the configuration file.
     */
    private static Properties loadConfiguration() {
        Path configurationPath = Workspace.getUserConfigPath();

        if (!Files.exists(configurationPath)) {
            System.err.printf("No configuration file found at path : %s%n",  configurationPath.toString());
            return null;
        }

      try (InputStream input = new FileInputStream(configurationPath.toFile())) {
            Properties prop = new Properties();

            if (input == null) {
                throw new MissingFileException("No file 'artemis.properties' was found.", "resources/procedure.properties", "CONFxLOAD1");
            }

            //load a properties file from class path, inside static method
            prop.load(input);
            return prop;
        } catch (IOException | MissingFileException ex) {
            System.err.println(ex.getMessage());
            return null;
        }
    }
}
