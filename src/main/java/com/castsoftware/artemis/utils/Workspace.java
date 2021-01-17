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
import com.castsoftware.artemis.config.LanguageConfiguration;
import com.castsoftware.artemis.config.LanguageProp;
import com.castsoftware.artemis.nlp.SupportedLanguage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Workspace {

    /**
     * Get the current path of the current Artemis Workspace
     * @return Path of the workspace
     */
    public static Path getWorkspacePath() {
        String workspace = Configuration.get("artemis.workspace.folder");
        return Path.of(workspace);
    }

    /**
     * Path to the user configuration file
     * @return
     */
    public static Path getUserConfigPath() {
        String workspace = Configuration.get("artemis.workspace.folder");
        return Path.of(workspace).resolve(Configuration.get("artemis.config.user.conf_file"));
    }

    /**
     * Get the path of the model for a specific language
     * @param language
     * @return
     */
    public static Path getLanguageModelFile(SupportedLanguage language) {
        Path workspace = Path.of(Configuration.get("artemis.workspace.folder"));
        LanguageProp lp = LanguageConfiguration.getInstance().getLanguageProperties(language.toString());

        if(lp == null)
            throw new IllegalArgumentException("No properties exists for language with name :".concat(language.toString()));

        Path modelFile = workspace.resolve(lp.getName());
        return modelFile.resolve(lp.getModelFileName());
    }

    /**
     * Validate if the workspace is valid and contains all the Artemis mandatory files
     * @return List of message to be displayed
     */
    public static List<String> validateWorkspace() {
        List<String> messageOutputList = new ArrayList<>();

        String workspace = Configuration.get("artemis.workspace.folder");
        Path workspacePath = Path.of(workspace);
        Path reportFolder = workspacePath.resolve(Configuration.get("artemis.reports_generator.folder"));
        Path enrichmentFolder = workspacePath.resolve(Configuration.get("artemis.nlp_enrichment.folder"));
        Path headerFilePath = workspacePath.resolve(Configuration.get("artemis.parser.header_file.name"));
        Path confFilePath = workspacePath.resolve(Configuration.get("artemis.config.user.conf_file"));

        LanguageConfiguration lc = LanguageConfiguration.getInstance();
        Map<String, LanguageProp> languagePropMap = lc.getLanguageMap();

        // Check if the folder is valid
        if (!Files.exists(workspacePath)) {
            messageOutputList.add(String.format("ERROR : %s does not exist. Please specify an existing directory.", workspace));
            return messageOutputList;
        }

        // Check main folders and create if necessary
        if (!Files.exists(reportFolder)) {
            try {
                Files.createDirectory(reportFolder);
                messageOutputList.add("Report folder was missing and has been created.");
            } catch (IOException e) {
                messageOutputList.add(String.format("ERROR : Report folder is missing and its creation failed : %s", e.getMessage()));
            }
        }

        // Enrichment folder
        if (!Files.exists(enrichmentFolder)) {
            try {
                Files.createDirectory(enrichmentFolder);
                messageOutputList.add("Enrichment folder was missing and has been created.");
            } catch (IOException e) {
                messageOutputList.add(String.format("ERROR : Enrichment folder is missing and its creation failed : %s", e.getMessage()));
            }
        }

        // Check the existent of the user agent file
        if (!Files.exists(headerFilePath)) {
            messageOutputList.add(String.format("ERROR : Header file '%s' is missing. The Google parse will not work without this file.", Configuration.get("artemis.parser.header_file.name")));
        }

        // Check the existent of the user configuration file
        if (!Files.exists(confFilePath)) {
            messageOutputList.add(String.format("ERROR : Configuration file '%s' is missing. The SMTP parse will not work without this file.", Configuration.get("artemis.config.user.conf_file")));
        }

        // Check languages & associated models
        Path languageFolder;
        Path modelFile;
        LanguageProp lp;
        for(Map.Entry<String, LanguageProp> en : languagePropMap.entrySet()) {
            lp = en.getValue();
            languageFolder = Path.of(workspace, lp.getName());

            if (!Files.exists(languageFolder)) {
                try {
                    Files.createDirectory(languageFolder);
                    messageOutputList.add(String.format("%s folder was missing and has been created.", lp.getName()));
                } catch (IOException e) {
                    messageOutputList.add(String.format("ERROR : %s folder is missing and its creation failed. The detection will not be possible for this language. Error : %s.", lp.getName(), e.getMessage()));
                }
            }

            modelFile = languageFolder.resolve(lp.getModelFileName());
            if (!Files.exists(modelFile)) {
                messageOutputList.add(String.format("ERROR : The model file is missing for language : %s. The detection will not be possible for this language.", lp.getName()));
            }
        }

        return messageOutputList;
    }
}
