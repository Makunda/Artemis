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

import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.modules.pythia.Pythia;
import com.castsoftware.artemis.modules.pythia.exceptions.PythiaException;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import com.castsoftware.artemis.results.BooleanResult;
import com.castsoftware.artemis.results.FrameworkResult;
import com.castsoftware.artemis.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.stream.Stream;

public class StatusProcedure {
  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  @Procedure(value = "artemis.get.pythia.status", mode = Mode.WRITE)
  @Description("artemis.get.pythia.status(String url, String token) - Get the status of the pythia Communication  ")
  public Stream<OutputMessage> getOracleStatus(
          @Name(value = "Url") String url,
          @Name(value = "Token") String token
  ) throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Pythia pythia = new Pythia(url, token);

      String status;
      try {
        status = pythia.getStatus();
      } catch (PythiaException e) {
        status = "UNREACHABLE";
        nal.logError("Checking the status produced an exception.", e);
      }

      return Stream.of(new OutputMessage(String.format("API Status %s", status)));
    } catch (Exception | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

}
