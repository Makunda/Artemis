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

package com.castsoftware.artemis.modules.repositories.github;

import com.castsoftware.artemis.modules.repositories.SPackage;
import org.json.JSONObject;

public class GithubPackage implements SPackage {

  private static final String TYPE = "GITHUB PACKAGE";

  private final String name;
  private final String fullName;
  private final String license;
  private final String lastUpdate;
  private final int startCount;

  public GithubPackage(
      String name, String fullName, String license, String lastUpdate, int startCount) {
    this.name = name;
    this.fullName = fullName;
    this.license = license;
    this.lastUpdate = lastUpdate;
    this.startCount = startCount;
  }

  @Override
  public String getFullName() {
    return this.fullName;
  }

  public String getLicence() {
    return this.license;
  }

  public String getName() {
    return name;
  }

  public String getLicense() {
    return license;
  }

  public String getLastUpdate() {
    return lastUpdate;
  }

  public int getStartCount() {
    return startCount;
  }

  public Double getConfidence() {
    return 0d;
  }

  @Override
  public JSONObject toJson() {
    JSONObject o = new JSONObject();
    o.put("Type", TYPE);
    o.put("FullName", this.getFullName());
    o.put("Name", this.getName());
    o.put("License", this.getLicense());
    o.put("Version", this.getLastUpdate());
    o.put("StarCount", this.getStartCount());
    return o;
  }
}
