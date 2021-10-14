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

package com.castsoftware.artemis.modules.sof.utils;

import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SofUtilities {

  /**
   * Get the presence of a string in the fullname of the objects in other applications
   *
   * @param neo4jAL Neo4j Access Layer
   * @param application Name of the current application (will be excluded of the results)
   * @param toSearchFullName Full name to search ( can be a part of the Fullname)
   * @return
   * @throws Neo4jQueryException
   */
  public static Set<String> getPresenceInOtherApplications(
      Neo4jAL neo4jAL, String application, String toSearchFullName) throws Neo4jQueryException {
    String req =
        String.format(
            "MATCH (o:Object) WHERE o.FullName CONTAINS $toSearchFullName AND NOT $nameApp IN LABELS(o) RETURN DISTINCT [x in LABELS(o) WHERE x<>'Object'][0] as application");
    Map<String, Object> params =
        Map.of("toSearchFullName", toSearchFullName, "nameApp", application);

    Result res = neo4jAL.executeQuery(req, params);
    Set<String> applications = new HashSet<>();

    while (res.hasNext()) {
      applications.add((String) res.next().get("application"));
    }

    return applications;
  }

  /**
   * Create a Sof Object
   *
   * @param neo4jAL Neo4j Access Layer
   * @param sourceApplication Name of the source application
   * @param targetApplication Name of the Targeted application
   * @return The nod
   * @throws Neo4jQueryException
   */
  public static Node createSofObject(
      Neo4jAL neo4jAL, String sourceApplication, String targetApplication)
      throws Neo4jQueryException, Neo4jBadRequestException {
    String fullName = "Services##Logic Services##Business Logic##Artemis##" + targetApplication;

    String req =
        String.format(
            "MERGE (o:Level5:`%s` { Name:$name, Concept:true }) "
                + "SET o.Color='rgb(233,66,53)' "
                + "SET o.Count=0 "
                + "SET o.FullName=$fullName "
                + "RETURN o as sof",
            sourceApplication);
    Map<String, Object> params = Map.of("name", targetApplication, "fullName", fullName);
    Result res = neo4jAL.executeQuery(req, params);

    if (!res.hasNext()) {
      throw new Neo4jBadRequestException(
          "The request '%s' failed to create a SOF Object", "SOFUxCREAS1");
    }

    return (Node) res.next().get("sof");
  }

  /**
   * Create a link from concept to a level in an application
   *
   * @param neo4jAL Neo4j access Layer
   * @param application Name of the application
   * @param idSofObject Id of the sof object
   * @param levelName Name of the level
   * @return
   * @throws Neo4jQueryException
   */
  public static Relationship fromLevelConceptRel(
      Neo4jAL neo4jAL, String application, Long idSofObject, String levelName)
      throws Neo4jQueryException {
    String req =
        String.format(
            "MATCH (o:Level5:`%1$s`) WHERE o.Name=$levelName WITH o as lev "
                + " MATCH (o:Level5:`%1$s`) WHERE ID(o)=$idSofObject MERGE (lev)-[r:References { Concept: true }]->(o) RETURN r as rel",
            application);
    Map<String, Object> params = Map.of("idSofObject", idSofObject, "levelName", levelName);

    Result res = neo4jAL.executeQuery(req, params);
    if (!res.hasNext()) {
      return null;
    }

    return (Relationship) res.next().get("rel");
  }
}
