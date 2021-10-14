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

import com.google.gson.Gson;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.json.JSONObject;

public class PythiaApiResponse<T> {

  private final Integer status;
  private T data;
  private String message = "";
  private String[] errors = new String[0];
  private Boolean success = false;

  /**
   * Build an API Response from the Unirest JSON
   *
   * @param response Response of Unirest
   * @param type Type of object to return
   */
  public PythiaApiResponse(HttpResponse<JsonNode> response, Class<T> type) {
    Gson gson = new Gson();

    // Get the status
    this.status = response.getStatus();

    // Check the value of the status. Must be like 2xx code
    this.success = this.status.toString().startsWith("2");

    // Assign the message, errors and data
    JSONObject body = response.getBody().getObject();

    // Test all the field
    if (body.has("message")) {
      this.message = body.getString("message");
    }

    // Check for data
    if (body.has("data")) {
      this.data = gson.fromJson(body.getString("data"), type);
    }

    // Check for errors
    if (body.has("errors")) {
      this.errors = gson.fromJson(body.getString("errors"), String[].class);
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

  /**
   * Get the list of errors
   *
   * @return List of the errors
   */
  public String[] getErrors() {
    return errors;
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
