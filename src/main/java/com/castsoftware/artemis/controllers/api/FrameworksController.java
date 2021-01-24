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
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;

public class FrameworksController {

    /**
     * Find a framework using its name
     * @param neo4jAL Neo4j Access Layer
     * @param name Name of the Framework to find
     * @return
     * @throws Neo4jQueryException
     * @throws Neo4jBadNodeFormatException
     */
    public static FrameworkNode findFrameworkByName(
        Neo4jAL neo4jAL,
        String name) throws Neo4jQueryException, Neo4jBadNodeFormatException {

        return FrameworkNode.findFrameworkByName(neo4jAL, name);
    }

    /**
     * Find a framework using its name and its internal type
     * @param neo4jAL Neo4j Access Layer
     * @param name Name of the Framework to find
     * @param internalType  Internal type of the object
     * @return
     * @throws Neo4jQueryException
     * @throws Neo4jBadNodeFormatException
     */
    public static FrameworkNode findFrameworkByNameAndType(
            Neo4jAL neo4jAL,
            String name, String internalType) throws Neo4jQueryException, Neo4jBadNodeFormatException {

        return FrameworkNode.findFrameworkByNameAndType(neo4jAL, name, internalType);
    }

    /**
     * Create a Framework node in the Database
     * @param neo4jAL Neo4j Access Layer
     * @param name Name of the Framework
     * @param discoveryDate Date of the discovery
     * @param location Location of the Framework ( repository, url, etc...)
     * @param description Description
     * @param type Framework category ( Framework, NotFramework, etc..)
     * @param category Category of the framework
     * @param internalType Internal type of the object detected
     * @return The node created
     * @throws Neo4jQueryException
     */
  public static FrameworkNode addFramework(
      Neo4jAL neo4jAL,
      String name,
      String discoveryDate,
      String location,
      String description,
      String type,
      String category,
      String internalType) throws Neo4jQueryException {

      FrameworkNode fn = new FrameworkNode(neo4jAL, name, discoveryDate, location, description, 0l, .0);
      fn.setInternalType(internalType);
      fn.setCategory(category);
      fn.setFrameworkType(FrameworkType.getType(type));
      fn.createNode();

      neo4jAL.logInfo(String.format("Framework with name %s has been inserted through API call", name));

      return fn;
  }

    /**
     * Update a Framework node in the Database
     * @param neo4jAL Neo4j Access Layer
     * @param name Name of the Framework
     * @param discoveryDate Date of the discovery
     * @param location Location of the Framework ( repository, url, etc...)
     * @param description Description
     * @param type Framework category ( Framework, NotFramework, etc..)
     * @param category Category of the framework
     * @param internalType Internal type of the object detected
     * @param numberOfDetection Number of detection
     * @param percentageOfDetection Detection rate
     * @return The new node
     * @throws Neo4jQueryException
     * @throws Neo4jBadNodeFormatException
     */
  public static FrameworkNode updateFramework(
          Neo4jAL neo4jAL,
          String name,
          String discoveryDate,
          String location,
          String description,
          String type,
          String category,
          Long numberOfDetection,
          Double percentageOfDetection,
          String internalType) throws Neo4jQueryException, Neo4jBadNodeFormatException {

      FrameworkNode fn = new FrameworkNode(neo4jAL, name, discoveryDate, location, description, numberOfDetection, percentageOfDetection);
      fn.setInternalType(internalType);
      fn.setCategory(category);
      fn.setFrameworkType(FrameworkType.getType(type));

      return FrameworkNode.updateFrameworkByName(neo4jAL, name, fn);
  }
}
