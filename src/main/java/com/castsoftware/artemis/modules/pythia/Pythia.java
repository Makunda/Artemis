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

import com.castsoftware.artemis.modules.pythia.controllers.PythiaFrameworkController;
import com.castsoftware.artemis.modules.pythia.controllers.PythiaLanguageController;
import com.castsoftware.artemis.modules.pythia.controllers.PythiaUtilController;
import com.castsoftware.artemis.modules.pythia.exceptions.PythiaException;
import com.castsoftware.artemis.modules.pythia.exceptions.PythiaResponse;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaFramework;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaLanguage;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaPattern;
import com.castsoftware.artemis.modules.pythia.models.utils.PythiaParameters;

import java.util.List;

public class Pythia {

  // API Parameters
  private final PythiaParameters parameters;
  private final PythiaUtilController utilController;
  private final PythiaFrameworkController frameworkController;
  private final PythiaLanguageController languageController;

  /**
   * Get the status of the Pythia connection
   * @return The API Status
   * @throws PythiaException If the request failed
   */
  public String getStatus() throws PythiaException, PythiaResponse {
    return utilController.getAuthStatus();
  }

  /**
   * Create a Framework on Pythia
   * @param framework Framework to create
   * @param patterns Framework to create
   * @return The created framework
   * @throws PythiaException If the request failed
   */
  public PythiaFramework createFramework(PythiaFramework framework, List<PythiaPattern> patterns) throws PythiaException, PythiaResponse {
    return frameworkController.createFramework(framework, patterns);
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
    return frameworkController.findFrameworkByPattern(pattern, language);
  }

  /**
   * Find a supported language in the Pythia Database
   *
   * @param language Language to search
   * @return A pythia Language object
   * @throws PythiaException if nothing has been found
   * @throws PythiaResponse If the query produced an error
   */
  public PythiaLanguage findLanguage(String language) throws PythiaResponse, PythiaException {
    return languageController.searchLanguage(language);
  }

  /**
   * Constructor of the Pythia Module
   * @param url Url to Pythia
   * @param token Token of Pythia
   */
  public Pythia(String url, String token) {
    this.parameters = new PythiaParameters(url, token);

    this.utilController = new PythiaUtilController(this.parameters);
    this.frameworkController = new PythiaFrameworkController(this.parameters);
    this.languageController = new PythiaLanguageController(this.parameters);
  }
}
