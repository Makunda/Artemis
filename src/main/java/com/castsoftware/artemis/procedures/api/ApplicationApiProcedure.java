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

import com.castsoftware.artemis.controllers.ApplicationController;
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.global.SupportedLanguage;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import com.castsoftware.artemis.results.DetectionCandidateResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ApplicationApiProcedure {

  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  @Procedure(value = "artemis.api.application.get.scanned.languages", mode = Mode.WRITE)
  @Description(
      "artemis.api.application.get.scanned.languages(String application) - Check the languages already scanned in an application")
  public Stream<DetectionCandidateResult> getLanguages(
      @Name(value = "Application") String application) throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      List<SupportedLanguage> sup = ApplicationController.getScannedLanguages(nal, application);

      return Stream.of(new DetectionCandidateResult(application, sup));
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.application.add.scanned.languages", mode = Mode.WRITE)
  @Description(
      "artemis.api.application.add.scanned.languages(String application, String language) - Check the languages already scanned in an application")
  public Stream<DetectionCandidateResult> addLanguage(
      @Name(value = "Application") String application, @Name(value = "Language") String language)
      throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      List<SupportedLanguage> sup = ApplicationController.getScannedLanguages(nal, application);

      return Stream.of(new DetectionCandidateResult(application, sup));
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.application.reset.scanned.languages", mode = Mode.WRITE)
  @Description(
      "artemis.api.application.reset.scanned.languages(String application) - Reset the languages already scanned in an application")
  public void resetLanguages(@Name(value = "Application") String application)
      throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      ApplicationController.resetLanguages(nal, application);
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.application.get.candidate.languages", mode = Mode.WRITE)
  @Description(
      "artemis.api.application.get.candidate.languages(String application) - Get the candidate languages for one application")
  public Stream<DetectionCandidateResult> getCandidateLanguages(
      @Name(value = "Application") String application) throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      List<SupportedLanguage> sup = ApplicationController.getCandidatesLanguages(nal, application);

      return Stream.of(new DetectionCandidateResult(application, sup));
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.application.get.all.candidate.languages", mode = Mode.WRITE)
  @Description(
      "artemis.api.application.get.all.candidate.languages() - Get all the applications candidate for the language detection")
  public Stream<DetectionCandidateResult> getAllCandidateLanguages() throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Map<String, List<SupportedLanguage>> sup = ApplicationController.getAllCandidates(nal);
      List<DetectionCandidateResult> arr = new ArrayList<>();

      for (Map.Entry<String, List<SupportedLanguage>> en : sup.entrySet()) {
        arr.add(new DetectionCandidateResult(en.getKey(), en.getValue()));
      }

      return arr.stream();
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }
}
