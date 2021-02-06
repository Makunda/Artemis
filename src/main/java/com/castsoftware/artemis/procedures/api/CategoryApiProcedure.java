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

import com.castsoftware.artemis.controllers.api.CategoryController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.CategoryNode;
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.results.BooleanResult;
import com.castsoftware.artemis.results.CategoryNodeResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class CategoryApiProcedure {

  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  @Procedure(value = "artemis.api.category.get.all.nodes", mode = Mode.WRITE)
  @Description("artemis.api.category.get.all.nodes() - Get all the category nodes")
  public Stream<CategoryNodeResult> getAllNodes() throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      List<CategoryNode> cnList = CategoryController.getAllNodes(nal);
      return cnList.stream().map(CategoryNodeResult::new);
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.category.update.node", mode = Mode.WRITE)
  @Description(
      "artemis.api.category.update.node(Long id, String name, String iconUrl) - Update a category node")
  public Stream<CategoryNodeResult> updateNode(
      @Name(value = "Id") Long id,
      @Name(value = "Name") String name,
      @Name(value = "IconUrl", defaultValue = "") String iconUrl)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      CategoryNode cn = CategoryController.updateById(nal, id, name, iconUrl);
      return Stream.of(new CategoryNodeResult(cn));
    } catch (Exception
        | Neo4jConnectionError
        | Neo4jQueryException
        | Neo4jBadNodeFormatException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.category.get.node.byId", mode = Mode.WRITE)
  @Description("artemis.api.category.get.node.byId(Long idNode) - Get a category node by its id")
  public Stream<CategoryNodeResult> getById(@Name(value = "IdNode") Long idNode)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      CategoryNode cn = CategoryController.getNodeById(nal, idNode);
      return Stream.of(new CategoryNodeResult(cn));
    } catch (Exception
        | Neo4jConnectionError
        | Neo4jQueryException
        | Neo4jBadNodeFormatException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.category.create.node", mode = Mode.WRITE)
  @Description(
      "artemis.api.category.create.node(String name, Optional String IconUrl) - Create a new Category Node")
  public Stream<CategoryNodeResult> createNode(
      @Name(value = "Name") String name, @Name(value = "IconUrl", defaultValue = "") String iconUrl)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      CategoryNode cn = CategoryController.createNode(nal, name, iconUrl);
      return Stream.of(new CategoryNodeResult(cn));
    } catch (Exception | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "artemis.api.category.remove.node", mode = Mode.WRITE)
  @Description("artemis.api.category.remove.node(Long idNode) - Remove a category Node")
  public Stream<BooleanResult> removeNode(@Name(value = "idNode") Long idNode)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Boolean result = CategoryController.removeNodeById(nal, idNode);
      return Stream.of(new BooleanResult(result));
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }
}
