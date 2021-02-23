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

package com.castsoftware.artemis.controllers;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.config.detection.LanguageConfiguration;
import com.castsoftware.artemis.config.detection.LanguageProp;
import com.castsoftware.artemis.controllers.api.BreakdownController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.nlp.SupportedLanguage;
import com.castsoftware.artemis.results.InteractionResult;
import com.castsoftware.artemis.results.LeafResult;
import com.castsoftware.artemis.results.OutputMessage;
import com.castsoftware.artemis.sof.famililes.FamiliesFinder;
import com.castsoftware.artemis.sof.famililes.FamilyGroup;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InteractionsController {

  public static final String ARTEMIS_SEARCH_PREFIX = Configuration.get("artemis.tag.prefix_search");
  public static final String IMAGING_OBJECT_LABEL = Configuration.get("imaging.node.object.label");
  public static final String IMAGING_OBJECT_TAGS =
      Configuration.get("imaging.link.object_property.tags");
  public static final String IMAGING_OBJECT_NAME = Configuration.get("imaging.node.object.name");
  public static final String IMAGING_APPLICATION_LABEL =
      Configuration.get("imaging.application.label");

  @Deprecated
  public static List<OutputMessage> launchDetection(
      Neo4jAL neo4jAL, String applicationContext, String language, Boolean flagNodes)
      throws Neo4jQueryException {

    // Get language
    SupportedLanguage sLanguage = SupportedLanguage.getLanguage(language);
    neo4jAL.logInfo(
        String.format(
            "Starting Artemis interaction detection on language '%s'...", sLanguage.toString()));

    // Get the list of nodes prefixed by dm_tag
    // String forgedTagRequest = String.format("MATCH (o:%1$s:%2$s) WHERE any( x in o.%3$s WHERE x
    // CONTAINS '%4$s') " +
    //        "RETURN o as node", IMAGING_OBJECT_LABEL, applicationContext, IMAGING_OBJECT_TAGS,
    // ARTEMIS_SEARCH_PREFIX);

    String forgedRequest =
        String.format(
            "MATCH (obj:%s:%s) WHERE  obj.Type CONTAINS '%s' AND obj.External=true RETURN obj as node",
            IMAGING_OBJECT_LABEL, applicationContext, sLanguage.toString());

    Result res = neo4jAL.executeQuery(forgedRequest);

    Instant start = Instant.now();

    // TODO extract this to a new logic layer
    // Build the map for each group as <Tag, Node list>
    List<Node> toInvestigateNodes = new ArrayList<>();
    while (res.hasNext()) {
      Map<String, Object> resMap = res.next();
      Node node = (Node) resMap.get("node");
      toInvestigateNodes.add(node);
    }

    Instant finish = Instant.now();
    neo4jAL.logInfo(
        String.format(
            "%d nodes were identified in %d Milliseconds.",
            toInvestigateNodes.size(), Duration.between(start, finish).toMillis()));

    FamiliesFinder ff = new FamiliesFinder(neo4jAL, toInvestigateNodes);
    List<FamilyGroup> resultList = ff.findFamilies(); // Logic of grouping

    // If the flag option is set, apply demeter tag on the objects
    for (FamilyGroup fg : resultList) {
      for (Node n : fg.getNodeList()) {
        UtilsController.applyDemeterParentTag(neo4jAL, n, fg.getCommonPrefix());
      }
    }

    neo4jAL.logInfo("Interaction detector done !");

    return resultList.stream()
        .map(
            x ->
                new OutputMessage(
                    "Name : " + x.getCommonPrefix() + " . Number of match : " + x.getFamilySize()))
        .collect(Collectors.toList());
  }

  /**
   * Get the interaction between
   *
   * @param neo4jAL
   * @param application
   * @param appToSearch
   * @param language
   * @param fullNameRegex
   * @return
   * @throws Neo4jQueryException
   */
  public static List<InteractionResult> getInteractionsTree(
      Neo4jAL neo4jAL,
      String application,
      List<String> appToSearch,
      String language,
      String fullNameRegex)
          throws Neo4jQueryException, IOException, MissingFileException {
    LanguageConfiguration lc = LanguageConfiguration.getInstance();
    if (!lc.checkLanguageExistence(language)) return new ArrayList<>(); // Return  empty list

    // Get the breakdown of the application
    List<LeafResult> flattenTree = BreakdownController.getBreakDown(neo4jAL, application, language);

    // Parse the tree and assign applications matching the leaves

    String req =
        "MATCH (p:Object) WHERE any( x in LABELS(p) WHERE x=~$appListRegex ) "
            + "AND p.InternalType IN $internalTypes AND p.FullName=~$toSearch "
            + "RETURN DISTINCT p.FullName as fullName, COLLECT([ x in LABELS(p) WHERE NOT x='Object' ][0]) as applications;";

    List<InteractionResult> interactionResultList = new ArrayList<>();

    for (LeafResult leaf : flattenTree) {
      interactionResultList.addAll(getInteraction(neo4jAL, appToSearch, language, leaf.name));
    }

    return interactionResultList;
  }

  /**
   * Get the interaction with others applications along with the number of object matching it, and
   * the externality of the objects
   *
   * @param neo4jAL Neo4J Access Layer
   * @param applications List of application to search in
   * @param language Language used for the internal type
   * @param toSearchRegex Regex to search
   * @return
   * @throws Neo4jQueryException
   */
  public static List<InteractionResult> getInteraction(
      Neo4jAL neo4jAL, List<String> applications, String language, String toSearchRegex)
      throws Neo4jQueryException {
    LanguageConfiguration lc = LanguageConfiguration.getInstance();
    if (!lc.checkLanguageExistence(language)) return new ArrayList<>(); // Return  empty list

    LanguageProp lp = LanguageConfiguration.getInstance().getLanguageProperties(language);

    String req =
        "MATCH (p:Object) WHERE any( x IN LABELS(p) WHERE x=~$appListRegex ) AND p.InternalType IN $internalTypes AND p.FullName=~$toSearch "
            + "RETURN DISTINCT [ x in LABELS(p) WHERE NOT x='Object' ][0] as application, "
            + "COUNT(p) as match, "
            + "CASE WHEN all(x in COLLECT(p) WHERE x.External=True) THEN 'External' WHEN all(x in COLLECT(p) WHERE x.External=FALSE) THEN 'Internal' ELSE 'External/Internal' END as Externality";

    List<InteractionResult> interactionResultList = new ArrayList<>();
    String appListRegex = String.join("|", applications);
    Map<String, Object> params =
        Map.of(
            "appListRegex",
            appListRegex,
            "internalTypes",
            lp.getObjectsInternalType(),
            "toSearch",
            toSearchRegex);

    Result res = neo4jAL.executeQuery(req, params);
    while (res.hasNext()) {
      Map<String, Object> record = res.next();
      interactionResultList.add(
          new InteractionResult(
              toSearchRegex,
              (String) record.get("application"),
              (Long) record.get("match"),
              (String) record.get("Externality")));
    }

    return interactionResultList;
  }
}
