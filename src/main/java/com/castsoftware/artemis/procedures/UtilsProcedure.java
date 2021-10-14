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

package com.castsoftware.artemis.procedures;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.controllers.UtilsController;
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.io.Cleaner;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import com.castsoftware.artemis.results.BooleanResult;
import com.castsoftware.artemis.results.LongResult;
import com.castsoftware.artemis.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class UtilsProcedure {

  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  @Procedure(value = "artemis.get.workspace", mode = Mode.WRITE)
  @Description("artemis.get.workspace() - Get the workspace directory of the Artemis extension.")
  public Stream<OutputMessage> getWorkspace() throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      String message = UtilsController.getArtemisDirectory(nal);
      return Stream.of(new OutputMessage(message));
    } catch (Exception | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.set.workspace", mode = Mode.WRITE)
  @Description(
      "artemis.set.workspace(String name) - Change the workspace of the Artemis extension.")
  public Stream<OutputMessage> setWorkspace(
      @Name(value = "ArtemisDirectory") String artemisDirectory) throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      List<String> outputMessages = UtilsController.setArtemisDirectory(nal, artemisDirectory);
      return outputMessages.stream().map(OutputMessage::new);
    } catch (Exception | MissingFileException | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.version", mode = Mode.WRITE)
  @Description("artemis.version() - Get the version of the extension")
  public Stream<OutputMessage> getVersion() throws ProcedureException {
    try {
      String version = Configuration.get("artemis.version");
      return Stream.of(new OutputMessage(version));
    } catch (Exception e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.set.onlineMode", mode = Mode.WRITE)
  @Description("artemis.set.onlineMode(Boolean value) - Set the value of online mode ")
  public Stream<BooleanResult> setOnlineMode(
      @Name(value = "Value", defaultValue = "true") Boolean value) throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Boolean mode = UtilsController.setOnlineMode(nal, value);
      return Stream.of(new BooleanResult(mode));
    } catch (Exception | MissingFileException | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.get.onlineMode", mode = Mode.WRITE)
  @Description("artemis.get.onlineMode() - Get the value of online mode.")
  public Stream<BooleanResult> getOnlineMode() throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Boolean mode = UtilsController.getOnlineMode(nal);
      return Stream.of(new BooleanResult(mode));
    } catch (Exception | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.set.repositoryMode", mode = Mode.WRITE)
  @Description("artemis.set.repositoryMode(Boolean value) - Set the value of repository mode.")
  public Stream<BooleanResult> setRepositoryMode(
      @Name(value = "Value", defaultValue = "true") Boolean value) throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Boolean mode = UtilsController.setRepositoryMode(nal, value);
      return Stream.of(new BooleanResult(mode));
    } catch (Exception | MissingFileException | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.get.repositoryMode", mode = Mode.WRITE)
  @Description("artemis.get.repositoryMode() - Get the value of repository mode.")
  public Stream<BooleanResult> getRepositoryMode() throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Boolean mode = UtilsController.getRepositoryMode(nal);
      return Stream.of(new BooleanResult(mode));
    } catch (Exception | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.set.learningMode", mode = Mode.WRITE)
  @Description("artemis.set.learningMode(Boolean value) - Set the value of the learning mode.")
  public Stream<BooleanResult> setLearningMode(
      @Name(value = "Value", defaultValue = "true") Boolean value) throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Boolean mode = UtilsController.setLearningMode(nal, value);
      return Stream.of(new BooleanResult(mode));
    } catch (Exception | MissingFileException | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.get.learningMode", mode = Mode.WRITE)
  @Description("artemis.get.learningMode() - Get the value of learning mode.")
  public Stream<BooleanResult> getLearningMode() throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Boolean mode = UtilsController.getLearningMode(nal);
      return Stream.of(new BooleanResult(mode));
    } catch (Exception | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.set.persistentMode", mode = Mode.WRITE)
  @Description("artemis.set.persistentMode(Boolean value) - Set the value of the persistent mode.")
  public Stream<BooleanResult> setPersistentMode(
      @Name(value = "Value", defaultValue = "true") Boolean value) throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Boolean mode = UtilsController.setRepositoryMode(nal, value);
      return Stream.of(new BooleanResult(mode));
    } catch (Exception | MissingFileException | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.get.persistentMode", mode = Mode.WRITE)
  @Description("artemis.get.persistentMode() - Get the value of the persistent mode.")
  public Stream<BooleanResult> getPersistentMode() throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Boolean mode = UtilsController.getRepositoryMode(nal);
      return Stream.of(new BooleanResult(mode));
    } catch (Exception | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.install", mode = Mode.WRITE)
  @Description("artemis.install(String artemisDirectory) - Install the Artemis extension.")
  public Stream<OutputMessage> install(@Name(value = "ArtemisDirectory") String artemisDirectory)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      List<String> outputMessages = UtilsController.install(nal, artemisDirectory);

      return outputMessages.stream().map(OutputMessage::new);
    } catch (Exception | MissingFileException | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.refresh", mode = Mode.WRITE)
  @Description("artemis.refresh() - Refresh and update the framework nodes to the new version.")
  public Stream<LongResult> refresh() throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Long[] res = Cleaner.updateNodes(nal);
      return Arrays.stream(res).map(LongResult::new);
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }
}
