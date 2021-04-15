/*
 *  Friendly exporter for Neo4j - Copyright (C) 2020  Hugo JOBY
 *
 *      This library is free software; you can redistribute it and/or modify it under the terms
 *      of the GNU Lesser General Public License as published by the Free Software Foundation;
 *      either version 2.1 of the License, or (at your option) any later version.
 *      This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *      without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *      See the GNU Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public License along with this library;
 *      If not, see <https://www.gnu.org/licenses/>.
 */

package com.castsoftware.artemis.io;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.exceptions.file.FileIOException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.results.OutputMessage;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Exporter {

  // Return message queue
  private static final List<OutputMessage> MESSAGE_QUEUE = new ArrayList<>();

  // Default properties
  private static final String DELIMITER = Configuration.get("io.csv.delimiter");
  private static final String EXTENSION = Configuration.get("io.csv.csv_extension");
  private static final String INDEX_COL = Configuration.get("io.index_col");
  private static final String INDEX_OUTGOING = Configuration.get("io.index_outgoing");
  private static final String INDEX_INCOMING = Configuration.get("io.index_incoming");
  private static final String RELATIONSHIP_PREFIX = Configuration.get("io.file.prefix.relationship");
  private static final String NODE_PREFIX = Configuration.get("io.file.prefix.node");

  private GraphDatabaseService db;
  private Log log;
  private Transaction transaction;

  // Parameters
  private Boolean saveRelationshipParams = false;
  private Path pathParams = null;

  // Class members
  private Set<Long> nodeLabelMap; // List of node Id visited
  private Set<Label> closedLabelSet; // Already visited Node labels
  private Map<String, List<Node>> labelNodeMap; // To visit Node labels
  private Set<String> createdFilenameList; // Filename created during this session

  public Exporter(Neo4jAL neo4jAL) {
    this.db = neo4jAL.getDb();
    this.log = neo4jAL.getLogger();
    this.transaction = neo4jAL.getTransaction();

    this.closedLabelSet = new HashSet<>();
    this.nodeLabelMap = new HashSet<>();
    this.createdFilenameList = new HashSet<>();
  }

  public Stream<OutputMessage> save(
      List<String> labels,
      List<Node> nodeList,
      Path path,
      String zipFileName,
      Boolean saveRelationShip)
      throws ProcedureException {
    MESSAGE_QUEUE.clear();

    // Init parameters
    saveRelationshipParams = saveRelationShip;
    pathParams = path;

    // Init members
    labelNodeMap = new HashMap<>();
    for (Node n : nodeList) {
      // Get the label of the node
      for (Label l : n.getLabels()) {
        if (labels.contains(l.name())) {

          if (!labelNodeMap.containsKey(l.name())) labelNodeMap.put(l.name(), new ArrayList<>());
          labelNodeMap.get(l.name()).add(n);
          continue;
        }
      }
    }

    // openTransaction
    try {
      String targetName = zipFileName.concat(".zip");
      log.info(String.format("Saving Configuration to %s ...", targetName));

      saveNodes();
      if (saveRelationshipParams) saveRelationships();

      createZip(targetName);
      MESSAGE_QUEUE.add(new OutputMessage("Saving done"));

      return MESSAGE_QUEUE.stream();
    } catch (FileIOException e) {
      throw new ProcedureException(e);
    }
  }

  /**
   * Iterate through label list, find associated nodes and export them to a CSV file.
   *
   * @throws FileIOException
   */
  private void saveNodes() throws FileIOException {
    for (Map.Entry<String, List<Node>> en : labelNodeMap.entrySet()) {
      Label toTreat = Label.label(en.getKey());

      String content = "";

      try {
        content = exportLabelToCSV(toTreat, en.getValue());
      } catch (Neo4jNoResult | Neo4jQueryException e) {
        log.error("Error trying to save label : ".concat(toTreat.name()), e);
        MESSAGE_QUEUE.add(
            new OutputMessage("Error : No nodes found with label : ".concat(toTreat.name())));
        continue;
      }

      String filename = NODE_PREFIX.concat(toTreat.name()).concat(EXTENSION);
      createdFilenameList.add(filename);

      try (FileWriter writer = new FileWriter(pathParams.resolve(filename).toFile(), true)) {
        writer.write(content);
      } catch (Exception e) {
        throw new FileIOException(
            "Error : Impossible to create/open file with name ".concat(filename), e, "SAVExSAVE01");
      }
    }
  }

  /**
   * Save relationship between found nodes. The saving process will parse relationships a first time
   * to extract all possible properties. Then, it will create associated file, pushes the full set
   * of header values, and write back the relationships. If a relationship doesn't contain a
   * property value, the column for this row will be left empty.
   *
   * @throws FileIOException
   */
  private void saveRelationships() throws FileIOException {

    Map<String, FileWriter> fileWriterMap = new HashMap<>();

    try {
      ArrayList<Relationship> relationships = new ArrayList<>();
      Map<String, Set<String>> relationshipsHeaders = new HashMap<>();

      // Parse all relationships, extract headers for each relations
      for (Long index : nodeLabelMap) {

        Node node = this.transaction.getNodeById(index);

        for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
          Node otherNode = rel.getOtherNode(node);
          // If the node was saved previously, save its associated relationships
          if (nodeLabelMap.contains(otherNode.getId())) {
            relationships.add(rel); // Save relationship for later

            String name = rel.getType().name();
            List<String> properties = new ArrayList<>();
            for (String prop : rel.getPropertyKeys())
              properties.add(prop); // Extract Iterable to List

            // Append or create header for this relationship
            if (relationshipsHeaders.containsKey(name)) {
              relationshipsHeaders.get(name).addAll(properties);
            } else {
              relationshipsHeaders.put(name, new HashSet<>(properties));
            }
          }
        }
      }

      // Open one FileWriter per name and append headers
      for (Map.Entry<String, Set<String>> pair : relationshipsHeaders.entrySet()) {

        String filename = RELATIONSHIP_PREFIX.concat(pair.getKey()).concat(EXTENSION);

        try {
          // Create a new file writer and write headers for each relationship type
          FileWriter writer = new FileWriter(pathParams.resolve(filename).toFile(), true);
          fileWriterMap.put(pair.getKey(), writer);
          createdFilenameList.add(filename);

          StringBuilder headers = new StringBuilder();
          headers.append(INDEX_OUTGOING.concat(DELIMITER)); // Add Source property
          headers.append(INDEX_INCOMING.concat(DELIMITER)); // Add Destination property
          headers.append(String.join(DELIMITER, pair.getValue())).append("\n");

          writer.write(headers.toString());
        } catch (IOException e) {
          log.error("Error : Impossible to create/open file with name ".concat(filename), e);
        }
      }

      // Parse previously saved relationships and write them back to their associated FileWriter
      for (Relationship rel : relationships) {
        String name = rel.getType().name();

        Set<String> headers = relationshipsHeaders.get(name);
        StringBuilder values = new StringBuilder();

        List<String> valueList = new ArrayList<>();
        // Append Source and destination nodes ID
        Long idSrc = rel.getStartNode().getId();
        Long idDest = rel.getEndNode().getId();
        valueList.add(idSrc.toString());
        valueList.add(idDest.toString());

        // Append rest of the properties
        for (String prop : headers) {
          String value = "";
          try {
            value = rel.getProperty(prop).toString();
          } catch (NotFoundException ignored) {
          }
          valueList.add(value);
        }

        fileWriterMap.get(name).write(String.join(DELIMITER, valueList).concat("\n"));
      }

    } catch (IOException rethrown) {
      throw new FileIOException("Error while saving relationships", rethrown, "SAVExSARE01");
    } finally {
      // Close FileWriters
      for (Map.Entry<String, FileWriter> pair : fileWriterMap.entrySet()) {
        try {
          pair.getValue().close();
        } catch (IOException e) {
          log.error("Error : Impossible to close file with name ".concat(pair.getKey()), e);
        }
      }
    }
  }

  /**
   * Appends all the files created during this process to the target zip. Every file appended will
   * be remove once added to the zip.
   *
   * @param targetName Name of the ZipFile
   * @throws IOException
   */
  private void createZip(String targetName) throws FileIOException {
    File f = pathParams.resolve(targetName).toFile();
    log.info("Creating zip file..");

    try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(f))) {

      for (String filename : createdFilenameList) {
        File fileToZip = pathParams.resolve(filename).toFile();

        try (FileInputStream fileStream = new FileInputStream(fileToZip)) {
          ZipEntry e = new ZipEntry(filename);
          zipOut.putNextEntry(e);

          byte[] bytes = new byte[1024];
          int length;
          while ((length = fileStream.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
          }
        } catch (Exception e) {
          log.error("An error occurred trying to zip file with name : ".concat(filename), e);
        }

        if (!fileToZip.delete())
          log.error("Error trying to delete file with name : ".concat(filename));
      }

    } catch (IOException e) {
      log.error("An error occurred trying create zip file with name : ".concat(targetName), e);
      throw new FileIOException(
          "An error occurred trying create zip file with name.", e, "SAVExCZIP01");
    }
  }

  /**
   * Convert a node list associated to the given label into a CSV format. If the
   * option @ConsiderNeighbors is active, neighbors label found will be added to the discovery list.
   *
   * @param label The label to save
   * @return <code>String</code> the list of node as CSV
   * @throws Neo4jNoResult No node with the label provided where found during parsing
   */
  private String exportLabelToCSV(Label label, List<Node> nodeList)
      throws Neo4jNoResult, Neo4jQueryException {
    Set<String> headers = new HashSet<>();

    for (Node n : nodeList) {
      // Retrieve all possible node property keys
      for (String s : n.getPropertyKeys()) headers.add(s);
    }

    // If no nodes were found, end with exception
    if (nodeList.isEmpty())
      throw new Neo4jNoResult(
          "No result for nodes with label".concat(label.name()),
          "findNodes(".concat(label.name()).concat(");"),
          "SAVExELTC02");

    // Create CSV string
    StringBuilder csv = new StringBuilder();
    csv.append(INDEX_COL.concat(DELIMITER)); // Add index property
    csv.append(String.join(DELIMITER, headers)).append("\n");

    log.info("Appending headers for label ".concat(label.name()));

    for (Node n : nodeList) {
      List<String> valueList = new ArrayList<>();

      // Using the Neo4j Node ID
      valueList.add(((Long) n.getId()).toString());
      nodeLabelMap.add(n.getId());

      for (String prop : headers) {
        String value = "";
        try {
          value = neo4jTypeToString(n, prop);
          value = value.replaceAll("\\n", " ").replaceAll("\\r\\n", " ");
        } catch (NotFoundException ignored) {
        }

        valueList.add(value);
      }
      csv.append(String.join(DELIMITER, valueList)).append("\n");
    }

    // Mark the label as visited
    closedLabelSet.add(label);
    return csv.toString();
  }

  public String neo4jTypeToString(Node n, String property) {
    if (!n.hasProperty(property)) {
      return "\"\"";
    }

    Object obj = n.getProperty(property);

    if (obj instanceof String[]) {
      String[] temp = (String[]) obj;
      return String.format("\"[%s]\"", String.join(", ", temp));
    }

    return String.format("\"%s\"", obj);
  }
}
