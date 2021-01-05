package com.castsoftware.artemis.procedures;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.controllers.DetectionController;
import com.castsoftware.artemis.controllers.UtilsController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jConnectionError;
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

    @Procedure(value = "artemis.changeWorkspace", mode = Mode.WRITE)
    @Description("artemis.changeWorkspace(String name) - Change the workspace of the Artemis extension.")
    public Stream<OutputMessage> changeWorkspace(@Name(value = "ArtemisDirectory") String artemisDirectory) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            String message = UtilsController.changeArtemisDirectory(artemisDirectory);

            return Stream.of(new OutputMessage(message));
        } catch (Exception | Neo4jConnectionError | MissingFileException e) {
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


    @Procedure(value = "artemis.setOnlineMode", mode = Mode.WRITE)
    @Description("artemis.setOnlineMode() - Set the online mode of artemis value")
    public Stream<OutputMessage> setOnlineMode(@Name(value = "Value", defaultValue = "true") Boolean value ) throws ProcedureException {
        try {
            String mode = UtilsController.switchOnlineMode(value);
            return Stream.of(new OutputMessage(String.format("Online mode is now set on '%s'.", mode)));
        } catch (Exception | MissingFileException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "artemis.getOnlineMode", mode = Mode.WRITE)
    @Description("artemis.getOnlineMode() - Get the value of online mode.")
    public Stream<OutputMessage> getOnlineMode(@Name(value = "Value", defaultValue = "true") Boolean value ) throws ProcedureException {
        try {
            String mode = UtilsController.switchOnlineMode(value);
            return Stream.of(new OutputMessage(String.format("Online mode is now set on '%s'.", mode)));
        } catch (Exception | MissingFileException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "artemis.setRepositoryMode", mode = Mode.WRITE)
    @Description("artemis.setRepositoryMode() - Get the value of online mode.")
    public Stream<OutputMessage> setRepositoryMode(@Name(value = "Value", defaultValue = "true") Boolean value ) throws ProcedureException {
        try {
            String mode = UtilsController.switchRepositoryMode(value);
            return Stream.of(new OutputMessage(String.format("Repository mode is now set on '%s'.", mode)));
        } catch (Exception | MissingFileException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }
}
