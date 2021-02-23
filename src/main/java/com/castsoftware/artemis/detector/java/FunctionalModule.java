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

package com.castsoftware.artemis.detector.java;

import com.castsoftware.artemis.config.detection.LanguageProp;
import com.castsoftware.artemis.controllers.UtilsController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Module to represent the different connection with other identified packages */
public class FunctionalModule {

  private final String application;
  private final String identifier;
  private final String delimiter;
  private final List<String> internalTypes;
  private final Integer depth;
  private Neo4jAL neo4jAL;
  private List<String> toOtherModules;
  private List<String> fromOtherModules;

  public FunctionalModule(
      Neo4jAL neo4jAL,
      String application,
      String identifier,
      LanguageProp languageProp,
      Integer depth)
      throws Neo4jQueryException {
    this.neo4jAL = neo4jAL;

    this.identifier = identifier;
    this.application = application;
    this.delimiter = languageProp.getPackageDelimiter();
    this.internalTypes = languageProp.getObjectsInternalType();
    this.depth = depth;

    this.toOtherModules = new ArrayList<>();
    this.fromOtherModules = new ArrayList<>();

    init();
  }

  private void init() throws Neo4jQueryException {
    Map<String, Object> params =
        Map.of(
            "identifier",
            identifier + delimiter,
            "delimiter",
            delimiter,
            "depth",
            depth,
            "languageTypes",
            internalTypes);

    String reqTo =
        String.format(
            "MATCH (o:Object:`%1$s`)-[]->(e:Object:`%1$s`) WHERE o.FullName CONTAINS $identifier "
                + "AND NOT e.FullName CONTAINS $identifier "
                + "AND o.InternalType IN $languageTypes AND e.InternalType IN $languageTypes "
                + "AND o.External=false AND e.External=false "
                + "RETURN DISTINCT apoc.text.join(SPLIT(e.FullName, $delimiter)[0..$depth], $delimiter) as package;",
            application);

    String reqFrom =
        String.format(
            "MATCH (o:Object:`%1$s`)<-[]-(e:Object:`%1$s`) WHERE o.FullName CONTAINS $identifier "
                + "AND NOT e.FullName CONTAINS $identifier "
                + "AND o.InternalType IN $languageTypes AND e.InternalType IN $languageTypes "
                + "AND o.External=false AND e.External=false "
                + "RETURN DISTINCT apoc.text.join(SPLIT(e.FullName, $delimiter)[0..$depth], $delimiter) as package;",
            application);

    // Relations to modules
    Result resTo = neo4jAL.executeQuery(reqTo, params);
    while (resTo.hasNext()) {
      toOtherModules.add((String) resTo.next().get("package"));
    }

    // Relations from modules
    Result resFrom = neo4jAL.executeQuery(reqFrom, params);
    while (resFrom.hasNext()) {
      fromOtherModules.add((String) resFrom.next().get("package"));
    }

    if (isOnlyUsed()) {
      applyLevelTag();
    }
  }

  public boolean isOnlyUsed() {
    return toOtherModules.isEmpty() && !fromOtherModules.isEmpty();
  }

  public void applyLevelTag() throws Neo4jQueryException {
    UtilsController.applyLevelTagRegexFullName(neo4jAL, identifier + delimiter, identifier);
  }

  public String getIdentifier() {
    return identifier;
  }

  public List<String> getToOtherModules() {
    return toOtherModules;
  }

  public List<String> getFromOtherModules() {
    return fromOtherModules;
  }

  public void applyArchitectureTag(String viewName) throws Neo4jQueryException {
    UtilsController.applyArchitectureTagRegexFullName(
        neo4jAL, identifier + delimiter, viewName, identifier);
  }

  @Override
  public String toString() {
    return "FunctionalModule{"
        + "identifier='"
        + identifier
        + '\''
        + ", depth="
        + depth
        + ", toOtherModules="
        + toOtherModules
        + ", fromOtherModules="
        + fromOtherModules
        + '}';
  }
}
