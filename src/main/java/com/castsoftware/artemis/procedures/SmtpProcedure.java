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

import com.castsoftware.artemis.controllers.SmtpController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.ProcedureException;
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
  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

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
  @Description(
      "artemis.set.mailsRecipients(String listRecipients) - Set the list of recipients used during Artemis's mail campaigns.")
  public Stream<BooleanResult> setMailsRecipients(
      @Name(value = "ListRecipients") String listRecipients) throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Boolean success = SmtpController.setMailsRecipients(nal, listRecipients);
      return Stream.of(new BooleanResult(success));
    } catch (Exception | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.get.smtpConfiguration", mode = Mode.WRITE)
  @Description("artemis.get.smtpConfiguration() - Get the configuration of the SMTP server.")
  public Stream<OutputMessage> getMailConfiguration() throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      return SmtpController.getMailConfiguration(nal).stream().map(OutputMessage::new);
    } catch (Exception | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }


}
