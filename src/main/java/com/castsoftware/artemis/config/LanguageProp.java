package com.castsoftware.artemis.config;

import java.util.List;

public class LanguageProp {
    Boolean onlineSearch = false;
    Boolean interactionDetector = false;
    String packageDelimiter = "";
    List<String> repositorySearch;

    public Boolean getOnlineSearch() {
        return onlineSearch;
    }

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

    public LanguageProp(Boolean onlineSearch, Boolean interactionDetector, String packageDelimiter, List<String> repositorySearch) {
        this.onlineSearch = onlineSearch;
        this.interactionDetector = interactionDetector;
        this.packageDelimiter = packageDelimiter;
        this.repositorySearch = repositorySearch;
    }

    @Override
    public String toString() {
        return "LanguageProp{" +
                "onlineSearch=" + onlineSearch +
                ", interactionDetector=" + interactionDetector +
                ", packageDelimiter='" + packageDelimiter + '\'' +
                ", repositorySearch=" + repositorySearch +
                '}';
    }
}
