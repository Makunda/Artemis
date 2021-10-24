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

/** Pythia interface for the framework detection pattern */
public class PythiaPattern extends PythiaObject implements JsonDeserializer<PythiaPattern> {
  public PythiaLanguage language;
  public String pattern;
  public Boolean isRegex;

  public PythiaPattern(PythiaLanguage language, String pattern, Boolean isRegex) {
    this.language = language;
    this.pattern = pattern;
    this.isRegex = isRegex;
  }

  @Override
  public JSONObject toJson() {
    JSONObject object = new JSONObject();
    object.put("language", language.toJson());
    object.put("pattern", pattern);
    object.put("isRegex", isRegex);
    return object;
  }

  @Override
  public PythiaPattern deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
    Gson gson = new Gson();

    JsonObject root = jsonElement.getAsJsonObject();
    PythiaLanguage language = gson.fromJson(root.get("language"), PythiaLanguage.class);

    String pattern = root.get("pattern").getAsString();
    Boolean isRegex = root.get("isRegex").getAsBoolean();

    return new PythiaPattern(language, pattern, isRegex);
  }
}
