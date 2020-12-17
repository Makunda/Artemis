package com.castsoftware.artemis.interactions;

import com.castsoftware.artemis.database.Neo4jAL;
import org.neo4j.graphdb.Node;

import java.util.List;

public class InteractionDetector {

    private Neo4jAL neo4jAL;
    private List<String> encounteredFull;

    public void findGroups() {

    }

    public InteractionDetector(Neo4jAL neo4jAL) {
        this.neo4jAL = neo4jAL;
    }
}
