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

package com.castsoftware.artemis.sof.utils;

import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
  public static List<String> getPresenceInOtherApplications(
      Neo4jAL neo4jAL, String application, String toSearchFullName) throws Neo4jQueryException {
    String req =
        String.format(
            "MATCH (o:Object) WHERE o.FullName CONTAINS $toSearchFullName AND NOT $nameApp IN LABELS(o) RETURN DISTINCT [x in LABELS(o) WHERE x<>'Object'][0] as application");
    Map<String, Object> params =
        Map.of("toSearchFullName", toSearchFullName, "nameApp", application);

    Result res = neo4jAL.executeQuery(req, params);
    List<String> applications = new ArrayList<>();

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
      throws Neo4jQueryException {
    Label levelLabel = Label.label("Level5");
    Label applicationLabel = Label.label(sourceApplication);

    Node node = neo4jAL.createNode(levelLabel);
    node.addLabel(applicationLabel);

    node.setProperty("Color", "rgb(233,66,53)");
    node.setProperty("Concept", true);
    node.setProperty("Count", 0L);
    node.setProperty(
        "FullName", "Services##Logic Services##Business Logic##Adobe##" + targetApplication);
    node.setProperty("Color", "rgb(233,66,53)");

    return node;
  }
}
