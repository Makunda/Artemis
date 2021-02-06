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

package com.castsoftware.artemis.procedures.user.input;

import com.castsoftware.artemis.controllers.user.input.CustomFrameworkController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.results.BooleanResult;
import com.castsoftware.artemis.results.FrameworkResult;
import com.castsoftware.artemis.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class CustomFrameworkProcedure {
  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  @Procedure(value = "artemis.launch.custom.framework.discovery", mode = Mode.WRITE)
  @Description(
      "artemis.launch.custom.framework.discovery() - Get the Objects flagged by the user with the custom tag framework  ")
  public Stream<FrameworkResult> getUserCustomFrameworks() throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      List<FrameworkNode> detectedFrameworks =
          CustomFrameworkController.getUserCustomFrameworks(nal);
      return detectedFrameworks.stream().map(FrameworkResult::new);
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.get.framework.tags.presence", mode = Mode.WRITE)
  @Description(
      "artemis.get.framework.tags.presence() - Get the Objects flagged by the user with the custom tag framework ")
  public Stream<BooleanResult> getTagPresence() throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      boolean bool = CustomFrameworkController.isTagPresent(nal);
      return Stream.of(new BooleanResult(bool));
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.get.framework.tag", mode = Mode.WRITE)
  @Description("artemis.get.framework.tag - Get the custom tag framework  ")
  public Stream<OutputMessage> getTag() throws ProcedureException {
    try {
      String tag = CustomFrameworkController.getTag();
      return Stream.of(new OutputMessage(tag));
    } catch (Exception e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.set.framework.tag", mode = Mode.WRITE)
  @Description("artemis.set.framework.tag(String tag) - Set the custom tag framework  ")
  public Stream<OutputMessage> setTag(@Name(value = "Tag") String tag) throws ProcedureException {
    try {
      String newTag = CustomFrameworkController.setTag(tag);
      return Stream.of(new OutputMessage(newTag));
    } catch (Exception | MissingFileException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }
}
