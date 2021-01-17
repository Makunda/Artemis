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
    public abstract List<? extends SPackage> getResults(String search, Integer limit) throws UnirestException;

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
