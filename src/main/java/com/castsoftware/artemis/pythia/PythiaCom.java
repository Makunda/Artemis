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

package com.castsoftware.artemis.pythia;

import com.castsoftware.artemis.config.NodeConfiguration;
import com.castsoftware.artemis.config.UserConfiguration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.utils.Workspace;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PythiaCom {

  private static PythiaCom INSTANCE;

  private Neo4jAL neo4jAL;
  private String uri;
  private String token;
  private boolean connected;

  private PythiaCom(Neo4jAL neo4jAL) {
    this.neo4jAL = neo4jAL;

    // If configuration is empty
    if (!UserConfiguration.isKey(neo4jAL, "oracle.server")
        || !UserConfiguration.isKey(neo4jAL, "oracle.token")) {
      neo4jAL.logInfo("The Oracle has not been set up. Please check your configuration");
    }

    // Else get the properties
    this.uri = UserConfiguration.get(neo4jAL, "oracle.server");
    this.token = UserConfiguration.get(neo4jAL, "oracle.token");

    this.connected = pingApi();
  }

  /**
   * Ping the API
   *
   * @return True if the API is reachable, false otherwise
   */
  public boolean pingApi() {
    this.uri = UserConfiguration.get(neo4jAL, "oracle.server");

    // If configuration is empty
    if (!UserConfiguration.isKey(neo4jAL, "oracle.server")
        || !UserConfiguration.isKey(neo4jAL, "oracle.token")) {
      neo4jAL.logInfo(
          String.format(
              "The Oracle has not been set up. Please check your configuration at %s",
              Workspace.getUserConfigPath(neo4jAL).toString()));
      if (!UserConfiguration.isKey(neo4jAL, "oracle.server"))
        neo4jAL.logInfo("Missing oracle.server parameter.");
      if (!UserConfiguration.isKey(neo4jAL, "oracle.token"))
        neo4jAL.logInfo("Missing oracle.token parameter.");
      return false;
    }
    ;

    // Else get the properties
    this.uri = UserConfiguration.get(neo4jAL, "oracle.server");
    this.token = UserConfiguration.get(neo4jAL, "oracle.token");

    if (uri == null || uri.isEmpty()) return false;

    StringBuilder url = new StringBuilder();
    url.append(this.uri);

    try {
      HttpResponse<String> jsonResponse =
          Unirest.get(url.toString())
              .header("accept", "application/json")
              .header("Authorization", "Bearer " + token)
              .asString();

      if (jsonResponse.getStatus() == 200 || jsonResponse.getStatus() == 304) {
        neo4jAL.logInfo(
            String.format(
                "PYTHIA COM : The API is online (%s). Message : %s",
                this.uri, jsonResponse.getBody()));
        return true;
      } else {
        neo4jAL.logError(
            String.format(
                "PYTHIA COM : Failed to connect to the API (%s) with status %d",
                this.uri, jsonResponse.getStatus()));
        return false;
      }
    } catch (Exception e) {
      neo4jAL.logError(
          String.format("PYTHIA COM : Failed to connect to the API (%s) with error.", this.uri), e);
      return false;
    }
  }

  /**
   * Check if the token is set in the configuration
   *
   * @param neo4jAL
   * @return
   */
  public static boolean isSet(Neo4jAL neo4jAL) {
    try {
      return (UserConfiguration.isKey(neo4jAL, "oracle.server")
          && UserConfiguration.isKey(neo4jAL, "oracle.token"));
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Get the instance of the OracleCom
   *
   * @return
   * @throws IOException
   */
  public static PythiaCom getInstance(Neo4jAL neo4jAL) {
    if (INSTANCE == null) {
      INSTANCE = new PythiaCom(neo4jAL);
    }
    return INSTANCE;
  }

  public String getUri() {
    return uri;
  }

  /**
   * Set a new URI for the Pythia
   *
   * @param newURI New URI
   * @return
   * @throws MissingFileException
   */
  public String setUri(String newURI) throws MissingFileException {
    UserConfiguration.set(neo4jAL, "oracle.server", newURI);
    UserConfiguration.saveAndReload(neo4jAL);
    this.uri = newURI;

    return UserConfiguration.get(neo4jAL, "oracle.server");
  }

  /**
   * Set a new token for the Pythia
   *
   * @param newToken New token
   * @return
   * @throws MissingFileException
   */
  public Boolean setToken(String newToken) throws MissingFileException {
    UserConfiguration.set(neo4jAL, "oracle.token", newToken);
    UserConfiguration.saveAndReload(neo4jAL);
    this.token = newToken;

    return newToken == UserConfiguration.get(neo4jAL, "oracle.token");
  }

  /**
   * Check if a token is present in the configuration
   *
   * @return
   */
  public Boolean isTokenPresent() {
    return (token != null && !token.isEmpty());
  }

  /**
   * Verify if the api is online
   *
   * @return
   */
  public boolean getStatus() {
    connected = this.pingApi();
    return connected;
  }

  public boolean getConnected() {
    return connected;
  }

  /**
   * Find a framework
   *
   * @param name Name of the framework
   * @param internalType Internal type of the framework
   * @return
   */
  public FrameworkNode findFramework(String name, String internalType) {
    if (uri == null || uri.isEmpty()) return null;

    StringBuilder url = new StringBuilder();
    url.append(this.uri).append("/api/artemis/frameworks/find");

    FrameworkNode fn = null;

    try {
      Map<String, Object> params = Map.of("name", name, "internalType", internalType);
      HttpResponse<String> pResponse =
          Unirest.post(url.toString())
              .header("accept", "application/json")
              .header("Authorization", "Bearer " + token)
              .body(new JSONObject(params))
              .asString();

      if (pResponse.getStatus() == 200 || pResponse.getStatus() == 304) {
        neo4jAL.logInfo(String.format("Response for the update %s", pResponse.getBody()));
        PythiaResponse pr = new PythiaResponse(pResponse.getBody());

        if (pr.data == null) return null;

        try {
          fn = PythiaUtils.JSONtoFramework(neo4jAL, (JSONObject) pr.data);
          fn.createNode();
        } catch (Neo4jQueryException | Neo4jBadNodeFormatException e) {
          neo4jAL.logError("Failed to retrieve the frameworks from pythias.", e);
        }
      } else {
        neo4jAL.logError(
            String.format(
                "PYTHIA COM : Failed to connect to the API (%s) with status %d",
                this.uri, pResponse.getStatus()));
      }
    } catch (Exception e) {
      neo4jAL.logError(
          String.format("PYTHIA COM : Failed to connect to the API (%s) with error.", this.uri), e);
    }

    return fn;
  }

  /**
   * Get the last update of Pythia instance
   *
   * @return
   */
  public Long getLastUpdate() {
    if (uri == null || uri.isEmpty()) return null;

    StringBuilder url = new StringBuilder();
    url.append(this.uri).append("/api/repo/lastUpdate");

    try {
      HttpResponse<String> pResponse =
          Unirest.get(url.toString())
              .header("accept", "application/json")
              .header("Authorization", "Bearer " + token)
              .asString();

      if (pResponse.getStatus() == 200 || pResponse.getStatus() == 304) {
        neo4jAL.logInfo(String.format("Response for the update %s", pResponse.getBody()));
        PythiaResponse pr = new PythiaResponse(pResponse.getBody());
        return (Long) pr.data;
      } else {
        neo4jAL.logError(
            String.format(
                "PYTHIA COM : Failed to connect to the API (%s) with status %d",
                this.uri, pResponse.getStatus()));
        return null;
      }
    } catch (Exception e) {
      neo4jAL.logError(
          String.format("PYTHIA COM : Failed to connect to the API (%s) with error.", this.uri), e);
      return null;
    }
  }

  /**
   * Get the number of framework that will be pulled
   *
   * @return
   */
  public Long getPullForecast() throws Neo4jQueryException, Neo4jBadRequestException {
    if (uri == null || uri.isEmpty()) return null;

    NodeConfiguration nc = NodeConfiguration.getInstance(neo4jAL);

    StringBuilder url = new StringBuilder();
    url.append(this.uri)
        .append("/api/repo/forecast/pull")
        .append("?timestamp=")
        .append(nc.getLastUpdate());

    try {
      HttpResponse<String> pResponse =
          Unirest.get(url.toString())
              .header("accept", "application/json")
              .header("Authorization", "Bearer " + token)
              .asString();

      if (pResponse.getStatus() == 200 || pResponse.getStatus() == 304) {
        neo4jAL.logInfo(
            String.format("Response for the last pull forecast %s", pResponse.getBody()));
        PythiaResponse pr = new PythiaResponse(pResponse.getBody());
        Integer temp = (Integer) pr.data;
        return temp.longValue();
      } else {
        neo4jAL.logError(
            String.format(
                "PYTHIA COM : Failed to connect to the API (%s) with status %d",
                this.uri, pResponse.getStatus()));
        return null;
      }
    } catch (Exception e) {
      neo4jAL.logError(
          String.format("PYTHIA COM : Failed to connect to the API (%s) with error.", this.uri), e);
      return null;
    }
  }

  /**
   * Pull the list of the new frameworks into the database ( in case of conflict, the user data will
   * not be overridden )
   *
   * @return The list of framework retrieved
   */
  public List<FrameworkNode> pullFrameworks()
      throws Neo4jQueryException, Neo4jBadRequestException, UnirestException {
    NodeConfiguration nodeConf = NodeConfiguration.getInstance(neo4jAL);
    Long lastUpdate = nodeConf.getLastUpdate();

    StringBuilder url = new StringBuilder();
    url.append(this.uri).append("/api/repo/pull?timestamp=").append(lastUpdate);

    // Retrieve the list of frameworks younger than this timestamp
    HttpResponse<String> pResponse =
        Unirest.get(url.toString())
            .header("accept", "application/json")
            .header("Authorization", "Bearer " + token)
            .asString();

    // Treat list of Frameworks
    List<FrameworkNode> pulledFrameworks = new ArrayList<>();
    PythiaResponse pr = new PythiaResponse(pResponse.getBody());
    try {
      JSONArray jsonArr = new JSONArray((String) pr.data);
      for (Object o : jsonArr) {
        if (o instanceof JSONObject) {
          FrameworkNode fn = PythiaUtils.JSONtoFramework(neo4jAL, (JSONObject) o);
          fn.createNode();
          pulledFrameworks.add(fn);
        } else {
          neo4jAL.logError(String.format("Malformed Json entry skipped. Entry : %s", o.toString()));
        }
      }

    } catch (JSONException | Neo4jBadNodeFormatException ex) {
      neo4jAL.logError("Failed to pull frameworks from Pythia Incorrect response format.", ex);
    }

    // Update timestamp if some frameworks were pulled
    if (!pulledFrameworks.isEmpty()) {
      nodeConf.updateLastUpdate();
    }

    return pulledFrameworks;
  }

  /**
   * Check the status
   *
   * @return
   */
  public boolean isConnected() {
    return connected;
  }

  /**
   * Send a Framework to the oracle in an another thread
   *
   * @param frameworkNode Framework to send to the Oracle
   * @return
   */
  public void asyncSendFramework(FrameworkNode frameworkNode) {
    new Thread(() -> {
      try {
        this.sendFramework(frameworkNode);
        System.out.println(String.format("Async :: Send Framework : Framework %s was sent to Pythia.", this.toString()) );
      } catch (Exception e) {
        System.err.println(String.format("Async :: Send Framework : Failed to send the framework to Pythia. Error : %s.", e.getLocalizedMessage()) );
      }
    }).start();
  }

  /**
   * Send a Framework to the oracle
   *
   * @param frameworkNode Framework to send to the Oracle
   * @return
   */
  public boolean sendFramework(FrameworkNode frameworkNode) {

    StringBuilder url = new StringBuilder();
    url.append(this.uri);
    url.append("/api/artemis/frameworks/add");

    try {
      HttpResponse<String> jsonResponse =
          Unirest.post(url.toString())
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + token)
              .body(frameworkNode.toJSON())
              .asString();

      if (jsonResponse.getStatus() != 201 || jsonResponse.getStatus() != 200 ) {
        neo4jAL.logError(
            String.format(
                "PYTHIA COM : Failed to add the framework : %s. Response: %s", frameworkNode.toJSON(), jsonResponse.getBody()));
        return false;
      } else {
        neo4jAL.logInfo(
            String.format(
                "PYTHIA COM : The framework with name %s has been added",
                frameworkNode.toJSON()));
        return true;
      }
    } catch (UnirestException e) {
      neo4jAL.logError(
          String.format(
              "PYTHIA COM : Failed to add the framework (%s) with error : %s",
              frameworkNode.toJSON(), e.getMessage()));
      return false;
    }
  }

  /**
   * Get a Framework from the oracle
   *
   * @param neo4jAL Neo4j Access Layer
   * @param frameworkName Name of the Framework to find
   * @param frameworkInternalType Internal Type of the framework
   * @return
   */
  public FrameworkNode findFramework(
      Neo4jAL neo4jAL, String frameworkName, String frameworkInternalType) {

    StringBuilder url = new StringBuilder();
    url.append(this.uri);
    url.append("/api/artemis/frameworks/");
    url.append(frameworkName);

    if (!frameworkInternalType.isEmpty()) {
      String sanitizedF = frameworkInternalType.replace(" ", "+");
      url.append("?internalType=").append(sanitizedF);
    }

    try {
      HttpResponse<String> pResponse =
          Unirest.get(url.toString())
              .header("accept", "application/json")
              .header("Authorization", "Bearer " + token)
              .asString();

      if (pResponse.getStatus() == 200 || pResponse.getStatus() == 304) {
        PythiaResponse pr = new PythiaResponse(pResponse.getBody());

        neo4jAL.logInfo(
            String.format(
                "PYTHIA COM : Framework with name '%s' has been found : %s",
                frameworkName, pr.toString()));

        return PythiaUtils.JSONtoFramework(neo4jAL, new JSONObject(pr.data));
      } else {
        neo4jAL.logError(
            String.format(
                "PYTHIA COM : Failed to retrieve framework with name : %s%n. Code : (%d)",
                frameworkName, pResponse.getStatus()));
        return null;
      }
    } catch (UnirestException | Neo4jQueryException | Neo4jBadNodeFormatException e) {
      neo4jAL.logError(
          String.format(
              "PYTHIA COM :  Failed to retrieve framework with name : %s. Error : %s",
              frameworkName, e.getMessage()));
      return null;
    }
  }
}
