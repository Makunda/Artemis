package com.castsoftware.artemis.procedures;

import com.castsoftware.artemis.controllers.DetectionController;
import com.castsoftware.artemis.controllers.UtilsController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.ProcedureException;
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

    @Procedure(value = "artemis.change", mode = Mode.WRITE)
    @Description("demeter.createConfiguration(String name) - Create a configuration node")
    public Stream<OutputMessage> launchDetection(@Name(value = "ArtemisDirectory") String artemisDirectory) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            String message = UtilsController.changeArtemisDirectory(artemisDirectory);

            return Stream.of(new OutputMessage(message));
        } catch (Exception | Neo4jConnectionError e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }

    }
}
