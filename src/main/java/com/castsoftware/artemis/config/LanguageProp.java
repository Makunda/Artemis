package com.castsoftware.artemis.config;

import java.util.List;

public class LanguageProp {
    String name;
    Boolean onlineSearch = false;
    Boolean interactionDetector = false;
    String packageDelimiter = "";
    List<String> repositorySearch;
    List<String> objectsInternalType;
    String modelFileName;

    public String getName() { return name; }

    public Boolean getOnlineSearch() {
        return onlineSearch;
    }

    public List<String> getObjectsInternalType() { return objectsInternalType; }

    public Boolean getInteractionDetector() {
        return interactionDetector;
    }

    public String getPackageDelimiter() {
        return packageDelimiter;
    }

    public List<String> getRepositorySearch() {
        return repositorySearch;
    }

    public String getModelFileName() {
        return modelFileName;
    }

    public LanguageProp() {
    }

    public LanguageProp(String name, Boolean onlineSearch, Boolean interactionDetector, String packageDelimiter, List<String> repositorySearch, List<String> objectsInternalType, String modelFileName) {
        this.name = name;
        this.onlineSearch = onlineSearch;
        this.interactionDetector = interactionDetector;
        this.packageDelimiter = packageDelimiter;
        this.repositorySearch = repositorySearch;
        this.objectsInternalType = objectsInternalType;
        this.modelFileName = modelFileName;
    }

    @Override
    public String toString() {
        return "LanguageProp{" +
                "name='" + name + '\'' +
                ", onlineSearch=" + onlineSearch +
                ", interactionDetector=" + interactionDetector +
                ", packageDelimiter='" + packageDelimiter + '\'' +
                ", repositorySearch=" + String.join(" - ",repositorySearch) +
                ", objectsInternalType=" + objectsInternalType +
                ", modelFileName='" + modelFileName + '\'' +
                '}';
    }
}
