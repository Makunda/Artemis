package com.castsoftware.artemis.repositories.github;

import com.castsoftware.artemis.repositories.SPackage;
import org.json.JSONObject;

public class GithubPackage implements SPackage {

    private static final String TYPE = "GITHUB PACKAGE";

    private String name;
    private String fullName;
    private String license;
    private String lastUpdate;
    private int startCount;

    @Override
    public String getFullName() {
        return this.fullName;
    }

    public String getLicence() { return  this.license; }

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

    public GithubPackage(String name, String fullName, String license, String lastUpdate, int startCount) {
        this.name = name;
        this.fullName = fullName;
        this.license = license;
        this.lastUpdate = lastUpdate;
        this.startCount = startCount;
    }
}
