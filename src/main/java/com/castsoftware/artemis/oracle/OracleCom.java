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

package com.castsoftware.artemis.oracle;

import com.castsoftware.artemis.config.UserConfiguration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import org.neo4j.logging.Log;

import java.io.IOException;

public class OracleCom {

    private static OracleCom INSTANCE;

    private Log log;
    private String uri;
    private String token;
    private boolean connected;

    /**
     * Ping the API
     * @return True if the API is reachable, false otherwise
     */
    public boolean pingApi()  {

        StringBuilder url = new StringBuilder();
        url.append(this.uri);

        try {
            HttpResponse<String> jsonResponse
                    = Unirest.get(url.toString())
                    .header("accept", "application/json")
                    .header("Authorization", "Bearer "+token)
                    .asString();

            if (jsonResponse.getStatus() != 200) {
                log.error(String.format("ORACLE COM : Failed to connect to the API (%s) with status %d", this.uri, jsonResponse.getStatus()));
                return false;
            } else {
                log.info(String.format("ORACLE COM : The API is online (%s). Message : %s", this.uri, jsonResponse.getBody()));
                return true;
            }
        } catch (UnirestException e) {
            log.error(String.format("ORACLE COM : Failed to connect to the API (%s) with error.", this.uri), e);
            return false;
        }
    }

    /**
     * Verify if the api is online
     * @return
     */
    public boolean getStatus() {
        connected = this.pingApi();
        return connected;
    }

    /**
     * Check the status
     * @return
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Send a Framework to the oracle
     * @param frameworkNode Framework to send to the Oracle
     * @return
     */
    public boolean addFramework(FrameworkNode frameworkNode) {

        StringBuilder url = new StringBuilder();
        url.append(this.uri);
        url.append("/artemis/frameworks");

        try {
            HttpResponse<JsonNode> jsonResponse
                    = Unirest.post(url.toString())
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer "+token)
                    .body(frameworkNode.toJSON())
                    .asJson();

            if (jsonResponse.getStatus() != 201) {
                log.error(String.format("ORACLE COM : Failed to add the framework : %s", frameworkNode.toString()));
                return false;
            } else {
                log.info(String.format("ORACLE COM : The framework with name %s has been added", frameworkNode.toString()));
                return true;
            }
        } catch (UnirestException e) {
            log.error(String.format("ORACLE COM : Failed to add the framework (%s) with error : %s", frameworkNode.toString(), e.getMessage()));
            return false;
        }
    }

    /**
     * Get a Framework from the oracle
     * @param neo4jAL Neo4j Access Layer
     * @param frameworkName Name of the Framework to find
     * @param frameworkInternalType Internal Type of the framework
     * @return
     */
    public FrameworkNode findFramework(Neo4jAL neo4jAL, String frameworkName, String frameworkInternalType) {


        StringBuilder url = new StringBuilder();
        url.append(this.uri);
        url.append("/artemis/frameworks/");
        url.append(frameworkName);

        if(!frameworkInternalType.isEmpty()) {
            String sanitizedF = frameworkInternalType.replace(" ", "+");
            url.append("?internalType=").append(sanitizedF);
        }

        try {
            HttpResponse<JsonNode> jsonResponse
                    = Unirest.get(url.toString())
                    .header("accept", "application/json")
                    .header("Authorization", "Bearer "+token)
                    .asJson();



            if (jsonResponse.getStatus() != 200) {
                log.error("ORACLE COM : Failed to retrieve framework with name : %s%n. Code : (%d)", frameworkName, jsonResponse.getStatus());
                return null;
            } else {

                JSONObject responseJson = jsonResponse.getBody().getObject();

                if(!responseJson.has("data") || responseJson.isNull("data")) {
                    log.error("ORACLE COM : Failed to retrieve framework with name : %s%n. Not found", frameworkName, jsonResponse.getStatus());
                    return null;
                }

                JSONObject frameworkJson = (JSONObject) responseJson.get("data");

                log.info("ORACLE COM : Framework with name '%s' has been found : %s", frameworkName, frameworkJson.toString());

                String name = frameworkJson.getString("name");
                String discoveryDate = frameworkJson.getString("discoveryDate");
                String location = frameworkJson.getString("location");
                String description = frameworkJson.has("description") ? frameworkJson.getString("description") : "";
                String category = frameworkJson.has("category") ? frameworkJson.getString("category") : "";
                String internalType = frameworkJson.getString("internalType");
                Long numberOfDetection = frameworkJson.has("numberOfDetection") ? frameworkJson.getLong("numberOfDetection") : 0L;
                Double percentageDetection = frameworkJson.has("percentageOfDetection") ? frameworkJson.getDouble("percentageOfDetection") : .0;

                String frameworkTypeAsString = frameworkJson.getString("type");
                FrameworkType frameworkType = FrameworkType.getType(frameworkTypeAsString);

                FrameworkNode fn =
                    new FrameworkNode(
                        neo4jAL,
                        name,
                        discoveryDate,
                        location,
                        description,
                        numberOfDetection,
                        percentageDetection);
                fn.setCategory(category);
                fn.setInternalType(internalType);
                fn.setFrameworkType(frameworkType);

                return fn;
            }
        } catch (UnirestException e) {
            log.error(String.format("ORACLE COM :  Failed to retrieve framework with name : %s. Error : %s", frameworkName, e.getMessage()));
            return null;
        }
    }

    /**
     * Get the instance of the OracleCom
     * @return
     * @throws IOException
     */
    public static OracleCom getInstance(Log log) {
        if(INSTANCE == null) {
            INSTANCE = new OracleCom(log);
        }
        return INSTANCE;
    }

    private OracleCom(Log log) {
        this.log = log;

        // If configuration is empty
        if(!UserConfiguration.isKey("oracle.server") || !UserConfiguration.isKey("oracle.token")) {
            log.info("The Oracle has not been set up. Please check your configuration");
        };

        // Else get the properties
        this.uri = UserConfiguration.get("oracle.server");
        this.token = UserConfiguration.get("oracle.token");

        this.connected = pingApi();
    }

}
