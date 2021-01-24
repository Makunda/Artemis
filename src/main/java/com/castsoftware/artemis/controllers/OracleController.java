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

package com.castsoftware.artemis.controllers;

import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.oracle.OracleCom;

public class OracleController {

    /**
     * Return the status of the Oracle Communication
     * @return True if working, false otherwise
     */
    public static Boolean getOracleComStatus(Neo4jAL neo4jAL) {
        return OracleCom.getInstance(neo4jAL.getLogger()).getStatus();
    }

    /**
     * Return the status of the Oracle Communication
     * @return True if working, false otherwise
     */
    public static FrameworkNode testOracleFindings(Neo4jAL neo4jAL, String frameworkName, String InternalType) {
        return OracleCom.getInstance(neo4jAL.getLogger()).findFramework(neo4jAL, frameworkName, InternalType);
    }

    /**
     * Return the status of the Oracle Communication
     * @return True if working, false otherwise
     */
    public static boolean testOracleAddFramework(Neo4jAL neo4jAL) {
        // Dummy Framework node
        FrameworkNode fn =
                new FrameworkNode(
                        neo4jAL,
                        "Test Upload",
                        "Now",
                        "Test Location",
                        "My description",
                        0L,
                        .0);
        fn.setCategory("Test Category");
        fn.setInternalType("Internal Type");
        fn.setFrameworkType(FrameworkType.NOT_KNOWN);


        return OracleCom.getInstance(neo4jAL.getLogger()).addFramework(fn);
    }

}