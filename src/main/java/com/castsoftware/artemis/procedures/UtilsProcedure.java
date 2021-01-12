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
            String message = UtilsController.setArtemisDirectory(artemisDirectory);

            return Stream.of(new OutputMessage(message));
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
    @Description("artemis.set.onlineMode() - Set the online mode of artemis value")
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

    @Procedure(value = "artemis.set.repositoryParse", mode = Mode.WRITE)
    @Description("artemis.set.repositoryParse(Boolean value) - Get the value of online mode.")
    public Stream<BooleanResult> getRepositoryMode(@Name(value = "Value", defaultValue = "true") Boolean value ) throws ProcedureException {
        try {
            Boolean mode = UtilsController.setRepositoryMode(value);
            return Stream.of(new BooleanResult(mode));
        } catch (Exception | MissingFileException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "artemis.get.repositoryParse", mode = Mode.WRITE)
    @Description("artemis.get.repositoryParse() - Get the value of online mode.")
    public Stream<BooleanResult> setRepositoryMode() throws ProcedureException {
        try {
            Boolean mode = UtilsController.getRepositoryMode();
            return Stream.of(new BooleanResult(mode));
        } catch (Exception  e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }
}
