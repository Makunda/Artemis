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

import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.pythia.PythiaCom;

import java.util.List;

public class PythiaComController {

    /**
     * Get the status of the connection to the Oracle
     * @param neo4jAL Neo4j Access Layer
     * @return
     */
    public static boolean isOracleConnected(Neo4jAL neo4jAL) {
        return PythiaCom.getInstance(neo4jAL.getLogger()).isConnected();
    }

    /**
     * Get the date of the last update
     * @param neo4jAL Neo4j Access Layer
     */
    public static String getLastUpdate(Neo4jAL neo4jAL) {
        return PythiaCom.getInstance(neo4jAL.getLogger()).getLastUpdate();
    }

    /**
     * Pull the list of new frameworks
     * @param neo4jAL Neo4j Access Layer
     * @return The list of the frameworks pulled
     */
    public static List<FrameworkNode> pullFrameworks(Neo4jAL neo4jAL) {
        return PythiaCom.getInstance(neo4jAL.getLogger()).pullFrameworks();
    }
}
