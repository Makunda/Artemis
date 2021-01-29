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
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class LanguageApiProcedure {
    @Context
    public GraphDatabaseService db;

    @Context public Transaction transaction;

    @Context public Log log;

    @Procedure(value = "artemis.get.supported.languages", mode = Mode.WRITE)
    @Description(
            "artemis.get.supported.languages() - Get the list of supported languages")
    public Stream<OutputMessage> getSupportedLanguages() throws ProcedureException {

        try {
            List<String> languages = LanguageController.getSupportedLanguages();
            return languages.stream().map(OutputMessage::new);
        } catch (Exception e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }


}
