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

import com.castsoftware.artemis.controllers.api.CategoryController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.CategoryNode;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.pythia.PythiaCom;

import java.util.Collections;
import java.util.Date;

public class OracleController {

  /**
   * Return the status of the Oracle Communication
   *
   * @return True if working, false otherwise
   */
  public static Boolean getOracleComStatus(Neo4jAL neo4jAL) {
    return PythiaCom.getInstance(neo4jAL).getStatus();
  }

  /**
   * Return the status of the Oracle Communication
   *
   * @return True if working, false otherwise
   */
  public static FrameworkNode testOracleFindings(
      Neo4jAL neo4jAL, String frameworkName, String InternalType) {
    return PythiaCom.getInstance(neo4jAL).findFramework(neo4jAL, frameworkName, InternalType);
  }

  /**
   * Return the status of the Oracle Communication
   *
   * @return True if working, false otherwise
   */
  public static boolean testOracleAddFramework(Neo4jAL neo4jAL)
      throws Neo4jQueryException, Neo4jBadNodeFormatException {
    // Dummy Framework node
    FrameworkNode fn =
        new FrameworkNode(
            neo4jAL,
            "Test Upload",
            "Pattern",
            false,
            "Now",
            "Test Location",
            "My description",
            0L,
            .0,
            new Date().getTime());
    CategoryNode cn = CategoryController.getOrCreateByName(neo4jAL, "Test Category");
    fn.setCategory(cn);
    fn.setInternalTypes(Collections.singletonList("Internal Type"));
    fn.setFrameworkType(FrameworkType.NOT_KNOWN);

    return PythiaCom.getInstance(neo4jAL).sendFramework(fn);
  }
}
