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
import com.castsoftware.artemis.neo4j.Neo4jAL;
import com.castsoftware.artemis.results.BooleanResult;
import com.castsoftware.artemis.results.FrameworkResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.stream.Stream;

public class StatusProcedure {
  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  @Procedure(value = "artemis.get.oracle.status", mode = Mode.WRITE)
  @Description("artemis.get.oracle.status() - Get the status of the Oracle Communication  ")
  public Stream<BooleanResult> getOracleStatus() throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Boolean mode = OracleController.getOracleComStatus(nal);
      return Stream.of(new BooleanResult(mode));
    } catch (Exception | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.test.oracle.find", mode = Mode.WRITE)
  @Description(
      "artemis.test.oracle.find(String frameworkName, Optional String internalType) - Get the status of the Oracle Communication  ")
  public Stream<FrameworkResult> testOracleFind(
      @Name(value = "FrameworkName") String frameworkName,
      @Name(value = "InternalType", defaultValue = "") String internalType)
      throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      FrameworkNode fn = OracleController.testOracleFindings(nal, frameworkName, internalType);

      if (fn == null) return Stream.empty();

      return Stream.of(new FrameworkResult(fn));
    } catch (Exception | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.test.oracle.addFramework", mode = Mode.WRITE)
  @Description("artemis.test.oracle.addFramework() - Send a test Framework to the Oracle")
  public Stream<BooleanResult> testOracleAddFramework() throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      boolean val = OracleController.testOracleAddFramework(nal);

      return Stream.of(new BooleanResult(val));
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
