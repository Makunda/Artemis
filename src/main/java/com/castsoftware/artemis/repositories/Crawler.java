package com.castsoftware.artemis.repositories;


import com.castsoftware.artemis.exceptions.repositories.MalformedResultException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;


import java.util.List;

public abstract class Crawler {

    /**
     * Get the result of the crawler as a list of objects
     * @param limit Max return items
     * @return List of package
     */
    public abstract List<SPackage> getResults(Integer limit);

    /**
     * Set the query of the crawler. In general the query will consist of only the name od the package you want to retrieve
     * @param query
     * @throws MalformedResultException
     */
    public abstract void setQuery(String query) throws MalformedResultException;

    /**
     * Get the body of the request
     * @param request Url to query
     * @return The body of the request as a JSONode
     * @throws UnirestException If the query failed to execute
     */
    protected JsonNode getRequest(String request) throws UnirestException {
        JsonNode response = Unirest.get(request)
                .header("Content-Type", "application/json")
                .asJson().getBody();
        return response;
    }

}
