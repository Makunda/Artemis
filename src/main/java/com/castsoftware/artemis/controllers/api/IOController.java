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

package com.castsoftware.artemis.controllers.api;

import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.io.Exporter;
import com.castsoftware.artemis.results.OutputMessage;
import com.castsoftware.artemis.utils.Workspace;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

public class IOController {

  /**
   * Export the list of Frameworks
   *
   * @param neo4jAL Neo4j Access Layer
   * @return
   * @throws ProcedureException
   */
  public static Stream<OutputMessage> exportFrameworks(Neo4jAL neo4jAL, String path)
          throws ProcedureException, Neo4jQueryException {
    List<String> listLabel = Arrays.asList(FrameworkNode.getLabel());
    List<Node> listNode = new ArrayList<>();

    String req = String.format("MATCH (o:%s) WHERE o.%s=$frameworkType RETURN o as framework", FrameworkNode.getLabel(), FrameworkNode.getTypeProperty());
    Map<String, Object> params = Map.of("frameworkType", "Framework");
    Result res = neo4jAL.executeQuery(req, params);
    while (res.hasNext()) {
      listNode.add((Node) res.next().get("framework"));
    }

    return exportNodes(neo4jAL, path, listLabel, listNode);
  }

  public static Stream<OutputMessage> exportAllFrameworks(Neo4jAL neo4jAL, String path)
          throws ProcedureException, Neo4jQueryException {
    List<String> listLabel = Arrays.asList(FrameworkNode.getLabel());
    List<Node> listNode = new ArrayList<>();

    String req = String.format("MATCH (o:%s) RETURN o as framework", FrameworkNode.getLabel());
    Map<String, Object> params = Map.of("frameworkType", "Framework");
    Result res = neo4jAL.executeQuery(req, params);
    while (res.hasNext()) {
      listNode.add((Node) res.next().get("framework"));
    }

    return exportNodes(neo4jAL, path, listLabel, listNode);
  }


  private static Stream<OutputMessage>exportNodes(Neo4jAL neo4jAL, String path, List<String> labels, List<Node> listNode) throws ProcedureException, Neo4jQueryException {
    Path exportPath = null;
    if (path.isBlank() || Files.exists(Path.of(path))) {
      neo4jAL.logInfo(String.format("The path '%s' doesn't seem to be valid. Will use default path : %s ", path,
              Workspace.getExportFolder(neo4jAL).toString()));
      exportPath = Workspace.getExportFolder(neo4jAL);
    } else {
      exportPath = Path.of(path);
    }

    Date date = new Date();
    SimpleDateFormat SDF = new SimpleDateFormat("yyyy_MM_dd_HHmmss");
    String asString = SDF.format(date);

    Exporter exporter = new Exporter(neo4jAL);
    return exporter.save(labels, listNode, exportPath, "Export_" + asString, false);
  }
}