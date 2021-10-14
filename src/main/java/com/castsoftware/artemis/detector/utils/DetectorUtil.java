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

package com.castsoftware.artemis.detector.utils;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.detector.DetectionCategory;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import org.neo4j.graphdb.Node;

import java.util.Map;

/** Utils function for the detector */
public class DetectorUtil {

  /**
   * Apply a the Artemis detection property on a node
   *
   * @param n Node to process
   * @param detectedAs Category of the detection
   */
  public static void applyNodeProperty(Node n, DetectionCategory detectedAs) {
    String artemisProperty = Configuration.get("artemis.node.detection");
    n.setProperty(artemisProperty, detectedAs.toString());
  }

  /**
   * Apply a category to the node
   *
   * @param n Node to process
   * @param category Category to apply
   */
  public static void applyCategory(Node n, String category) throws Neo4jQueryException {
    String artemisProperty = Configuration.get("artemis.node.category");
    n.setProperty(artemisProperty, category);
  }

  /**
   * Apply a description to the node
   *
   * @param n Node to process
   * @param description Description on the node
   */
  public static void applyDescriptionProperty(Neo4jAL neo4jAL, Node n, String description)
      throws Neo4jQueryException {
    String propertyName = Configuration.get("artemis.sub_node.description.property");
    String req =
        "MERGE (o:ObjectProperty { Description : $DescName }) WITH o as subProperty "
            + "MATCH (n) WHERE ID(n)=$IdNode MERGE (subProperty)<-[r:Property]-(n) SET r.value=$DescValue";
    Map<String, Object> params =
        Map.of("DescName", propertyName, "IdNode", n.getId(), "DescValue", description);

    neo4jAL.executeQuery(req, params);
  }
}
