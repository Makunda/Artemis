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

package com.castsoftware.artemis.repositories.maven;

import com.castsoftware.artemis.repositories.SPackage;
import org.json.JSONObject;

public class MavenPackage implements SPackage {

    private static final String TYPE = "MAVEN PACKAGE";
    
    private String groupId;
    private String name;
    private String fullName;
    private String version;
    private String[] tags;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public Double getConfidence() {
        return 0d;
    }

    @Override
    public String getLicence() {
        return "No license information";
    }

    @Override
    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        o.put("Type", TYPE);
        o.put("FullName", this.getFullName());
        o.put("Name", this.getName());
        o.put("GroupId", this.getGroupId());
        o.put("Version", this.getVersion());
        o.put("Tags", this.getTags());
        return o;
    }


    public MavenPackage(String groupId, String name, String fullName, String version) {
        this.groupId = groupId;
        this.name = name;
        this.fullName = fullName;
        this.version = version;
    }
}
