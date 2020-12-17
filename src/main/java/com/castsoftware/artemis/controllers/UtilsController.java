package com.castsoftware.artemis.controllers;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Node;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class UtilsController {

    public static final String DEMETER_PREFIX = Configuration.get("artemis.tag.demeter_prefix");

    /**
     * Change the Artemis default workspace
     * @param directoryPath The new Artemis workspace
     * @return
     */
    public static String changeArtemisDirectory(String directoryPath) throws MissingFileException {
        if (!Files.exists(Path.of(directoryPath))) {
            return String.format("'%s' is not a valid path. Make sure the target folder exists and retry.", directoryPath);
        }

        Configuration.set("artemis.workspace.folder", directoryPath);
        return String.format("Artemis workspace folder was successfully changed to '%s'.", directoryPath);
    }

    /**
     * Apply a demeter tag to the object using its parent level
     * @param neo4jAL Neo4j access layer
     * @param n Node to tag
     * @param groupName Name of the group
     * @throws Neo4jQueryException
     */
    public static void applyDemeterTag(Neo4jAL neo4jAL, Node n, String groupName) throws Neo4jQueryException {
        Long id = n.getId();
        String tagRequest = String.format("MATCH (obj) WHERE ID(obj)=%1$s " +
                "SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN ['%2$s'] ELSE obj.Tags + '%2$s' END", id, DEMETER_PREFIX+groupName);
        neo4jAL.executeQuery(tagRequest);
    }

    /**
     * Apply a demeter tag to the object using its parent level and a suffix provided
     * @param neo4jAL Neo4j access Layer
     * @param n Node to tag
     * @param suffix Suffix to append to the group
     * @throws Neo4jQueryException
     */
    public static void applyDemeterParentTag(Neo4jAL neo4jAL, Node n, String suffix) throws Neo4jQueryException {
        Long id = n.getId();
        String tagRequest = String.format("MATCH (obj)<-[:Aggregates]-(l:Level5) WHERE ID(obj)=%1$s " +
                "WITH obj, '%2$s' + l.Name + '%3$s' as tagName " +
                "SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN [tagName] ELSE obj.Tags + tagName END", id, DEMETER_PREFIX, suffix);
        neo4jAL.executeQuery(tagRequest);
    }

    /**
     * Switch online mode for Artemis detection
     * @param active New state
     * @return The new state of the online mode
     */
    public static String switchOnlineMode(Boolean active) throws MissingFileException {
        Configuration.set("artemis.onlineMode", active.toString());
        return Configuration.get("artemis.onlineMode");
    }


}


