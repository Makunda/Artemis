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

package com.castsoftware.artemis.io;

import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Result;

import java.util.List;

public class Cleaner {

  private static final List<String> LABEL_LIST = List.of(FrameworkNode.getLabel());

  /**
   * Remove Artemis nodes in the database
   *
   * @param neo4jAL Neo4j Access Layer
   * @return
   * @throws Neo4jQueryException
   */
  public static Long cleanNodes(Neo4jAL neo4jAL) throws Neo4jQueryException {
    Long totalDeleted = 0L;

    String templateReq = "MATCH (o:%s) DETACH DELETE o RETURN COUNT(o) as deleted";
    String req;
    Result res;
    for (String label : LABEL_LIST) {
      req = String.format(templateReq, label);
      res = neo4jAL.executeQuery(req);

      if (!res.hasNext()) continue;
      totalDeleted += (Long) res.next().get("deleted");
    }

    return totalDeleted;
  }

  /**
   * Remove Artemis nodes in the database
   *
   * @param neo4jAL Neo4j Access Layer
   * @return
   * @throws Neo4jQueryException TODO : complete the function
   */
  public static Long cleanFrameworks(Neo4jAL neo4jAL) throws Neo4jQueryException {
    Long totalDeleted = 0L;

    String req = String.format("MATCH (o:%s) WHERE o.Name=", FrameworkNode.getLabel());

    return totalDeleted;
  }
}
