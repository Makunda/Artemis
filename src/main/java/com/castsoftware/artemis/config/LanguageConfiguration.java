package com.castsoftware.artemis.config;


import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;

public class LanguageConfiguration {

    private static final String CONFIG_FILE_NAME = "language_conf.json";

    private static LanguageConfiguration instance = new LanguageConfiguration();

    private Map<String, LanguageProp> languageMap = new HashMap();

    /**
     * Check the existence of a Language in the configuration
     * @param language Language to search in the configuration
     * @return True if the language exist, False otherwise
     */
    public boolean checkLanguageExistence(String language) {
        language = language.toUpperCase();
        return languageMap.containsKey(language);
    }

    /**
     * Get the properties for a specific language
     * @param language
     * @return The associated language properties, or null if not found ( language doesn't exist)
     */
    public LanguageProp getLanguageProperties(String language) {
        language = language.toUpperCase();
        if(!checkLanguageExistence(language)) return null;
        return languageMap.get(language);
    }

    /**
     * Return the Map of the languages
     * @return
     */
    public Map<String, LanguageProp> getLanguageMap() {
        return languageMap;
    }

    /**
     * @return Actual instance of Language configuration
     */
    public static LanguageConfiguration getInstance() {
        return instance;
    }

    private LanguageConfiguration() {
        try (InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {


            if (inputStream == null) {
                throw new MissingFileException("No file 'artemis.properties' was found.", "resources/procedure.properties", "CONFxLOAD1");
            }


            String inputStr;
            Gson gson = new Gson();
            StringBuilder responseStrBuilder = new StringBuilder();
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            Type languagePropType = new TypeToken<List<String>>() {}.getType();

            // Convert input stream to Json
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);

            System.out.println("JSON :" + responseStrBuilder.toString());
            JSONObject jsonConfig = new JSONObject(responseStrBuilder.toString());

            // Iterate over Json keys and feed languageMap
            Iterator<String> it  =  jsonConfig.keys();
            while( it.hasNext() ){
                String key = it.next();
                JSONObject j = jsonConfig.getJSONObject(key);
                List<String> repoList = new ArrayList<>();
                List<String> objectTypeList = new ArrayList<>();

                Iterator<Object> repoIterator = j.getJSONArray("repository_search").iterator();
                while(repoIterator.hasNext()) {
                    Object o = repoIterator.next();
                    repoList.add(o.toString());
                }

                Iterator<Object> objectTypeIterator = j.getJSONArray("objects_internal_type").iterator();
                while(objectTypeIterator.hasNext()) {
                    Object o = objectTypeIterator.next();
                    objectTypeList.add(o.toString());
                }

                LanguageProp lp  = new LanguageProp(
                        j.getString("name"),
                        j.getBoolean("online_search"),
                        j.getBoolean("interaction_detector"),
                        j.getString("package_delimiter"),
                        repoList,
                        objectTypeList);

                languageMap.put(key, lp);
                // Print the JSON detected
                System.out.println("Detected : " + lp.toString());
            }

        } catch (IOException | MissingFileException ex) {
            System.err.println(ex.getMessage());
            System.exit(-1);
        }

    }

}
