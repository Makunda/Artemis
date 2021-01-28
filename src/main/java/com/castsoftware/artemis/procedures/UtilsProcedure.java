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
import com.castsoftware.artemis.controllers.DetectionController;
import com.castsoftware.artemis.controllers.UtilsController;
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

import java.util.List;
import java.util.stream.Stream;

public class UtilsProcedure {

    @Context
    public GraphDatabaseService db;

    @Context
    public Transaction transaction;

    @Context
    public Log log;


    @Procedure(value = "artemis.get.workspace", mode = Mode.WRITE)
    @Description("artemis.get.workspace() - Get the workspace directory of the Artemis extension.")
    public Stream<OutputMessage> getWorkspace() throws ProcedureException {
        try {
            String message = UtilsController.getArtemisDirectory();
            return Stream.of(new OutputMessage(message));
        } catch (Exception e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }

    }

    @Procedure(value = "artemis.set.workspace", mode = Mode.WRITE)
    @Description("artemis.set.workspace(String name) - Change the workspace of the Artemis extension.")
    public Stream<OutputMessage> setWorkspace(@Name(value = "ArtemisDirectory") String artemisDirectory) throws ProcedureException {
        try {
            List<String> outputMessages = UtilsController.setArtemisDirectory(artemisDirectory);
            return outputMessages.stream().map(OutputMessage::new);
        } catch (Exception | MissingFileException e) {
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
    public Stream<BooleanResult> setOnlineMode(@Name(value = "Value", defaultValue = "true") Boolean value ) throws ProcedureException {
        try {
            Boolean mode = UtilsController.setOnlineMode(value);
            return Stream.of(new BooleanResult(mode));
        } catch (Exception | MissingFileException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "artemis.get.onlineMode", mode = Mode.WRITE)
    @Description("artemis.get.onlineMode() - Get the value of online mode.")
    public Stream<BooleanResult> getOnlineMode() throws ProcedureException {
        try {
            Boolean mode = UtilsController.getOnlineMode();
            return Stream.of(new BooleanResult(mode));
        } catch (Exception  e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "artemis.set.repositoryMode", mode = Mode.WRITE)
    @Description("artemis.set.repositoryMode(Boolean value) - Set the value of repository mode.")
    public Stream<BooleanResult> setRepositoryMode(@Name(value = "Value", defaultValue = "true") Boolean value ) throws ProcedureException {
        try {
            Boolean mode = UtilsController.setRepositoryMode(value);
            return Stream.of(new BooleanResult(mode));
        } catch (Exception | MissingFileException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "artemis.get.repositoryMode", mode = Mode.WRITE)
    @Description("artemis.get.repositoryMode() - Get the value of repository mode.")
    public Stream<BooleanResult> getRepositoryMode() throws ProcedureException {
        try {
            Boolean mode = UtilsController.getRepositoryMode();
            return Stream.of(new BooleanResult(mode));
        } catch (Exception  e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "artemis.set.persistentMode", mode = Mode.WRITE)
    @Description("artemis.set.persistentMode(Boolean value) - Set the value of the persistent mode.")
    public Stream<BooleanResult> setPersistentMode(@Name(value = "Value", defaultValue = "true") Boolean value ) throws ProcedureException {
        try {
            Boolean mode = UtilsController.setRepositoryMode(value);
            return Stream.of(new BooleanResult(mode));
        } catch (Exception | MissingFileException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "artemis.get.persistentMode", mode = Mode.WRITE)
    @Description("artemis.get.persistentMode() - Get the value of the persistent mode.")
    public Stream<BooleanResult> getPersistentMode() throws ProcedureException {
        try {
            Boolean mode = UtilsController.getRepositoryMode();
            return Stream.of(new BooleanResult(mode));
        } catch (Exception  e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "artemis.install", mode = Mode.WRITE)
    @Description("artemis.install(String artemisDirectory) - Install the Artemis extension.")
    public Stream<OutputMessage> install(@Name(value = "ArtemisDirectory") String artemisDirectory) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            List<String> outputMessages = UtilsController.install(nal, artemisDirectory);

            return outputMessages.stream().map(OutputMessage::new);
        } catch (Exception | MissingFileException | Neo4jConnectionError e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }

    }

}
