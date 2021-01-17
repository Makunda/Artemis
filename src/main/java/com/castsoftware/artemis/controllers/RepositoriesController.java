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
