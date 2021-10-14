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
import com.castsoftware.artemis.modules.pythia.controllers.PythiaUtilController;
import com.castsoftware.artemis.modules.pythia.exceptions.PythiaException;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaFramework;
import com.castsoftware.artemis.modules.pythia.models.utils.PythiaParameters;

public class Pythia {

  // API Parameters
  private final PythiaParameters parameters;
  private final PythiaUtilController utilController;
  private final PythiaFrameworkController frameworkController;

  /**
   * Get the status of the Pythia connection
   * @return The API Status
   * @throws PythiaException If the request failed
   */
  public String getStatus() throws PythiaException {
    return utilController.getAuthStatus();
  }

  /**
   * Create a Framework on Pythia
   * @param toCreate Framework to create
   * @return The created framework
   * @throws PythiaException If the request failed
   */
  public PythiaFramework createFramework(PythiaFramework toCreate) throws PythiaException {
    return frameworkController.createFramework(toCreate);
  }

  /**
   * Constructor of the Pythia Modulde
   *
   * @param url Url to Pythia
   * @param token Token of Pythia
   */
  public Pythia(String url, String token) {
    this.parameters = new PythiaParameters(url, token);

    this.utilController = new PythiaUtilController(this.parameters);
    this.frameworkController = new PythiaFrameworkController(this.parameters);
  }
}
