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

import com.castsoftware.artemis.controllers.InteractionsController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.results.InteractionResult;
import com.castsoftware.artemis.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class InteractionsProcedure {

  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  @Procedure(value = "artemis.interactions.search", mode = Mode.WRITE)
  @Description(
      "artemis.interactions.search(String Applications, String language, String search) - Search the presence of a pattern in applications")
  public Stream<InteractionResult> searchInteractions(
      @Name(value = "Applications") List<String> applications,
      @Name(value = "Language") String language,
      @Name(value = "ToSearch") String toSearch)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      List<InteractionResult> detectedInteraction = InteractionsController.getInteraction(nal, applications, language, toSearch);

      return detectedInteraction.stream();
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }
}
