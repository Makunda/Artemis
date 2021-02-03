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

package com.castsoftware.artemis.config;

import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.utils.Workspace;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.util.Date;
import java.util.Map;

public class NodeConfiguration {

    private static final String NODE_LABEL = "ArtemisConfiguration";
    private static final String LAST_UPDATE_PROP = "LastUpdate";
    private static final String WORKSPACE_PROP = "Workspace";

    private Node node;
    private Long lastUpdate;
    private String workspace;

    public Long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    /**
     * Update the last timestamp value
     * @return
     */
    public Long updateLastUpdate() {
        Long timestamp = new Date().getTime();
        this.node.setProperty(LAST_UPDATE_PROP, timestamp);
        this.lastUpdate = timestamp;
        return timestamp;
    }

    /**
     * Update the value of the workspace
     * @param newWorkspace new workspace
     * @return
     */
    public String updateWorkspace(String newWorkspace) {
        this.node.setProperty(WORKSPACE_PROP, newWorkspace);
        this.workspace = newWorkspace;
        return workspace;
    }

    private NodeConfiguration(Node node) {
        this.node = node;

        String workspace = Workspace.getWorkspacePath().toString();
        Long lastUpdate = 0L;

        if(!node.hasProperty(WORKSPACE_PROP)) {
            node.setProperty(WORKSPACE_PROP, workspace);
        } else {
            workspace = (String) node.getProperty(WORKSPACE_PROP);
        }

        if(!node.hasProperty(LAST_UPDATE_PROP)) {
            node.setProperty(LAST_UPDATE_PROP, 0L);
        } else {
            lastUpdate = (Long) node.getProperty(LAST_UPDATE_PROP);
        }

        this.lastUpdate = lastUpdate;
        this.workspace = workspace;
    }

    /**
     * Create a new Artemis configuration mode
     * @param neo4jAL Neo4j Access Layer
     * @return
     * @throws Neo4jQueryException
     */
    private static NodeConfiguration createConfiguration(Neo4jAL neo4jAL) throws Neo4jQueryException, Neo4jBadRequestException {
        String workspace = Workspace.getWorkspacePath().toString();
        Long lastUpdate = 0L;

        String req = String.format("MERGE (o:%s) SET o.%s=$workspace SET o.%s=$lastUpdate RETURN o as node", NODE_LABEL, WORKSPACE_PROP, LAST_UPDATE_PROP);
        Map<String, Object> params = Map.of("workspace", workspace, "lastUpdate", lastUpdate);

        Result res = neo4jAL.executeQuery(req, params);
        if(!res.hasNext()) throw new Neo4jBadRequestException("Failed to create the artemis configuration.", "NODECxCREAC1");

        Node n = (Node) res.next().get("node");

        return new NodeConfiguration(n);
    }

    /**
     * Delete and recreate a configuration
     * @param neo4jAL
     * @return
     * @throws Neo4jQueryException
     */
    public static NodeConfiguration forceRecreateConfiguration(Neo4jAL neo4jAL) throws Neo4jQueryException, Neo4jBadRequestException {
        String req = String.format("MATCH (o:%s) DETACH DELETE o", NODE_LABEL);
        neo4jAL.executeQuery(req);

        return createConfiguration(neo4jAL);
    }

    /**
     * Get the configuration in the database. If no configuration is detected it will create a new one
     * @param neo4jAL Neo4j Access Layer
     * @return The configuration
     * @throws Neo4jQueryException
     */
    public static NodeConfiguration getConfiguration(Neo4jAL neo4jAL) throws Neo4jQueryException, Neo4jBadRequestException {
        String req = String.format("MATCH (o:%s) RETURN o as node LIMIT 1", NODE_LABEL);
        Result res = neo4jAL.executeQuery(req);

        // Create if not exists
        if(!res.hasNext()) return createConfiguration(neo4jAL);

        // else get the parameters
        Node n = (Node) res.next().get("node");

        return new NodeConfiguration(n);
    }

}