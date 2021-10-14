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

package com.castsoftware.artemis.modules.repositories.maven;

import com.castsoftware.artemis.modules.repositories.Crawler;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Maven extends Crawler {

  private static final String URL = "https://search.maven.org/solrsearch/select?q=";
  private static final String OPTIONS = "&rows=5&wt=json";

  /**
   * Get the results of the maven repository search with a size limit
   *
   * @param search Name of the packet to search
   * @param limit Max return items
   * @return The list of the Maven package detected
   * @throws UnirestException
   */
  @Override
  public List<MavenPackage> getResults(String search, Integer limit) throws UnirestException {
    List<MavenPackage> packages = getMavenPackages(search);

    if (packages.isEmpty()) return packages;
    if (packages.size() < limit) limit = packages.size();

    return packages.subList(0, limit);
  }

  /**
   * Request the Maven repository and get a list of packets matching the search
   *
   * @param search Name of the package to search
   * @return The list of best matching packets
   * @throws UnirestException
   */
  public List<MavenPackage> getMavenPackages(String search) throws UnirestException {
    StringBuilder urlBuilder = new StringBuilder().append(URL).append(search).append(OPTIONS);
    JSONObject jsonResult = this.getRequest(urlBuilder.toString()).getObject();
    return buildPackageList((JSONArray) jsonResult.getJSONObject("response").get("docs"));
  }

  /**
   * Build a list of maven package from the JSON results
   *
   * @param jsonObject Json object containing the maven package
   * @return
   */
  private List<MavenPackage> buildPackageList(JSONArray jsonObject) {
    List<MavenPackage> returnList = new ArrayList<>();

    Iterator<Object> it = jsonObject.iterator();
    while (it.hasNext()) {
      Object o = it.next();
      // Ignore if the object is not a JSON Object
      if (!(o instanceof JSONObject)) {
        continue;
      }
      JSONObject jo = (JSONObject) o;
      String g = jo.getString("g");
      String a = jo.getString("a");
      String id = jo.getString("id");
      String v = jo.getString("latestVersion");
      MavenPackage jp = new MavenPackage(g, a, id, v);
      returnList.add(jp);
    }

    /** TODO Analyze result full name and return best candidate */
    return returnList;
  }
}
