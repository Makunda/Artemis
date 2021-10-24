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

package com.castsoftware.artemis.modules.pythia;

import com.castsoftware.artemis.modules.pythia.exceptions.PythiaResponse;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaObject;
import com.castsoftware.artemis.modules.pythia.models.utils.PythiaApiResponse;
import com.castsoftware.artemis.modules.pythia.models.utils.PythiaParameters;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;

import java.util.HashMap;
import java.util.Map;

/** Class in charge of the communication with the API */
public class PythiaProxyCom {
  private final PythiaParameters parameters;

  /**
   * Constructor of the communication proxy
   *
   * @param parameters Pythia parameters
   */
  public PythiaProxyCom(PythiaParameters parameters) {
    this.parameters = parameters;
  }

  /**
   * Define the headers based on the configuration
   *
   * @return Headers for Pythia
   */
  private Map<String, String> getHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("Accept", "application/json");
    headers.put("Authorization", "Bearer " + parameters.getToken());
    return headers;
  }

  /**
   * GET Request proxified
   *
   * @param <T> Type of the class to deserialize
   * @param relativeURL Relative url to query
   * @param type Type of the object returned
   */
  public <T> PythiaApiResponse<T> get(String relativeURL, Class<T> type) throws PythiaResponse {
    String url = parameters.getUrl() + relativeURL;
    HttpResponse<JsonNode> response = Unirest.get(url).headers(this.getHeaders()).asJson();
    return new PythiaApiResponse<T>(response, type);
  }

  /**
   * POST Request proxified
   *
   * @param <T> Type
   * @param relativeURL Relative url to query
   * @param data Data to pass to the API
   */
  public <T> PythiaApiResponse<T> post(String relativeURL, PythiaObject data, Class<T> type) throws PythiaResponse {
    String url = parameters.getUrl() + relativeURL;

    HttpResponse<JsonNode> response =
        Unirest.post(url).headers(this.getHeaders()).body(data.toJson()).asJson();
    return new PythiaApiResponse<T>(response, type);
  }
}
