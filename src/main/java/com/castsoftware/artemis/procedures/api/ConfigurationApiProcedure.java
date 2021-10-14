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
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Procedure;

import java.util.List;
import java.util.stream.Stream;

public class ConfigurationApiProcedure {
  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  @Procedure(value = "artemis.api.configuration.get.detection.property", mode = Mode.WRITE)
  @Description(
      "artemis.api.configuration.get.detection.property - Get the name of detection property applied on the nodes during the detection")
  public Stream<OutputMessage> getNodeDetectionProperty() throws ProcedureException {
    try {
      String detectionProperty = ConfigurationController.getNodeDetectionProperty();
      return Stream.of(new OutputMessage(detectionProperty));
    } catch (Exception | MissingFileException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.configuration.get.category.property", mode = Mode.WRITE)
  @Description(
      "artemis.api.configuration.get.category.property - Get the name of category property applied on the nodes during the detection")
  public Stream<OutputMessage> getNodeCategoryProperty() throws ProcedureException {

    try {
      String detectionProperty = ConfigurationController.getNodeCategoryProperty();
      return Stream.of(new OutputMessage(detectionProperty));
    } catch (Exception | MissingFileException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.configuration.get.detection.property.values", mode = Mode.WRITE)
  @Description(
      "artemis.api.configuration.get.detection.property.values - Get the different values of the detection property")
  public Stream<OutputMessage> getListDetectionValues() throws ProcedureException {

    try {
      List<String> values = ConfigurationController.getListDetectionValues();
      return values.stream().map(OutputMessage::new);
    } catch (Exception | MissingFileException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }
}
