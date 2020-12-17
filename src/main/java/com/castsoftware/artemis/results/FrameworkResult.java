package com.castsoftware.artemis.results;

public class FrameworkResult {
    public String name;
    public String description;
    public String detectedAs;

    public FrameworkResult(String name, String description, String detectedAs) {
        this.name = name;
        this.description = description;
        this.detectedAs = detectedAs;
    }
}
