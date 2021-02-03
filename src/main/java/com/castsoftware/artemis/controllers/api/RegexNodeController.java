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

import com.castsoftware.artemis.config.UserConfiguration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.RegexNode;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RegexNodeController {

    /**
     * Create a Regex Node
     * @see RegexNode#createRegexNode(Neo4jAL, String, List, List,  String, String)
     */
    public static RegexNode createRegexNode(Neo4jAL neo4jAL, String name, List<String> regexes, List<String> internalTypes, String framework, String category) {
        return RegexNode.createRegexNode(neo4jAL, name, regexes, internalTypes, framework, category);
    }

    /**
     * Get a specific Regex Node
     * @see RegexNode#getRegexNodeById(Neo4jAL, Long)
     */
    public static RegexNode getRegexNodeById(Neo4jAL neo4jAL, Long idNode) throws Neo4jQueryException, Neo4jBadNodeFormatException {
        return RegexNode.getRegexNodeById(neo4jAL, idNode);
    }

    /**
     * Link a node to a parent
     * @see RegexNode#linkToParent(Neo4jAL, Long, Long)
     */
    public static Relationship linkToParent(Neo4jAL neo4jAL, Long idChild, Long idParent) throws Neo4jQueryException {
        return RegexNode.linkToParent(neo4jAL, idChild, idParent);
    }

    /**
     * Get all Regex nodes
     * @see RegexNode#getAllNodes(Neo4jAL)
     */
    public static List<RegexNode> getAllRegexNode(Neo4jAL neo4jAL) throws Neo4jQueryException {
        return RegexNode.getAllNodes(neo4jAL);
    }

    /**
     * Remove a regex node by its id
     * @see RegexNode#removeRegexNode(Neo4jAL, Long)
     */
    public static boolean removeRegexNodeById(Neo4jAL neo4jAL, Long idNode) throws Neo4jQueryException {
        return RegexNode.removeRegexNode(neo4jAL, idNode);
    }

    /**
     * Flag objects matching a regex node
     * @param neo4jAL Neo4j Access Layer
     * @param rn Regex Node
     * @return
     * @throws Neo4jQueryException
     */
    private static Long flagObjects(Neo4jAL neo4jAL, RegexNode rn) throws Neo4jQueryException {
        String demeterPrefix = UserConfiguration.get("demeter.prefix.group_level");
        String tagName = demeterPrefix+rn.getCategory();
        String req = String.format("MATCH (o:Object) WHERE any( x IN $listRegex WHERE o.FullName~=x ) AND o.InternalType in $listInternalType " +
                "SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN ['%1$s'] ELSE obj.Tags + '%1$s' " +
                "END RETURN COUNT(DISTINCT o) as count", tagName);

        Map<String, Object> params = Map.of("listRegex", rn.getChildren(), "listInternalType", rn.getInternalTypes());
        Result res = neo4jAL.executeQuery(req, params);
        if(!res.hasNext()) return 0L;
        return (Long) res.next().get("count");
    }

    /**
     * Flag all the nodes from the regex
     * @param neo4jAL Neo4j Access Layer
     * @return
     * @throws Neo4jQueryException
     */
    public static Long flagRegexNodes(Neo4jAL neo4jAL) throws Neo4jQueryException {
        List<RegexNode> toVisit = RegexNode.getRootRegexNodes(neo4jAL);

        Iterator<RegexNode> itr = toVisit.iterator();
        Long regexMatched = 0L;

        while (itr.hasNext()) {
            RegexNode r = itr.next();
            toVisit.addAll(r.getChildren()); // Add the children

            // Get regex and find node matching them
            regexMatched += flagObjects(neo4jAL, r);
            itr.remove(); // Remove the node
        }

        return regexMatched;
    }


}
