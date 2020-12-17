package com.castsoftware.artemis.interactions.famililes;

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
        for(Node n : nodeList) {
            UtilsController.applyDemeterTag(neo4jAL, n, commonPrefix);
        }
    }

    public void addDemeterAndParentTag(Neo4jAL neo4jAL) throws Neo4jQueryException {
        for(Node n : nodeList) {
            UtilsController.applyDemeterParentTag(neo4jAL, n, commonPrefix);
        }
    }

    public FamilyGroup(String commonPrefix, List<Node> nodeList) {
        this.commonPrefix = commonPrefix;
        this.nodeList = nodeList;
    }
}
