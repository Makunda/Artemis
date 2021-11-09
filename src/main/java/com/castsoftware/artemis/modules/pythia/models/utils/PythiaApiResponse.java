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

package com.castsoftware.artemis.modules.pythia.models.utils;

import com.castsoftware.artemis.modules.pythia.exceptions.PythiaResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PythiaApiResponse<T> {

  private final Integer status;
  private T data;
  private String rawData = "";
  private String message = "";
  private String rawError = "";
  private List<String> errors = new ArrayList<>();
  private Boolean success = false;

  /**
   * Build an API Response from the Unirest JSON
   *
   * @param response Response of Unirest
   * @param type Type of object to return
   */
  public PythiaApiResponse(HttpResponse<JsonNode> response, Class<T> type) throws PythiaResponse {
    Gson gson = new Gson();

    // Get the status
    this.status = response.getStatus();

    // Check the value of the status. Must be like 2xx code
    this.success = this.status.toString().startsWith("2");
    this.rawData = response.getBody().toString();

    // Assign the message, errors and data
    JSONObject body = response.getBody().getObject();

    // Test all the field
    if (body.has("message")) {
      this.message = body.getString("message");
      System.out.println("Detected message " + this.message);
    }

    // Check for data
    if (body.has("data")) {
      try {
        if(!body.isNull("data")) this.data = gson.fromJson(body.get("data").toString(), type);
        else this.data = null;
      } catch (Exception err) {
        this.errors.add(String.format("Failed to deserialize %s. Error: %s", type.getName(), err.getMessage()));
        this.data = null;
      }
    }

    // Check for errors
    if (body.has("errors")) {
      try {
        this.rawError = body.getString("errors");
        Type typeList = new TypeToken<List<String[]>>() {}.getType();
        this.errors = gson.fromJson(body.getString("errors"), typeList);
      } catch (Exception err) {
        this.errors.add("Failed to retrieve the error List. Bad formatting.");
      }
    }
  }

  /**
   * Get the Data returned by the query
   *
   * @return
   */
  public T getData() {
    return data;
  }

  /**
   * Get the message from the API
   *
   * @return The Message
   */
  public String getMessage() {
    return message;
  }

  public Boolean hasErrors() { return !errors.isEmpty(); }
  /**
   * Get the list of errors
   *
   * @return List of the errors
   */
  public List<String> getErrors() {
    return errors;
  }

  /**
   * Return the list of error as a string
   * @return List of errors as string
   */
  public String getErrorsAsString() {
    return String.join(" - ", errors);
  }

  public String getRawError() { return rawError ; }

  public String getRawData() {
    return rawData;
  }

  /**
   * Get the HTTP status of the query
   *
   * @return The Status
   */
  public Integer getStatus() {
    return status;
  }

  /**
   * Get the state of the response
   *
   * @return True if the query has a good Status code, false otherwise
   */
  public Boolean isSuccess() {
    return success;
  }
}
