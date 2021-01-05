package com.castsoftware.artemis.controllers;

import com.castsoftware.artemis.repositories.Crawler;
import com.castsoftware.artemis.repositories.SPackage;
import com.castsoftware.artemis.repositories.github.Github;
import com.castsoftware.artemis.repositories.maven.Maven;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepositoriesController {

    private static Map<String, Crawler> repositoryCrawlers;
    static {
        repositoryCrawlers = new HashMap<>();
        repositoryCrawlers.put("Maven", new Github());
        repositoryCrawlers.put("Github", new Maven());
    }

    /**
     * Get up to the 4 best matches in the repositories
     * @param packageName Name of the package to search
     * @param candidatesRepository List of the repositories to crawl
     * @return The list of Package found in the repositories
     */
    public static List<SPackage> getRepositoryMatches(String packageName, List<String> candidatesRepository) {
        List<SPackage> returnPackage = new ArrayList<>();

        Crawler crawler;
        for(String repo : candidatesRepository){
            // Ignore this step if the repository provided doesn't exist
            if(!repositoryCrawlers.containsKey(repo)) {
                continue;
            }

            crawler = repositoryCrawlers.get(repo);
            try {
                List<? extends SPackage> packageDetected = crawler.getResults(packageName, 2);
                returnPackage.addAll(packageDetected);
            } catch (UnirestException e) {
                String msg = String.format("The request to repository with name '%s' failed for the reason : %s", repo, e.getMessage());
                System.err.println(msg);
            }
        }

        return returnPackage;
    }



}
