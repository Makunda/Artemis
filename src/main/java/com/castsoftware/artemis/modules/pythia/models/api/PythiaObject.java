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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import kong.unirest.json.JSONObject;

import java.lang.reflect.Type;

/**
 * All the object communicating with pythia should be serializable as JSON
 */
public abstract class PythiaObject {

	/**
	 * Transform an object to a JSON
	 * @return Object as Json string
	 */
	public abstract JSONObject toJson();
}
