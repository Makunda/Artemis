package com.castsoftware.artemis.repositories.github;

import com.castsoftware.artemis.repositories.Crawler;
import com.castsoftware.artemis.repositories.SPackage;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;


import java.util.ArrayList;
import java.util.List;

public class Github extends Crawler {

    private static final String URL = "https://api.github.com/search/repositories?q=";
    private static final String OPTIONS = "&per_page=10&page=1";

    private String query;
    private List<SPackage> resultPackages;

    /**
     * Build the package list from the result of the query
     * @param jsonObject
     * @return
     */
    private List<GithubPackage> buildPackageList(JSONArray jsonObject){
        List<GithubPackage> returnList = new ArrayList<>();
        List objectList = jsonObject.toList();

        for(Object o : objectList) {
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
    private List<GithubPackage> getByName() throws UnirestException {
        StringBuilder urlBuilder = new StringBuilder()
                .append(URL)
                .append(this.query)
                .append(OPTIONS);
        JSONObject jsonResult = getRequest(urlBuilder.toString()).getObject();
        return buildPackageList((JSONArray) jsonResult.get("items"));
    }

    @Override
    public List<SPackage> getResults(Integer limit) {
        if(this.resultPackages == null || this.resultPackages.isEmpty()) return null;
        if(resultPackages.size() < limit) limit = resultPackages.size();
        return this.resultPackages.subList(0, limit);
    }

    @Override
    public void setQuery(String query) {
        this.query = query;
    }

}
