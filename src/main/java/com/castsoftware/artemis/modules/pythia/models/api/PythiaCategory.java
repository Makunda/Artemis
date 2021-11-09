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

package com.castsoftware.artemis.modules.pythia.models.api;

import com.google.gson.*;
import kong.unirest.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Category attached to the Framework
 */
public class PythiaCategory extends PythiaObject implements JsonDeserializer<PythiaCategory> {

	public String title;
	public String description;
	public List<String> tags;
	public Boolean isRoot;

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public List<String> getTags() {
		return tags;
	}

	public Boolean getRoot() {
		return isRoot;
	}

	/**
	 * Constructor
	 * @param title	Title that will be displayed
	 * @param description Description of the fraemwork
	 * @param tags Tags
	 * @param isRoot Consider as root
	 */
	public PythiaCategory(String title, String description, List<String> tags, Boolean isRoot) {
		this.title = title;
		this.description = description;
		this.tags = tags;
		this.isRoot = isRoot;
	}

	@Override
	public JSONObject toJson() {
		JSONObject object = new JSONObject();
		object.put("title", title);
		object.put("description", description);
		object.put("tags", tags);
		object.put("isRoot", isRoot);
		return object;
	}

	@Override
	public PythiaCategory deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
		JsonObject root = jsonElement.getAsJsonObject();

		// Get elements in the JSON
		String title = root.get("title").getAsString();
		String description = root.get("description").getAsString();
		Boolean isRoot = root.get("isRoot").getAsBoolean();

		return new PythiaCategory(title, description, new ArrayList<>(), isRoot);
	}
}
