package com.castsoftware.artemis.procedures;

import com.castsoftware.artemis.controllers.SmtpController;
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

public class SmtpProcedure {
    @Context
    public GraphDatabaseService db;

    @Context
    public Transaction transaction;

    @Context
    public Log log;

    @Procedure(value = "artemis.get.mailsRecipients", mode = Mode.WRITE)
    @Description("artemis.get.mailsRecipients() - Get the current recipients for the mail campaign.")
    public Stream<OutputMessage> getMailsRecipients() throws ProcedureException {

        try {
            List<String> recipients = SmtpController.getMailsRecipients();
            return recipients.stream().map(OutputMessage::new);
        } catch (Exception e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }

    }

    @Procedure(value = "artemis.set.mailsRecipients", mode = Mode.WRITE)
    @Description("artemis.set.mailsRecipients(String listRecipients) - Set the list of recipients used during Artemis's mail campaigns.")
    public Stream<BooleanResult> setMailsRecipients(@Name(value = "ListRecipients") String listRecipients) throws ProcedureException {

        try {
            Boolean success = SmtpController.setMailsRecipients(listRecipients);
            return Stream.of(new BooleanResult(success));
        } catch (Exception  e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }

    }

}
