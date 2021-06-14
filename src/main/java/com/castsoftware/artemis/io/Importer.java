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
import com.castsoftware.artemis.datasets.CategoryNode;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.exceptions.ProcedureException;
import com.castsoftware.artemis.exceptions.file.FileCorruptedException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Importer {

  // Return message queue
  private static final List<String> MESSAGE_QUEUE = new ArrayList<>();

  // Default values ( Should be added to a property file )
  private static final String DELIMITER = Configuration.get("io.csv.delimiter");
  private static final String EXTENSION = Configuration.get("io.csv.csv_extension");
  private static final String INDEX_COL = Configuration.get("io.index_col");
  private static final String INDEX_OUTGOING = Configuration.get("io.index_outgoing");
  private static final String INDEX_INCOMING = Configuration.get("io.index_incoming");
  private static final String RELATIONSHIP_PREFIX =
      Configuration.get("io.file.prefix.relationship");
  private static final String NODE_PREFIX = Configuration.get("io.file.prefix.node");

  private static final String FRAMEWORK_ID = Configuration.get("artemis.frameworkNode.name");
  private static final String FRAMEWORK_LABEL = Configuration.get("artemis.frameworkNode.label");

  // Unique property filter
  Map<String, List<String>> mapUniqueProperty = Map.of(
          FrameworkNode.getLabel(), List.of(FrameworkNode.getPatternProperty()), // Unique on Pattern Property
          CategoryNode.getLabel(), List.of(CategoryNode.getNameProperty())
  );



  // Members
  private Long countLabelCreated;
  private Long countRelationTypeCreated;
  private Long ignoredFile;
  private Long nodeCreated;
  private Long relationshipCreated;

  // Binding map between csv ID and Neo4j created nodes. Only the Node id is stored here, to limit
  // the usage of heap memory.
  private Map<Long, Long> idBindingMap;
  private final Neo4jAL neo4jAL;

  public Importer(Neo4jAL neo4jAL) {
    this.neo4jAL = neo4jAL;

    // Init members
    this.countLabelCreated = 0L;
    this.countRelationTypeCreated = 0L;
    this.ignoredFile = 0L;
    this.nodeCreated = 0L;
    this.relationshipCreated = 0L;
    this.idBindingMap = new HashMap<>();
  }

  public Stream<String> load(Path pathToZipFileName) throws ProcedureException {
    MESSAGE_QUEUE.clear();

    try {
      File zipFile = pathToZipFileName.toFile();

      // End the procedure if the path specified isn't valid
      if (!zipFile.exists()) {
        MESSAGE_QUEUE.add(
            "No zip file found at path "
                .concat(pathToZipFileName.toString())
                .concat(". Please check the path provided"));
        return MESSAGE_QUEUE.stream();
      }

      parseZip(zipFile);

    } catch (IOException | Neo4jQueryException e) {
      throw new ProcedureException(e);
    }

    MESSAGE_QUEUE.add(
        String.format(
            "%d file(s) containing a label where found and processed.", countLabelCreated));
    MESSAGE_QUEUE.add(
        String.format(
            "%d file(s) containing relationships where found and processed.",
            countRelationTypeCreated));
    MESSAGE_QUEUE.add(
        String.format("%d file(s) where ignored. Check logs for more information.", ignoredFile));
    MESSAGE_QUEUE.add(
        String.format(
            "%d node(s) and %d relationship(s) were created during the import.",
            nodeCreated, relationshipCreated));

    return MESSAGE_QUEUE.stream();
  }

  /**
   * Parse all files within zip file. For each file a BufferedReader will be open and stored as a
   * Node BufferReader or a Relationship BufferReader. The procedure will use the prefix in the
   * filename to decide if it must be treated as a file containing node or relationships.
   *
   * @param file The Zip file to be treated
   * @throws IOException
   */
  private void parseZip(File file) throws IOException, Neo4jQueryException {
    Map<BufferedReader, String> nodeBuffers = new HashMap<>();
    Map<BufferedReader, String> relBuffers = new HashMap<>();

    try (ZipFile zf = new ZipFile(file)) {
      Enumeration entries = zf.entries();

      while (entries.hasMoreElements()) {
        ZipEntry ze = (ZipEntry) entries.nextElement();
        String filename = ze.getName();
        neo4jAL.logInfo(String.format("File discovered in ZIP is being processed %s", filename));

        if (ze.getSize() < 0) continue; // Empty entry

        try {
          BufferedReader br = new BufferedReader(new InputStreamReader(zf.getInputStream(ze)));

          if (filename.startsWith(RELATIONSHIP_PREFIX)) {
            relBuffers.put(br, filename);
          } else if (filename.startsWith(NODE_PREFIX)) {
            nodeBuffers.put(br, filename);
          } else {
            ignoredFile++;
            neo4jAL.logError(
                String.format("Unrecognized file with name '%s' in zip file. Skipped.", filename));
          }
        } catch (Exception e) {
          neo4jAL.logError(
              "An error occurred trying to process entry with file name ".concat(filename), e);
          neo4jAL.logError("This entry will be skipped");
        }
      }

      // Treat nodes in a first time, to fill the idBindingMap for relationships
      for (Map.Entry<BufferedReader, String> pair : nodeBuffers.entrySet()) {
        try {
          String labelAsString = getLabelFromFilename(pair.getValue());
          treatNodeBuffer(labelAsString, pair.getKey());
          countLabelCreated++;
        } catch (FileCorruptedException e) {
          this.neo4jAL.logError("The file".concat(pair.getValue()).concat(" seems to be corrupted. Skipped."));
          ignoredFile++;
        }
      }

      // Treat Relationships buffer
      for (Map.Entry<BufferedReader, String> pair : relBuffers.entrySet()) {
        try {
          String relAsString = getLabelFromFilename(pair.getValue());
          treatRelBuffer(relAsString, pair.getKey());
          countRelationTypeCreated++;
        } catch (FileCorruptedException e) {
          neo4jAL.logError("The file".concat(pair.getValue()).concat(" seems to be corrupted. Skipped."));
          ignoredFile++;
        } catch (Neo4jQueryException e) {
          neo4jAL.logError("Operation failed, check the stack trace for more information.");
          throw e;
        }
      }

    } finally {
      // Close bufferedReader
      for (BufferedReader bf : nodeBuffers.keySet()) bf.close();
      for (BufferedReader bf : relBuffers.keySet()) bf.close();
    }
  }

  /**
   * Get the label stored within the filename by removing the prefix and the extension
   *
   * @param filename
   * @return
   */
  private String getLabelFromFilename(String filename) {
    return filename
        .replace(RELATIONSHIP_PREFIX, "")
        .replace(NODE_PREFIX, "")
        .replace(EXTENSION, "");
  }

  /**
   * Treat a node buffer by extracting the first row as a list of header value. Treat all the other
   * rows as list of node's values.
   *
   * @param associatedLabel Name of the label
   * @param nodeFileBuf BufferReader pointing to the node file
   * @throws IOException thrown if the procedure fails to read the buffer
   * @throws FileCorruptedException thrown if the file isn't in a good format ( If the headers are
   *     missing, or if it does not contains any Index Column)
   */
  private void treatNodeBuffer(String associatedLabel, BufferedReader nodeFileBuf)
      throws IOException, FileCorruptedException {
    String line;
    String headers = nodeFileBuf.readLine();
    if (headers == null)
      throw new FileCorruptedException("No header found in file.", "LOADxTNBU01");

    Label label = Label.label(associatedLabel);

    // Process header line
    List<String> headerList = sanitizeCSVInput(headers);
    if (!headerList.contains(INDEX_COL))
      throw new FileCorruptedException("No index column found in file.", "LOADxTNBU02");

    while ((line = nodeFileBuf.readLine()) != null) {
      List<String> values = sanitizeCSVInput(line);
      try {
        createNode(label, headerList, values);
      } catch (Exception | Neo4jQueryException e) {
        neo4jAL.logError(
            "An error occurred during creation of node with label : "
                .concat(associatedLabel)
                .concat(" and values : ")
                .concat(String.join(DELIMITER, values)),
            e);
      }
    }
  }

  /**
   * Treat a relationship buffer by extracting the first row as a list of header value. Treat all
   * the other rows as list of relationship's values.
   *
   * @param associatedRelation Name of the relationship
   * @param relFileBuf BufferReader pointing to the relationship file
   * @throws IOException thrown if the procedure fails to read the buffer
   * @throws FileCorruptedException thrown if the file isn't in a good format ( If the headers are
   *     missing, or if it does not contains any Source or Destination index column)
   */
  private void treatRelBuffer(String associatedRelation, BufferedReader relFileBuf)
      throws IOException, FileCorruptedException, Neo4jQueryException {
    String line;
    String headers = relFileBuf.readLine();
    if (headers == null)
      throw new FileCorruptedException("No header found in file.", "LOADxTNBU01");

    RelationshipType relName = RelationshipType.withName(associatedRelation);

    List<String> headerList = sanitizeCSVInput(headers);
    if (!headerList.contains(INDEX_OUTGOING) || !headerList.contains(INDEX_INCOMING))
      throw new FileCorruptedException(
          "Corrupted header (missing source or destination columns).", "LOADxTNBU02");

    while ((line = relFileBuf.readLine()) != null) {
      List<String> values = sanitizeCSVInput(line);
      createRelationship(relName, headerList, values);
    }
  }

  /**
   * Remove EOL token and split the row to List<String>
   *
   * @param input CVS row to sanitize
   * @return Sanitized string
   */
  private List<String> sanitizeCSVInput(String input) {
    // Split using delimiters. Ignore delimiter surrounded by quotations marks.
    return Arrays.asList(
        input
            .replaceAll("\\\\r\\\\n", "")
            .split(DELIMITER + "(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)"));
  }

  /**
   * Create a Node based on provided header and values. If a value is empty, it will not be added as
   * a property to the node. To make the com.castsoftware.exporter more generic, the conversion from
   * CSV values to Java Values does not necessitate POJOs object. However, the drawback is that this
   * conversion can create some errors. @see Loader.getNeo4jType() for more information.
   *
   * @param label Label that will be give to the node
   * @param headers Headers as a list of String
   * @param values Values as a list of String
   */
  private void createNode(Label label, List<String> headers, List<String> values)
      throws Neo4jQueryException {

    // Skip empty rows
    if(values.size() == 0) return;

    int indexCol = headers.indexOf(INDEX_COL);
    Long id = Long.parseLong(values.get(indexCol));

    // Apply a filter on unique properties
    List<String> uniqueProperties = new ArrayList<>();
    if(mapUniqueProperty.containsKey(label.name())) {
      uniqueProperties = mapUniqueProperty.get(label.name());
      neo4jAL.logInfo(
          String.format(
              "Node with label %s will be filtered on %s",
              label.name(), String.join(", ", uniqueProperties)));
    }

    try {
      // Check if the node already exist
      int minSize = Math.min(values.size(), headers.size());


      // If the node has a unique property identifier verify that it doesn't already exists
      if (!uniqueProperties.isEmpty()) {

        var wrapperParameters = new Object(){ Map<String, Object> parameters = new HashMap<>(); };
        final Integer[] paramPosition = {1};

        String filter = uniqueProperties.stream()
                .map(x -> {
                  if(!headers.contains(x)) return "";
                  String val = values.get(headers.indexOf(x));

                  String toReq = String.format("o.%s=$val%d", x, paramPosition[0]);

                  wrapperParameters.parameters.put(String.format("val%d", paramPosition[0]), val);
                  paramPosition[0]++;

                  return toReq; })
                .filter(item-> !item.isEmpty()).collect(Collectors.joining(" AND "));


        if(!filter.isBlank()) {
          String req =
                  String.format("MATCH (o:%s) WHERE %s return o", label.name(), filter);
          neo4jAL.logInfo("Merging query : " + req);
          Result res = this.neo4jAL.executeQuery(req, wrapperParameters.parameters);

          // If the request match a node, skip the upload of a similar node
          if (res.hasNext()) {
            return;
          }
        }

      }

      // No node with similar name was detected, insert a new one
      Node n = this.neo4jAL.createNode(label);
      for (int i = 0; i < minSize; i++) {
        if (i == indexCol || values.get(i).isEmpty()) continue; // Index col or empty value
        Object extractedVal = getNeo4jType(values.get(i));

        n.setProperty(headers.get(i), extractedVal);
      }

      nodeCreated++;
      idBindingMap.put(
          id, n.getId()); // We need to keep a track of the csv id to bind node together later
    } catch (Exception e) {
      throw new Neo4jQueryException("Node creation failed.", e, "IMPOxCREN01");
    }
  }

  /**
   * Create a relationship between two node. Source node ID and Destination node must be specified
   * in the header and the value list. If one of these information is missing the relationship will
   * be ignored.
   *
   * @param relationshipType The name of the relationship
   * @param headers List containing the value of the header
   * @param values List containing the value of the relationship
   */
  private void createRelationship(
      RelationshipType relationshipType, List<String> headers, List<String> values)
      throws Neo4jQueryException {
    int indexOutgoing = headers.indexOf(INDEX_OUTGOING);
    Long idOutgoing = Long.parseLong(values.get(indexOutgoing));

    int indexIncoming = headers.indexOf(INDEX_INCOMING);
    Long idIncoming = Long.parseLong(values.get(indexIncoming));

    Long srcNodeId = idBindingMap.get(idOutgoing);
    Long destNodeId = idBindingMap.get(idIncoming);

    if (srcNodeId == null || destNodeId == null)
      return; // Ignore this relationship, at least one node is missing

    Node srcNode = null;
    Node destNode = null;

    try {
      srcNode = this.neo4jAL.getNodeById(srcNodeId);
      destNode = this.neo4jAL.getNodeById(destNodeId);
    } catch (Exception e) {
      throw new Neo4jQueryException("Impossible to retrieve Dest/Src Node.", e, "IMPOxCRER01");
    }

    Relationship rel = srcNode.createRelationshipTo(destNode, relationshipType);

    int minSize = Math.min(values.size(), headers.size());
    for (int i = 0; i < minSize; i++) {
      if (i == indexOutgoing || i == indexIncoming || values.get(i).isEmpty())
        continue; // Index col or empty value
      Object extractedVal = getNeo4jType(values.get(i));
      rel.setProperty(headers.get(i), extractedVal);
    }

    relationshipCreated++;
  }

  /**
   * Convert a String containing a Neo4j Type to a Java Type Handled type are : Boolean, Char, Byte,
   * Short, Long, Double, LocalDate, OffsetTime, LocalTime, ZoneDateTime. If none of these types are
   * detected, it will return the value as a String. <u>Warning :</u> TemporalAmount and
   * org.neo4j.graphdb.spatial.Point are not detected Check
   * https://neo4j.com/docs/java-reference/current/java-embedded/property-values/index.html for more
   * informations <u>Warning :</u> The goal of this function is to reassign the correct Java type to
   * the value discovered in the CSV. It mays detect the wrong type.
   *
   * @param value Neo4j Value as a string
   * @return Object of the Java Type associated to the discovered type within the string provided
   */
  private Object getNeo4jType(String value) {

    // Remove Sanitization
    value =  value.strip().replaceAll("^\"+|\"+$", "");

    // Long
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ignored) {
    }

    // Double
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException ignored) {
    }

    // Integer : Return Long value
    try {
      return ((Integer) Integer.parseInt(value)).longValue();
    } catch (NumberFormatException ignored) {
    }

    // Byte
    try {
      return Byte.parseByte(value);
    } catch (NumberFormatException ignored) {
    }
    // Short
    try {
      return Short.parseShort(value);
    } catch (NumberFormatException ignored) {
    }

    // Boolean
    if (value.toLowerCase().matches("true|false")) {
      return Boolean.parseBoolean(value);
    }

    // DateTimeFormatter covering all Neo4J Date Format  (cf :
    // https://neo4j.com/docs/cypher-manual/current/syntax/temporal/ )
    DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern(
            "[yyyy-MM-dd]"
                + "[yyyy-MM-dd hh:mm:ss]"
                + "[yyyyMMdd]"
                + "[yyyy-MM]"
                + "[yyyyMM]"
                + "[yyyy-Www-D]"
                + "[yyyy- W ww]"
                + "[yyyy W ww]"
                + "[yyyy- Q q-DD]"
                + "[yyyy Q q]"
                + "[yyyy-DDD]"
                + "[yyyyDDD]"
                + "[yyyy]");

    // LocalDate
    try {
      return LocalDate.parse(value, formatter);
    } catch (DateTimeParseException ignored) {
    }
    // OffsetTime
    try {
      return OffsetTime.parse(value, formatter);
    } catch (DateTimeParseException ignored) {
    }
    // LocalTime
    try {
      return LocalTime.parse(value, formatter);
    } catch (DateTimeParseException ignored) {
    }
    // ZoneDateTime
    try {
      return ZonedDateTime.parse(value, formatter);
    } catch (DateTimeParseException ignored) {
    }

    // Array list
    try {
      if (value.matches("^\\[([\\w\\s]*,?)+\\]")) {
        // Remove brackets
        return value.replaceAll("[|]", "").strip().split(",");
      }
    } catch (Exception ignored) {
    }

    // Char
    if (value.length() == 1) return value.charAt(0);

    // String
    return value;
  }
}
