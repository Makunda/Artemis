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

import com.castsoftware.artemis.controllers.DetectionController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.exceptions.google.GoogleBadResponseCodeException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.exceptions.nlp.NLPIncorrectConfigurationException;
import com.castsoftware.artemis.results.FrameworkResult;
import com.castsoftware.artemis.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

public class DetectionProcedure {

  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  @Procedure(value = "artemis.launch.detection", mode = Mode.WRITE)
  @Description(
      "artemis.launch.detection(String ApplicationContext, String Language) - Launch Detection for a specific language")
  public Stream<FrameworkResult> launchDetection(
      @Name(value = "ApplicationContext") String applicationContext,
      @Name(value = "Language", defaultValue = "") String language,
      @Name(value = "FlagNodes", defaultValue = "true") Boolean flagNodes)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      List<FrameworkResult> detectedFrameworks =
          DetectionController.launchDetection(nal, applicationContext, language, flagNodes);

      return detectedFrameworks.stream();
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jBadRequestException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.launch.bulkDetection", mode = Mode.WRITE)
  @Description(
      "artemis.launch.bulkDetection(String Language) - Launch Detection for a specific language")
  public Stream<FrameworkResult> bulkDetection(
      @Name(value = "Language") String language,
      @Name(value = "FlagNodes", defaultValue = "false") Boolean flagNodes)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      List<FrameworkResult> detectedFrameworks =
          DetectionController.launchBulkDetection(nal, language, flagNodes);

      return detectedFrameworks.stream();
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException | MissingFileException | NLPIncorrectConfigurationException | GoogleBadResponseCodeException | Neo4jBadRequestException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.trainModel", mode = Mode.WRITE)
  @Description("artemis.trainModel() - Launch Detection for a specific language")
  public Stream<OutputMessage> trainModel() throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);

      Instant start = Instant.now();
      DetectionController.trainArtemis(nal);
      Instant stop = Instant.now();

      String message =
          String.format(
              "Model was trained in '%d' milliseconds.", Duration.between(start, stop).toMillis());

      return Stream.of(new OutputMessage(message));
    } catch (Exception | Neo4jConnectionError | NLPIncorrectConfigurationException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }
}
