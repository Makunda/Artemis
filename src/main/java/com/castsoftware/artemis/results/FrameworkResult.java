package com.castsoftware.artemis.results;

public class FrameworkResult {
    public String name;
    public String description;
    public String category;
    public String detectedAs;

    public FrameworkResult(String name, String description, String category, String detectedAs) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.detectedAs = detectedAs;
    }
}
