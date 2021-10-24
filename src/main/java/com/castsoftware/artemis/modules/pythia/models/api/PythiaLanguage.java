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

/**
 * Language model for Pythia
 */
public class PythiaLanguage extends PythiaObject implements JsonDeserializer<PythiaLanguage> {
	public String _id;
	public String name;
	public String description;

	/**
	 * Constructor
	 * @param _id Id coming from the API
	 * @param name Name of the language
	 * @param description Description
	 */
	public PythiaLanguage(String _id, String name, String description) {
		this._id = _id;
		this.name = name;
		this.description = description;
	}

	/**
	 * Serialize the language to JSON
	 * @return A json string
	 */
	@Override
	public JSONObject toJson() {
		JSONObject object = new JSONObject();
		object.put("_id", _id);
		object.put("name", name);
		object.put("description", description);
		return object;
	}

	/**
	 * Deserialize a JSON String using GSON
	 * @param jsonElement Element containing the object
	 * @param type Type of object to deserialize
	 * @param jsonDeserializationContext Context
	 * @return The Pythia Language
	 * @throws JsonParseException
	 */
	@Override
	public PythiaLanguage deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
		JsonObject root = jsonElement.getAsJsonObject();
		String _id = root.get("_id").getAsString();
		String name = root.get("name").getAsString();
		String description = root.get("description").getAsString();

		return new PythiaLanguage(_id, name, description);
	}
}
