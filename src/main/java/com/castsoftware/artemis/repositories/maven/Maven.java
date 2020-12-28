package com.castsoftware.artemis.repositories.maven;


import com.castsoftware.artemis.exceptions.repositories.MalformedResultException;
import com.castsoftware.artemis.repositories.Crawler;
import com.castsoftware.artemis.repositories.SPackage;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Maven extends Crawler {

    private static final String URL = "https://search.maven.org/solrsearch/select?q=";
    private static final String OPTIONS = "&rows=5&wt=json";

    private String name;

    private List<SPackage> resultPackages;

    public String getName() {
        return name;
    }

    private List<MavenPackage> buildPackageList(JSONArray jsonObject){
        List<MavenPackage> returnList = new ArrayList<>();
        List objectList = jsonObject.toList();

        System.out.println(objectList.size() + " package were found in maven repository.");

        for(Object o : objectList) {
            if(! (o instanceof JSONObject)) {
                continue;
            }
            JSONObject jo = (JSONObject) o;
            String g = jo.getString("g");
            String a = jo.getString("a");
            String id = jo.getString("id");
            String v = jo.getString("latestVersion");
            MavenPackage jp = new MavenPackage(g, a , id, v);
            returnList.add(jp);
        }

        /**
         * TODO Analyze result full name and return best candidate
         */
        return returnList;
    }


    private List<MavenPackage> getByName() throws UnirestException {
        StringBuilder urlBuilder = new StringBuilder()
                .append(URL)
                .append(this.name)
                .append(OPTIONS);
        JSONObject jsonResult = this.getRequest(urlBuilder.toString()).getObject();
        return buildPackageList((JSONArray) jsonResult.getJSONObject("response").get("docs"));
    }

    @Override
    public List<SPackage> getResults(Integer limit) {
        if(this.resultPackages == null || this.resultPackages.isEmpty()) return null;
        if(resultPackages.size() < limit) limit = resultPackages.size();
        return this.resultPackages.subList(0, limit);
    }

    @Override
    public void setQuery(String fullName) throws MalformedResultException {
        String[] matches = fullName.split("\\.");
        if(matches.length >= 3) {
            try {
                this.name =  String.join("%20", Arrays.copyOfRange(matches, 0, 3));
            } catch (Exception e ) {
                throw new MalformedResultException("The result query is not in a valid format.", "MAV");
            }
        } else {
            this.name = fullName;
        }
    }

}
