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

package com.castsoftware.artemis.repositories.github;

import com.castsoftware.artemis.repositories.Crawler;
import com.castsoftware.artemis.repositories.SPackage;
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

  @Override
  public List<GithubPackage> getResults(String search, Integer limit) throws UnirestException {
    List<GithubPackage> packages = getGithubPackages(search);

    if (packages == null || packages.isEmpty()) return null;
    if (packages.size() < limit) limit = packages.size();

    return packages.subList(0, limit);
  }

  /**
   * Get the results for this specific instance
   *
   * @return
   */
  public List<GithubPackage> getGithubPackages(String search) throws UnirestException {
    StringBuilder urlBuilder = new StringBuilder().append(URL).append(search).append(OPTIONS);
    JSONObject jsonResult = getRequest(urlBuilder.toString()).getObject();
    return buildPackageList((JSONArray) jsonResult.get("items"));
  }

  /**
   * Build the package list from the result of the query
   *
   * @param jsonObject
   * @return
   */
  private List<GithubPackage> buildPackageList(JSONArray jsonObject) {
    List<GithubPackage> returnList = new ArrayList<>();

    Iterator<Object> it = jsonObject.iterator();
    while (it.hasNext()) {
      Object o = it.next();

      if (!(o instanceof JSONObject)) {
        continue;
      }
      JSONObject jo = (JSONObject) o;
      String n = jo.getString("name");
      String fn = jo.getString("full_name");

      String licenses = "";
      if (jo.get("license") != null) {
        licenses = jo.getJSONObject("license").getString("key");
      }
      String ud = jo.getString("updated_at");
      int sc = jo.getInt("stargazers_count");

      GithubPackage gp = new GithubPackage(n, fn, licenses, ud, sc);
      returnList.add(gp);
    }
    /** TODO Analyze result full name and return best candidate */
    return returnList;
  }
}
