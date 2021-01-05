package com.castsoftware.artemis.repositories.maven;


import com.castsoftware.artemis.exceptions.repositories.MalformedResultException;
import com.castsoftware.artemis.repositories.Crawler;
import com.castsoftware.artemis.repositories.SPackage;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


public class Maven extends Crawler {

    private static final String URL = "https://search.maven.org/solrsearch/select?q=";
    private static final String OPTIONS = "&rows=5&wt=json";

    /**
     * Build a list of maven package from the JSON results
     * @param jsonObject Json object containing the maven package
     * @return
     */
    private List<MavenPackage> buildPackageList(JSONArray jsonObject){
        List<MavenPackage> returnList = new ArrayList<>();

        Iterator<Object> it = jsonObject.iterator();
        while(it.hasNext()) {
            Object o = it.next();
            // Ignore if the object is not a JSON Object
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

    /**
     * Request the Maven repository and get a list of packets matching the search
     * @param search Name of the package to search
     * @return The list of best matching packets
     * @throws UnirestException
     */
    public List<MavenPackage> getMavenPackages(String search) throws UnirestException {
        StringBuilder urlBuilder = new StringBuilder()
                .append(URL)
                .append(search)
                .append(OPTIONS);
        JSONObject jsonResult = this.getRequest(urlBuilder.toString()).getObject();
        return buildPackageList((JSONArray) jsonResult.getJSONObject("response").get("docs"));
    }

    /**
     * Get the results of the maven repository search with a size limit
     * @param search Name of the packet to search
     * @param limit Max return items
     * @return The list of the Maven package detected
     * @throws UnirestException
     */
    @Override
    public List<MavenPackage> getResults(String search, Integer limit) throws UnirestException {
        List<MavenPackage> packages = getMavenPackages(search);

        if(packages == null || packages.isEmpty()) return null;
        if(packages.size() < limit) limit = packages.size();

        return packages.subList(0, limit);
    }

    /*@Override
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
    }*/

}
