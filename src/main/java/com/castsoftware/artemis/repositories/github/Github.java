package com.castsoftware.artemis.repositories.github;

import com.castsoftware.artemis.repositories.Crawler;
import com.castsoftware.artemis.repositories.SPackage;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Github extends Crawler {

    private static final String URL = "https://api.github.com/search/repositories?q=";
    private static final String OPTIONS = "&per_page=10&page=1";

    private List<SPackage> resultPackages;

    /**
     * Build the package list from the result of the query
     * @param jsonObject
     * @return
     */
    private List<GithubPackage> buildPackageList(JSONArray jsonObject){
        List<GithubPackage> returnList = new ArrayList<>();

        Iterator<Object> it = jsonObject.iterator();
        while(it.hasNext()) {
            Object o = it.next();

            if(! (o instanceof JSONObject)) {
                continue;
            }
            JSONObject jo = (JSONObject) o;
            String n = jo.getString("name");
            String fn = jo.getString("full_name");

            String licenses = "";
            if( jo.get("license") != null){
                licenses = jo.getJSONObject("license").getString("key");
            }
            String ud = jo.getString("updated_at");
            int sc = jo.getInt("stargazers_count");

            GithubPackage gp = new GithubPackage(n, fn, licenses, ud, sc);
            returnList.add(gp);
        }
        /**
         * TODO Analyze result full name and return best candidate
         */
        return returnList;
    }

    /**
     * Get the results for this specific instance
     * @return
     */
    public List<GithubPackage> getGithubPackages(String search) throws UnirestException {
        StringBuilder urlBuilder = new StringBuilder()
                .append(URL)
                .append(search)
                .append(OPTIONS);
        JSONObject jsonResult = getRequest(urlBuilder.toString()).getObject();
        return buildPackageList((JSONArray) jsonResult.get("items"));
    }

    @Override
    public List<GithubPackage> getResults(String search, Integer limit) throws UnirestException {
        List<GithubPackage> packages = getGithubPackages(search);

        if(packages == null || packages.isEmpty()) return null;
        if(packages.size() < limit) limit = packages.size();

        return packages.subList(0, limit);
    }

}
