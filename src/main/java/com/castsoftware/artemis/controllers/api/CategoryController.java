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

package com.castsoftware.artemis.controllers.api;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.CategoryNode;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.util.List;
import java.util.Map;

public class CategoryController {

  /**
   * Get the default name category
   *
   * @return
   */
  public static String getDefaultName() {
    return Configuration.getBestOfAllWorlds("artemis.default_category");
  }

  /**
   * Create a Regex Node
   *
   * @see CategoryNode#createNode(Neo4jAL, String, String)
   */
  public static CategoryNode createNode(Neo4jAL neo4jAL, String name, String iconUrl) {
    return CategoryNode.createNode(neo4jAL, name, iconUrl);
  }

  /**
   * Get a Node by its name, or create it if it doesn't exist
   *
   * @param neo4jAL Neo4j Access Layer
   * @param name Name of the category
   * @return
   * @throws Neo4jQueryException
   * @throws Neo4jBadNodeFormatException
   */
  public static CategoryNode getOrCreateByName(Neo4jAL neo4jAL, String name)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {
    String req =
        String.format(
            "MATCH(o:%s) WHERE o.%s=$name RETURN o as cat",
            CategoryNode.getLABEL(), CategoryNode.getNameProperty());
    Map<String, Object> params = Map.of("name", name);

    Result res = neo4jAL.executeQuery(req, params);
    if (res.hasNext()) {
      return new CategoryNode((Node) res.next().get("cat"));
    } else {
      return CategoryNode.createNode(neo4jAL, name, "");
    }
  }

  /**
   * Get a specific Category Node
   *
   * @see CategoryNode#getNode() (Neo4jAL, Long)
   */
  public static CategoryNode getNodeById(Neo4jAL neo4jAL, Long idNode)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {
    return CategoryNode.getNode(neo4jAL, idNode);
  }

  /**
   * Get all Category nodes
   *
   * @see CategoryNode#getAllNodes(Neo4jAL)
   */
  public static List<CategoryNode> getAllNodes(Neo4jAL neo4jAL) throws Neo4jQueryException {
    return CategoryNode.getAllNodes(neo4jAL);
  }

  /**
   * Remove a regex node by its id
   *
   * @see CategoryNode#removeNode(Neo4jAL, Long)
   */
  public static boolean removeNodeById(Neo4jAL neo4jAL, Long idNode) throws Neo4jQueryException {
    return CategoryNode.removeNode(neo4jAL, idNode);
  }

  /**
   * Update node by its ID
   *
   * @param neo4jAL Neo4j Access Layer
   * @param idNode Id of the node
   * @param name New name
   * @param iconUrl New Icon Url
   * @return
   * @throws Neo4jQueryException
   * @throws Neo4jBadNodeFormatException
   */
  public static CategoryNode updateById(Neo4jAL neo4jAL, Long idNode, String name, String iconUrl)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {
    CategoryNode cn = getNodeById(neo4jAL, idNode);
    if (cn == null || cn.getNode() == null) return null;

    CategoryNode newNode = createNode(neo4jAL, name, iconUrl);
    CategoryNode.removeNode(neo4jAL, cn.getId());

    return newNode;
  }
}
