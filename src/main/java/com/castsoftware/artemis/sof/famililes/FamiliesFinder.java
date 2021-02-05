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
import com.castsoftware.artemis.database.Neo4jAL;
import org.neo4j.graphdb.Node;

import java.util.*;
import java.util.stream.Collectors;

public class FamiliesFinder {

    private static final Integer PREFIX_LENGTH = 3;
    private static final Integer PREFIX_MAX_LENGTH = 3;
    private static final Integer MIN_FAMILY_SIZE = 3;
    private static final String IMAGING_OBJECT_NAME = Configuration.get("imaging.node.object.name");

    private Neo4jAL neo4jAL;
    private List<Node> candidates;
    private FamilyTree familyTree;


//    public List<FamilyGroup> findFamilies() {
//        Map<String, List<Node>> families  = new HashMap<>();
//
//        Node n;
//        String nodeName;
//
//        // Iterate through candidate list
//        Iterator<Node> itNode = candidates.listIterator();
//        while (itNode.hasNext()) {
//            n = itNode.next();
//
//            // Ignore node without name
//            if(!n.hasProperty(IMAGING_OBJECT_NAME)) continue;
//            nodeName = (String) n.getProperty(IMAGING_OBJECT_NAME);
//
//            // Ignore not relevant names
//            if(nodeName.length() <= PREFIX_LENGTH) continue;
//            String prefix = nodeName.substring(0, PREFIX_LENGTH);
//
//            // Check if it's a known prefix
//            List<Node> list = families.getOrDefault(prefix, new ArrayList<>());
//            list.add(n);
//            families.put(prefix, list);
//
//        }
//
//        // Return families with a minimum number of candidates
//        List<FamilyGroup> fg = new ArrayList<>();
//        for(Map.Entry<String, List<Node>> en : families.entrySet()) {
//            // Ignore small groups
//            if(en.getValue().size() < MIN_FAMILY_SIZE) continue;
//
//            fg.add(new FamilyGroup(en.getKey(), en.getValue()));
//        }
//
//        return fg;
//    }

    /**
     * Get the family in the framework detection
     * @return
     */
    public List<FamilyGroup> findFamilies() {
        List<FamilyGroup> returnList = this.familyTree.getEndLeaves().stream()
                .map(x -> new FamilyGroup(x.name, x.items))
                .collect(Collectors.toList());

        for(FamilyGroup fg : returnList){
            neo4jAL.logInfo("Common prefix found : "+fg.getCommonPrefix() + ". Number of candidates: "+fg.getFamilySize());
        }

        return returnList;
    }

    public FamiliesFinder(Neo4jAL neo4jAL, List<Node> candidates) {
        this.neo4jAL = neo4jAL;
        this.candidates = candidates;

        this.familyTree = new FamilyTree(neo4jAL.getLogger(), candidates);
        this.familyTree.buildTree(8);
    }
}
