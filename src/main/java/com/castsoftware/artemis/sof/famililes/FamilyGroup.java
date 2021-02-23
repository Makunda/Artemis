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

package com.castsoftware.artemis.sof.famililes;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.controllers.UtilsController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Node;

import java.util.List;

public class FamilyGroup {

  private static final String DEMETER_TAG = Configuration.get("artemis.tag.demeter_prefix");

  private String commonPrefix;
  private List<Node> nodeList;

  public FamilyGroup(String commonPrefix, List<Node> nodeList) {
    this.commonPrefix = commonPrefix;
    this.nodeList = nodeList;
  }

  public String getCommonPrefix() {
    return commonPrefix;
  }

  public List<Node> getNodeList() {
    return nodeList;
  }

  public Integer getFamilySize() {
    return nodeList.size();
  }

  public void addDemeterTag(Neo4jAL neo4jAL) throws Neo4jQueryException {
    for (Node n : nodeList) {
      UtilsController.applyDemeterLevelTag(neo4jAL, n, commonPrefix);
    }
  }

  public void addDemeterAndParentTag(Neo4jAL neo4jAL) throws Neo4jQueryException {
    for (Node n : nodeList) {
      UtilsController.applyDemeterParentTag(neo4jAL, n, commonPrefix);
    }
  }
}
