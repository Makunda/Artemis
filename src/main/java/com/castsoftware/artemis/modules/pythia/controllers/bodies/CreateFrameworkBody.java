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

package com.castsoftware.artemis.modules.pythia.controllers.bodies;

import com.castsoftware.artemis.modules.pythia.models.api.PythiaFramework;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaFramework;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaObject;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaPattern;
import kong.unirest.json.JSONObject;

import java.util.List;

public class CreateFrameworkBody  extends PythiaObject {
	public String name;
	public String level5;
	public String level4;
	public String level3;
	public String level2;
	public String level1;
	public String description;
	public String location;
	public List<String> tags;
	public Boolean isRoot;
	public String detectionData;

	public List<PythiaPattern> patterns;

	/**
	 * Create a body to be sent to pythia for a Framework generation
	 * @param pf Pythia Framework to create
	 * @param pp List of patterns to bind
	 */
	public CreateFrameworkBody(PythiaFramework pf, List<PythiaPattern> pp) {
		this.name = pf.name;

		// Taxonomy
		this.level5 = pf.level5;
		this.level4 = pf.level4;
		this.level3 = pf.level3;
		this.level2 = pf.level2;
		this.level1 = pf.level1;

		// Properties
		this.description = pf.description;
		this.location = pf.location;
		this.tags  = pf.tags;
		this.isRoot = pf.isRoot;

		// Detection pattern
		this.detectionData = pf.detectionData;
		this.patterns = pp;
	}


	@Override
	public JSONObject toJson() {
		JSONObject object = new JSONObject();
		object.put("name", name);

		object.put("level5", level5);
		if(level4 == null) object.put("level4", level4);
		if(level3 == null) object.put("level3", level3);
		if(level2 == null) object.put("level2", level2);
		if(level1 == null) object.put("level1", level1);

		object.put("description", description);
		object.put("location", location);
		object.put("detectionData", detectionData);
		object.put("patterns", patterns);
		return object;
	}
}
