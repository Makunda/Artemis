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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class PythiaImagingFramework extends PythiaObject implements JsonDeserializer<PythiaImagingFramework> {


	public String language;
	public String pattern;
	public String name;
	public String description;
	public String location;

	public Boolean isRoot;

	// Taxonomy
	public String level1;
	public String level2;
	public String level3;
	public String level4;
	public String level5;

	public Date createdOn;
	public Date lastModified;

	public Integer views;

	/**
	 * Constructor of a Pythia Framework
	 * @param language Language of the detection
	 * @param pattern Pattern of the detection
	 * @param name Name
	 * @param description Description
	 * @param level1 Level 1
	 * @param level2 Level 2
	 * @param level3 Level 3
	 * @param level4 Level 4
	 * @param level5 Level 5
	 * @param createdOn Created on date as a Timestamp
	 * @param lastModified Last modified date
	 * @param views Number of views
	 */
	public PythiaImagingFramework(String language, String pattern, String name,
								  String description, String location, Boolean isRoot, String level1, String level2,
								  String level3, String level4, String level5,
								  Date createdOn, Date lastModified, Integer views) {
		this.language = language;
		this.pattern = pattern;
		this.name = name;
		this.description = description;
		this.location = location;
		this.isRoot = isRoot;
		this.level1 = level1;
		this.level2 = level2;
		this.level3 = level3;
		this.level4 = level4;
		this.level5 = level5;
		this.createdOn = createdOn;
		this.lastModified = lastModified;
		this.views = views;
	}


	@Override
	public JSONObject toJson() {
		JSONObject object = new JSONObject();
		object.put("name", name);
		object.put("language", language);
		object.put("pattern", pattern);
		object.put("description", description);
		object.put("location", location);
		object.put("isRoot", isRoot);

		object.put("level1", level1);
		object.put("level2", level2);
		object.put("level3", level3);
		object.put("level4", level4);
		object.put("level5", level5);

		object.put("createdOn", createdOn.toString());
		object.put("lastModified", lastModified.toString());

		object.put("views", views);

		return object;
	}

	@Override
	public PythiaImagingFramework deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
		JsonObject root = jsonElement.getAsJsonObject();

		// Get elements in the JSON
		String name = root.get("name").getAsString();
		String language = root.get("language").getAsString();
		String pattern = root.get("pattern").getAsString();
		String description = root.get("description").getAsString();
		String location = root.get("location").getAsString();

		Boolean isRoot = false;
		if(!root.has("isRoot")) root.get("isRoot").getAsBoolean();

		// Taxonomy
		String level1 = root.get("level1").getAsString();
		String level2 = root.get("level2").getAsString();
		String level3 = root.get("level3").getAsString();
		String level4 = root.get("level4").getAsString();
		String level5 = root.get("level5").getAsString();

		String createdOn = root.get("createdOn").getAsString();
		String lastModified = root.get("lastModified").getAsString();

		Date createdOnDate;
		Date lastModifiedDate;

		try {
			createdOnDate = new SimpleDateFormat().parse(createdOn);
			lastModifiedDate = new SimpleDateFormat().parse(lastModified);
		} catch (ParseException e) {
			createdOnDate = new Date();
			lastModifiedDate = new Date();
		}

		Integer views = root.get("lastModified").getAsInt();

		return new PythiaImagingFramework(language, pattern, name, description, location, isRoot,
				level1, level2, level3, level4, level5, createdOnDate, lastModifiedDate, views);
	}

	/**
	 * Get the formatted cast Taxonomy separated by two #
	 * @return
	 */
	public String getFormattedTaxonomy() {
		List<String> levels =  List.of(this.level1, this.level2, this.level3, this.level4, this.level5);
		return String.join("##", levels);
	}

	public String getLanguage() {
		return language;
	}

	public String getPattern() {
		return pattern;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getLocation() {
		return location;
	}

	public String getLevel1() {
		return level1;
	}

	public String getLevel2() {
		return level2;
	}

	public String getLevel3() {
		return level3;
	}

	public String getLevel4() {
		return level4;
	}

	public String getLevel5() {
		return level5;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public Integer getViews() {
		return views;
	}

	public Boolean getRoot() {
		return false;
	}

	public void setLevel1(String level1) {
		this.level1 = level1;
	}

	public void setLevel2(String level2) {
		this.level2 = level2;
	}

	public void setLevel3(String level3) {
		this.level3 = level3;
	}

	public void setLevel4(String level4) {
		this.level4 = level4;
	}

	public void setLevel5(String level5) {
		this.level5 = level5;
	}

	public void setLevel5asAPI(String level5) {
		String cap = level5.substring(0, 1).toUpperCase() + level5.substring(1);
		cap = cap.replaceAll("(?i)API", "");
		this.level5 = String.format("API %s", cap);
	}

	public void setPattern(String pattern, String delimiter) {
		// remove dot at the end
		if(pattern.endsWith(delimiter)) {
			pattern = pattern.length() == 0
					? ""
					: pattern.substring(0, pattern.length() - 1);
		}

		this.pattern = pattern;
	}
}
