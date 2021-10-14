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

import com.castsoftware.artemis.controllers.api.RegexNodeController;
import com.castsoftware.artemis.datasets.RegexNode;
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import com.castsoftware.artemis.results.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class RegexNodeApiProcedure {

  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  @Procedure(value = "artemis.api.regex.create.node", mode = Mode.WRITE)
  @Description(
      "artemis.api.regex.create.node(String name, String[] regexes, String[] internalTypes, String framework, String category, Optional Long parentId) - Create a new Regex Node")
  public Stream<RegexNodeResult> createRegexNode(
      @Name(value = "Name") String name,
      @Name(value = "Regexes") List<String> regexes,
      @Name(value = "InternalType") List<String> internalTypes,
      @Name(value = "Framework") String framework,
      @Name(value = "Category") String category,
      @Name(value = "ParentId", defaultValue = "-1") Long parentId)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      RegexNode rn;
      if (parentId == -1) {
        rn =
            RegexNodeController.createRegexNode(
                nal, name, regexes, internalTypes, framework, category);
      } else {
        rn =
            RegexNodeController.createRegexNodeWithParent(
                nal, name, regexes, internalTypes, framework, category, parentId);
      }
      return Stream.of(new RegexNodeResult(rn));
    } catch (Exception
        | Neo4jConnectionError
        | Neo4jQueryException
        | Neo4jBadNodeFormatException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.regex.update.node", mode = Mode.WRITE)
  @Description(
      "artemis.api.regex.update.node(Long id, String name, String[] regexes, String[] internalTypes, String framework, String category) - Update a Regex Node")
  public Stream<RegexNodeResult> updateRegexNode(
      @Name(value = "Id") Long id,
      @Name(value = "Name") String name,
      @Name(value = "regexes") List<String> regexes,
      @Name(value = "InternalType") List<String> internalTypes,
      @Name(value = "framework") String framework,
      @Name(value = "category") String category,
      @Name(value = "parentId") Long parentID)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      RegexNode rn =
          RegexNodeController.updateById(
              nal, id, name, regexes, internalTypes, framework, category, parentID);
      if (rn == null) return Stream.empty();
      return Stream.of(new RegexNodeResult(rn));
    } catch (Exception
        | Neo4jConnectionError
        | Neo4jQueryException
        | Neo4jBadNodeFormatException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.regex.get.node.byId", mode = Mode.WRITE)
  @Description("artemis.api.regex.get.node.byId(Long idNode) - Get a regex node by its id")
  public Stream<RegexNodeResult> getById(@Name(value = "IdNode") Long idNode)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      RegexNode rn = RegexNodeController.getRegexNodeById(nal, idNode);
      return Stream.of(new RegexNodeResult(rn));
    } catch (Exception
        | Neo4jConnectionError
        | Neo4jQueryException
        | Neo4jBadNodeFormatException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.regex.link.node.to.parent", mode = Mode.WRITE)
  @Description(
      "artemis.api.regex.link.node.to.parent(Long idChild, Long idParent) - Link a Regex node to its parent")
  public Stream<RelationshipResult> linkToParent(
      @Name(value = "IdChild") Long idChild, @Name(value = "IdParent") Long idParent)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Relationship rn = RegexNodeController.linkToParent(nal, idChild, idParent);
      return Stream.of(new RelationshipResult(rn));
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.regex.get.all.nodes", mode = Mode.WRITE)
  @Description("artemis.api.regex.get.all.nodes() - Get all the regex nodes")
  public Stream<RegexNodeResult> getAllNodes() throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      List<RegexNode> rnList = RegexNodeController.getAllRegexNode(nal);
      return rnList.stream().map(RegexNodeResult::new);
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.regex.remove.node.byId", mode = Mode.WRITE)
  @Description("artemis.api.regex.remove.node.byId(Long idNode) - Get a regex node by its id")
  public Stream<BooleanResult> removeById(@Name(value = "IdNode") Long idNode)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Boolean op = RegexNodeController.removeRegexNodeById(nal, idNode);
      return Stream.of(new BooleanResult(op));
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.regex.flag", mode = Mode.WRITE)
  @Description(
      "artemis.api.regex.flag() - Flag the nodes matching the regex nodes using the demeter.")
  public Stream<LongResult> flagNodes() throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Long aLong = RegexNodeController.flagRegexNodes(nal);
      return Stream.of(new LongResult(aLong));
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.regex.test", mode = Mode.WRITE)
  @Description("artemis.api.regex.test(Long idRegexNode) - Test the Regex Node")
  public Stream<LongResult> testRegex(@Name(value = "IdNode") Long idNode)
      throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Long aLong = RegexNodeController.testRegexNode(nal, idNode);
      return Stream.of(new LongResult(aLong));
    } catch (Exception
        | Neo4jConnectionError
        | Neo4jQueryException
        | Neo4jBadNodeFormatException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.regex.get.request", mode = Mode.WRITE)
  @Description(
      "artemis.api.regex.get.request(Long idRegexNode) - Get the request associated to a node")
  public Stream<OutputMessage> getRequest(@Name(value = "IdNode") Long idNode)
      throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      String req = RegexNodeController.getRegexRequest(nal, idNode);
      return Stream.of(new OutputMessage(req));
    } catch (Exception
        | Neo4jConnectionError
        | Neo4jQueryException
        | Neo4jBadNodeFormatException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }
}
