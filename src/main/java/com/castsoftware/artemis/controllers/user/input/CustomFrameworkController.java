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

package com.castsoftware.artemis.controllers.user.input;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.config.UserConfiguration;
import com.castsoftware.artemis.controllers.api.CategoryController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.CategoryNode;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class CustomFrameworkController {

    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private static Boolean alreadyWarned = false;

    /**
     * Create frameworks from the selected nodes
     * @param neo4jAL Neo4j Access Layer
     * @return The list of detected frameworks
     * @throws Neo4jQueryException
     */
    public static List<FrameworkNode> getUserCustomFrameworks(Neo4jAL neo4jAL) throws Neo4jQueryException {
        String artemisFrameworkTag = null;
        if( UserConfiguration.isKey("artemis.tag.framework.identifier")) {
            artemisFrameworkTag = UserConfiguration.get("artemis.tag.framework.identifier");
        } else {
            artemisFrameworkTag = Configuration.get("artemis.tag.framework.identifier");
            if(!alreadyWarned) neo4jAL.logInfo(String.format("No artemis.tag.framework.identifier found in the user configuration. Will use the default : '%s' tag", artemisFrameworkTag));
            alreadyWarned = true;
        }


        String req = "MATCH (o:Object) WHERE single( x in o.Tags WHERE x CONTAINS $tagArtemis) " +
                "RETURN o as object, [x in o.Tags WHERE x CONTAINS $tagArtemis ][0] as tag";
        Map<String, Object> params = Map.of("tagArtemis", artemisFrameworkTag);

        neo4jAL.logInfo("DEBUG :: " + req);

        Result res = neo4jAL.executeQuery(req, params);
        List<FrameworkNode> frameworkNodeList = new ArrayList<>();

        Date date = Calendar.getInstance().getTime();
        String strDate = dateFormat.format(date);
        Integer numObjects = 0;

        // Iterate over the results and flag the object as framework
        Node n;
        FrameworkNode fn;
        CategoryNode cn;
        String category = "";
        String tag = "";
        while(res.hasNext()) {
            numObjects++;
            Map<String, Object> obj = res.next();
            n = (Node) obj.get("object");
            tag = (String) obj.get("tag");

            if(!n.hasProperty("Name")) continue;
            String name = (String) n.getProperty("Name");

            if(!n.hasProperty("InternalType")) continue;
            String internalType = (String) n.getProperty("InternalType");

            try {
                // Split tag and get category
                category = tag.replace(artemisFrameworkTag, "");

                fn = new FrameworkNode(neo4jAL, name, strDate, "", "", 1L, 1.0, new Date().getTime());

                cn = CategoryController.getOrCreateByName(neo4jAL, category);
                fn.setCategory(cn);
                fn.setInternalType(internalType);
                fn.setFrameworkType(FrameworkType.FRAMEWORK);

                // Update the framework if not exist in the database
                if(FrameworkNode.findFrameworkByNameAndType(neo4jAL, name, internalType) == null) {
                    fn.createNode();
                }

                frameworkNodeList.add(fn);
            } catch (Exception | Neo4jBadNodeFormatException e) {
                neo4jAL.logError(String.format("Failed to create framework for object with name : %s", name));
            }
        }

        // Clean tags
        String reqTag = "MATCH (o:Object) WHERE single( x in o.Tags WHERE x CONTAINS $tagArtemis) " +
                "SET o.Tags=[x in o.Tags WHERE NOT x CONTAINS $tagArtemis]";
        neo4jAL.executeQuery(reqTag, params);
        neo4jAL.logInfo(String.format("%d frameworks detected during the discovery.", numObjects));

        return frameworkNodeList;
    }

    /**
     * Check if the tag is present in the database
     * @param neo4jAL Neo4j Access Layer
     * @return True if tags are present
     */
    public static boolean isTagPresent(Neo4jAL neo4jAL) throws Neo4jQueryException {
        String artemisFrameworkTag = null;
        if( UserConfiguration.isKey("artemis.tag.framework.identifier")) {
            artemisFrameworkTag = UserConfiguration.get("artemis.tag.framework.identifier");
        }else {
            artemisFrameworkTag = Configuration.get("artemis.tag.framework.identifier");
            if(!alreadyWarned)neo4jAL.logInfo(String.format("No artemis.tag.framework.identifier parameter found in the user configuration. Will use the default one '%s'.", artemisFrameworkTag));
            alreadyWarned=true;
        }

        String req = "MATCH (o:Object) WHERE single( x in o.Tags WHERE x CONTAINS $tagArtemis) " +
                "RETURN o as object LIMIT 1;";
        Map<String, Object> params = Map.of("tagArtemis", artemisFrameworkTag);

        Result res = neo4jAL.executeQuery(req, params);
        return res.hasNext();
    }


    /**
     * Get the custom framework tag
     * @return The tag
     */
    public static String getTag() {
        if(UserConfiguration.isKey("artemis.tag.framework.identifier")) {
            return UserConfiguration.get("artemis.tag.framework.identifier");
        }

        return Configuration.get("artemis.tag.framework.identifier");
    }

    /**
     * Set a new custom framework tag in the user configuration
     * @param newTag New tag
     * @return The new value of the custom framework tag
     * @throws MissingFileException If the user configuration doesn't exist
     */
    public static String setTag(String newTag) throws MissingFileException {
        String tag = UserConfiguration.set("artemis.tag.framework.identifier", newTag);
        UserConfiguration.reload();
        return tag;
    }

}
