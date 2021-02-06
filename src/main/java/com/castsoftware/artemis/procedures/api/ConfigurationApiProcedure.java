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

import com.castsoftware.artemis.controllers.api.ConfigurationController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.artemis.results.BooleanResult;
import com.castsoftware.artemis.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.stream.Stream;

public class ConfigurationApiProcedure {
  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  // Configuration Pythia
  @Procedure(value = "artemis.api.configuration.get.pythia.uri", mode = Mode.WRITE)
  @Description("artemis.api.configuration.get.pythia.uri() - Get the URI of Pythia")
  public Stream<OutputMessage> getPythiaURL() throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      String uri = ConfigurationController.getURIPythia(nal);
      return Stream.of(new OutputMessage(uri));
    } catch (Exception | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.configuration.set.pythia.uri", mode = Mode.WRITE)
  @Description("artemis.api.configuration.set.pythia.uri(String URI) - Set the URI of Pythia")
  public Stream<OutputMessage> setPythiaURL(@Name(value = "URI") String URI)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      String uri = ConfigurationController.setURIPythia(nal, URI);
      return Stream.of(new OutputMessage(uri));
    } catch (Exception | Neo4jConnectionError | MissingFileException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.configuration.get.pythia.token", mode = Mode.WRITE)
  @Description(
      "artemis.api.configuration.get.pythia.token() - Get the presence of the Pythia token")
  public Stream<BooleanResult> getPythiaToken() throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Boolean present = ConfigurationController.getTokenPythia(nal);
      return Stream.of(new BooleanResult(present));
    } catch (Exception | Neo4jConnectionError | MissingFileException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.configuration.set.pythia.token", mode = Mode.WRITE)
  @Description(
      "artemis.api.configuration.set.pythia.token() - Get the presence of the Pythia token")
  public Stream<BooleanResult> setPythiaToken(@Name(value = "Token") String token)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Boolean changed = ConfigurationController.setTokenPythia(nal, token);
      return Stream.of(new BooleanResult(changed));
    } catch (Exception | Neo4jConnectionError | MissingFileException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }
}
