package com.castsoftware.artemis.procedures;

import com.castsoftware.artemis.controllers.InteractionsController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class InteractionsProcedure {

    @Context
    public GraphDatabaseService db;

    @Context
    public Transaction transaction;

    @Context
    public Log log;

    @Procedure(value = "artemis.get.interactions", mode = Mode.WRITE)
    @Description("artemis.get.interactions(String ApplicationContext, String language, Boolean flagNodes) - Get the interactions in an application")
    public Stream<OutputMessage> launchDetection(@Name(value = "ApplicationContext") String applicationContext,
                                                 @Name(value = "Language") String language,
                                                 @Name(value = "FlagNodes", defaultValue = "false") Boolean flagNodes) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            List<OutputMessage> detectedFrameworks = InteractionsController.launchDetection(nal, applicationContext, language, flagNodes);

            return detectedFrameworks.stream();
        } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }

    }

}
