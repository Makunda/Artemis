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

package com.castsoftware.artemis.detector.java;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.controllers.RepositoriesController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.detector.ADetector;
import com.castsoftware.artemis.exceptions.google.GoogleBadResponseCodeException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.exceptions.nlp.NLPBlankInputException;
import com.castsoftware.artemis.nlp.SupportedLanguage;
import com.castsoftware.artemis.nlp.model.NLPResults;
import com.castsoftware.artemis.nlp.parser.GoogleResult;
import com.castsoftware.artemis.repositories.SPackage;
import org.neo4j.graphdb.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JavaDetector extends ADetector {

    private FrameworkTree ft;

    @Override
    public List<FrameworkNode> launch() throws IOException, Neo4jQueryException {
        // Get Dynamic parameters
        boolean onlineMode = Boolean.parseBoolean(Configuration.get("artemis.onlineMode"));
        boolean learningMode = Boolean.parseBoolean(Configuration.get("artemis.learning_mode"));

        // Init properties
        List<FrameworkNode> returnList = new ArrayList<>();

        try {

            // Build the framework tree
            for(Node n : toInvestigateNodes) {

                // Ignore object without a fullname property
                if (!n.hasProperty(IMAGING_OBJECT_FULL_NAME)) continue;
                String fullName = (String) n.getProperty(IMAGING_OBJECT_FULL_NAME);
                String objectName = (String) n.getProperty(IMAGING_OBJECT_NAME);
                String internalType = (String) n.getProperty(IMAGING_INTERNAL_TYPE);

                // Insert the package in the tree
                ft.insert(fullName);

                FrameworkNode fb = FrameworkNode.findFrameworkByName(neo4jAL, fullName);
                if (fb != null) {
                    neo4jAL.logInfo(
                            String.format(
                                    "The object with fullName '%s' is already known by Artemis as a '%s'.",
                                    fullName, fb.getFrameworkType()));
                }

                if(fb == null) {
                    // Parse repositories with object name
                    if (!languageProperties.getRepositorySearch().isEmpty()) {
                        List<SPackage> sPackageList =
                                RepositoriesController.getRepositoryMatches(
                                        fullName, languageProperties.getRepositorySearch());
                        for (SPackage sp : sPackageList) {
                            neo4jAL.logInfo(
                                    String.format(
                                            "Package detected for object with name '%s' : '%s'. ",
                                            fullName, sp.toJson().toString()));
                        }
                    }

                    // Parse google
                    if(googleParser != null && onlineMode && languageProperties.getOnlineSearch()) {
                        String toSearch = String.format("%s %s", objectName, languageProperties.getName());
                        GoogleResult gr = googleParser.request(toSearch);
                        String requestResult = gr.getContent();
                        neo4jAL.logInfo(
                                " - Name of the package to search : "
                                        + objectName
                                        + "\n\t - Results : "
                                        + gr.getNumberResult()
                                        + "\n\t - Blacklisted : "
                                        + gr.isBlacklisted());
                        NLPResults nlpResult = nlpEngine.getNLPResult(requestResult);

                        // Apply a malus on Node with name containing number, exclude it

                        fb = saveFrameworkResult(objectName, nlpResult, internalType);

                        if (learningMode) {
                            nlpSaver.writeNLPResult(nlpResult.getCategory(), requestResult);
                        }
                    }
                }
            }


            // Analyze Framework tree
            neo4jAL.logInfo("Displaying framework tree.");
            ft.print();

        } catch (Exception | Neo4jBadNodeFormatException | GoogleBadResponseCodeException | NLPBlankInputException e) {
            neo4jAL.logError("An error occurred during the detection.", e);
        }


        // Get the NLP results on undetected

        return returnList;
    }

    /**
     * Detector for the Java
     *
     * @param neo4jAL     Neo4j Access Layer
     * @param application Name of the application
     * @throws IOException
     * @throws Neo4jQueryException
     */
    public JavaDetector(Neo4jAL neo4jAL, String application) throws IOException, Neo4jQueryException {
        super(neo4jAL, application, SupportedLanguage.JAVA);
        this.ft = new FrameworkTree();
    }


}
