package com.castsoftware.artemis.config;

import java.util.List;

public class LanguageProp {
    String name;
    Boolean onlineSearch = false;
    Boolean interactionDetector = false;
    String packageDelimiter = "";
    List<String> repositorySearch;
    List<String> objectsInternalType;

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

    public LanguageProp() {
    }

    public LanguageProp(String name, Boolean onlineSearch, Boolean interactionDetector, String packageDelimiter, List<String> repositorySearch, List<String> objectsInternalType) {
        this.name = name;
        this.onlineSearch = onlineSearch;
        this.interactionDetector = interactionDetector;
        this.packageDelimiter = packageDelimiter;
        this.repositorySearch = repositorySearch;
        this.objectsInternalType = objectsInternalType;
    }


}
