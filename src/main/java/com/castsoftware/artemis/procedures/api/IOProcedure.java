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

import com.castsoftware.artemis.controllers.api.IOController;
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import com.castsoftware.artemis.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.stream.Stream;

public class IOProcedure {

  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  @Procedure(value = "artemis.api.export.findings", mode = Mode.WRITE)
  @Description(
      "artemis.api.export.findings(Optional String path) - Export all the elements detected as Frameworks. If no path is specified, the results will be exported in the default export folder")
  public Stream<OutputMessage> exportFindings(@Name(value = "Path", defaultValue = "") String path)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      return IOController.exportFrameworks(nal, path);
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.export.all", mode = Mode.WRITE)
  @Description(
      "artemis.api.export.all(Optional String path) - Export all the ArtemisFrameworks ( Framework and Not Frameworks). If no path is specified, the results will be exported in the default export folder ")
  public Stream<OutputMessage> exportAll(@Name(value = "Path", defaultValue = "") String path)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      return IOController.exportAllFrameworks(nal, path);
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.import.data", mode = Mode.WRITE)
  @Description("artemis.api.import.data(String path) - Import the data coming from a zip file.")
  public Stream<OutputMessage> importData(@Name(value = "Path") String path)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      return IOController.importNodes(nal, path);
    } catch (Exception | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }
}
