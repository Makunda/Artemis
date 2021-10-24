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

package com.castsoftware.artemis.modules.pythia.controllers;

import com.castsoftware.artemis.modules.pythia.PythiaProxyCom;
import com.castsoftware.artemis.modules.pythia.controllers.bodies.CreateFrameworkBody;
import com.castsoftware.artemis.modules.pythia.exceptions.PythiaException;
import com.castsoftware.artemis.modules.pythia.exceptions.PythiaResponse;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaFramework;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaLanguage;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaPattern;
import com.castsoftware.artemis.modules.pythia.models.utils.PythiaApiResponse;
import com.castsoftware.artemis.modules.pythia.models.utils.PythiaParameters;

import java.util.List;

/** Class handling the communication to pythia, and the different queries */
public class PythiaFrameworkController extends PythiaController{

  /**
   * Constructor
   *
   * @param parameters Pythia parameters
   */
  public PythiaFrameworkController(PythiaParameters parameters) {
    super(parameters);
  }

  /**
   * Create a Framework on pythia
   * @param framework Framework to create
   * @return The Framework created
   */
  public PythiaFramework createFramework(PythiaFramework framework, List<PythiaPattern> patterns) throws PythiaException, PythiaResponse {
    CreateFrameworkBody body = new CreateFrameworkBody(framework, patterns);

    PythiaApiResponse<PythiaFramework> response =
        this.pythiaProxyCom.post("api/framework/pythia/create", body, PythiaFramework.class);

    if (response.isSuccess()) {
      // Return value if success
      return response.getData();
    }
    // Response is not a success
    throw new PythiaException("Failed to get the status", response.getRawError());
  }

  /**
   * Find a framework based on its pattern and Language
   * @param pattern Pattern to search
   * @param language Language to query
   * @return The Framework
   * @throws PythiaException if nothing has been found
   * @throws PythiaResponse If the query produced an error
   */
  public PythiaFramework findFrameworkByPattern(String pattern, String language) throws PythiaException, PythiaResponse {
    String url = "api/framework/pythia/getByPattern?pattern=" + pattern + "&language=" + language;
    PythiaApiResponse<PythiaFramework> response = this.pythiaProxyCom.get(url, PythiaFramework.class);

    if (response.isSuccess()) {
      System.out.printf("Found a framework: %s%n", response.getRawData());
      // Return value if success
      return response.getData();
    }
    // Response is not a success
    throw new PythiaException("Failed to find the framework", response.getErrors().toArray(String[]::new));
  }
}
