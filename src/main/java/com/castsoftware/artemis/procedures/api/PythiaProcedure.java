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

import com.castsoftware.artemis.controllers.api.LanguageController;
import com.castsoftware.artemis.controllers.api.PythiaComController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.results.BooleanResult;
import com.castsoftware.artemis.results.FrameworkResult;
import com.castsoftware.artemis.results.LongResult;
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

public class PythiaProcedure {

    @Context
    public GraphDatabaseService db;

    @Context public Transaction transaction;

    @Context public Log log;

    @Procedure(value = "artemis.api.pythia.get.lastUpdate", mode = Mode.WRITE)
    @Description(
            "artemis.api.pythia.get.lastUpdate() - Get the last update of the oracle")
    public Stream<LongResult> getLastUpdate() throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            boolean connected = PythiaComController.isOracleConnected(nal);
            if(!connected) return Stream.empty();

            Long lastUpdate = PythiaComController.getLastUpdate(nal);

            return Stream.of(new LongResult(lastUpdate));
        } catch (Exception | Neo4jConnectionError e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "artemis.api.pythia.get.connection", mode = Mode.WRITE)
    @Description(
            "artemis.api.pythia.get.connection() - Check if Artemis is connected to the Pythia")
    public Stream<BooleanResult> getConnection() throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            boolean connected = PythiaComController.isOracleConnected(nal);
            return Stream.of(new BooleanResult(connected));
        } catch (Exception | Neo4jConnectionError e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "artemis.api.pythia.pull.frameworks.forecast", mode = Mode.WRITE)
    @Description(
            "artemis.api.pythia.pull.frameworks.forecast() - Get a forecast of the pull")
    public Stream<LongResult> pullFrameworksForecast() throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            boolean connected = PythiaComController.isOracleConnected(nal);
            if(!connected) return Stream.empty();

            Long numFramework = PythiaComController.pullFrameworksForecast(nal);
            return Stream.of(new LongResult(numFramework));
        } catch (Exception | Neo4jConnectionError | Neo4jBadRequestException | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "artemis.api.pythia.pull.frameworks", mode = Mode.WRITE)
    @Description(
            "artemis.api.pythia.pull.frameworks() - Check if Artemis is connected to the Pythia")
    public Stream<FrameworkResult> pullFrameworks() throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            boolean connected = PythiaComController.isOracleConnected(nal);
            if(!connected) return Stream.empty();

            List<FrameworkNode> listFrameworks = PythiaComController.pullFrameworks(nal);
            return listFrameworks.stream().map(FrameworkResult::new);
        } catch (Exception | Neo4jConnectionError | Neo4jBadRequestException | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }


}
