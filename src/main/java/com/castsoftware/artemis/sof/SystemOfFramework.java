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

package com.castsoftware.artemis.sof;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.nlp.SupportedLanguage;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import java.util.List;

public class SystemOfFramework {

    private Neo4jAL neo4jAL;
    private SupportedLanguage language;
    private String application;
    private List<FrameworkNode> frameworkNodeList;

    /**
     * Create an
     * @param targetApplication Name of the Targeted application
     * @return The nod
     * @throws Neo4jQueryException
     */
    public Node createSofObject(String targetApplication) throws Neo4jQueryException {
        Label levelLabel = Label.label("Level5");
        Label applicationLabel = Label.label(application);

        Node node = neo4jAL.createNode(levelLabel);
        node.addLabel(applicationLabel);

        node.setProperty("Color", "rgb(233,66,53)");
        node.setProperty("Concept", true);
        node.setProperty("Count", 0L);
        node.setProperty("FullName", "Services##Logic Services##Business Logic##Adobe##"+targetApplication);
        node.setProperty("Color", "rgb(233,66,53)");

        return node;
    }

    public void run() {
        String request = String.format("");

    }

    public SystemOfFramework(Neo4jAL neo4jAL, SupportedLanguage language, String application, List<FrameworkNode> frameworkNodeList) {
        this.neo4jAL = neo4jAL;
        this.language = language;
        this.application = application;
        this.frameworkNodeList = frameworkNodeList;
    }
}
