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
import com.castsoftware.artemis.modules.pythia.exceptions.PythiaException;
import com.castsoftware.artemis.modules.pythia.exceptions.PythiaResponse;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaLanguage;
import com.castsoftware.artemis.modules.pythia.models.utils.PythiaApiResponse;
import com.castsoftware.artemis.modules.pythia.models.utils.PythiaParameters;

public class PythiaLanguageController extends PythiaController {


	/**
	 * Constructor
	 *
	 * @param parameters Pythia parameters
	 */
	public PythiaLanguageController(PythiaParameters parameters) {
		super(parameters);
	}

	/**
	 * Get the language object  of Pythia
	 *
	 * @return Language object on pythia
	 */
	public PythiaLanguage searchLanguage(String search) throws PythiaException, PythiaResponse {
		String url = "api/language/search?search=" + search;
		PythiaApiResponse<PythiaLanguage> response = this.pythiaProxyCom.get(url, PythiaLanguage.class);

		if (response.isSuccess()) {
			// Return value if success
			return response.getData();
		}
		// Response is not a success
		throw new PythiaException("Failed to find the language", response.getErrors().toArray(String[]::new));
	}
}
