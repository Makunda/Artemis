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

package com.castsoftware.artemis.procedures.api;

import com.castsoftware.artemis.controllers.api.FrameworksController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.results.FrameworkResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.stream.Stream;

public class FrameworksApiProcedure {
  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  @Procedure(value = "artemis.api.add.framework", mode = Mode.WRITE)
  @Description(
      "artemis.api.add.framework(String Name, String DiscoveryDate, String Location, String Description, String Type, String Category, String InternalType) - Add a framework")
  public Stream<FrameworkResult> addFramework(
      @Name(value = "Name") String name,
      @Name(value = "DiscoveryDate", defaultValue = "") String discoveryDate,
      @Name(value = "Location", defaultValue = "") String location,
      @Name(value = "Description", defaultValue = "") String description,
      @Name(value = "Type", defaultValue = "") String type,
      @Name(value = "Category", defaultValue = "") String category,
      @Name(value = "InternalType", defaultValue = "") String internalType)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      FrameworkNode addedFramework =
          FrameworksController.addFramework(
              nal, name, discoveryDate, location, description, type, category, internalType);

      return Stream.of(new FrameworkResult(addedFramework));
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.update.framework", mode = Mode.WRITE)
  @Description(
      "artemis.api.update.framework(String Name, String DiscoveryDate, String Location, String Description, String Type, String Category, String InternalType, Long NumberOfDetection, Double PercentageOfDetection ) - Update a framework using its name")
  public Stream<FrameworkResult> updateFramework(
      @Name(value = "Name") String name,
      @Name(value = "DiscoveryDate") String discoveryDate,
      @Name(value = "Location") String location,
      @Name(value = "Description") String description,
      @Name(value = "Type") String type,
      @Name(value = "Category") String category,
      @Name(value = "InternalType") String internalType,
      @Name(value = "NumberOfDetection", defaultValue = "0") Long numberOfDetection,
      @Name(value = "PercentageOfDetection", defaultValue = "0") Double percentageOfDetection)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      FrameworkNode addedFramework =
          FrameworksController.updateFramework(
              nal,
              name,
              discoveryDate,
              location,
              description,
              type,
              category,
              numberOfDetection,
              percentageOfDetection,
              internalType);
      return Stream.of(new FrameworkResult(addedFramework));
    } catch (Exception
        | Neo4jConnectionError
        | Neo4jQueryException
        | Neo4jBadNodeFormatException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.find.framework", mode = Mode.WRITE)
  @Description(
          "artemis.api.find.framework(String Name) - Find a framework using its name")
  public Stream<FrameworkResult> updateFramework(
          @Name(value = "Name") String name) throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      FrameworkNode fn =
              FrameworksController.findFrameworkByName(
                      nal,
                      name);

      if(fn != null) {
        return Stream.of(new FrameworkResult(fn));
      } else {
        return null;
      }

    } catch (Exception
            | Neo4jConnectionError
            | Neo4jQueryException
            | Neo4jBadNodeFormatException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

}
