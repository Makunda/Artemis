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
import com.castsoftware.artemis.modules.pythia.models.api.PythiaObject;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaPattern;
import kong.unirest.json.JSONObject;

import java.util.List;
import java.util.regex.Pattern;

public class CreateFrameworkBody  extends PythiaObject {
	public String name;
	public String imagingName;
	public String description;
	public String location;
	public List<String> tags;
	public Boolean isRoot;
	public String detectionData;

	public List<PythiaPattern> patterns;

	public CreateFrameworkBody(PythiaFramework pf, List<PythiaPattern> pp) {
		this.name = pf.name;
		this.imagingName = pf.imagingName;
		this.description = pf.description;
		this.location = pf.location;
		this.tags  = pf.tags;
		this.isRoot = pf.isRoot;
		this.detectionData = pf.detectionData;
		this.patterns = pp;
	}


	@Override
	public JSONObject toJson() {
		JSONObject object = new JSONObject();
		object.put("name", name);
		object.put("imagingName", imagingName);
		object.put("description", description);
		object.put("location", location);
		object.put("detectionData", detectionData);
		object.put("patterns", patterns);
		return object;
	}
}
