package com.castsoftware.artemis.controllers;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.exceptions.google.GoogleBadResponseCodeException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.exceptions.nlp.NLPIncorrectConfigurationException;
import com.castsoftware.artemis.interactions.famililes.FamiliesFinder;
import com.castsoftware.artemis.interactions.famililes.FamilyGroup;
import com.castsoftware.artemis.nlp.SupportedLanguage;
import com.castsoftware.artemis.results.FrameworkResult;
import com.castsoftware.artemis.results.OutputMessage;
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
    public static final String IMAGING_OBJECT_TAGS = Configuration.get("imaging.link.object_property.tags");
    public static final String IMAGING_OBJECT_NAME = Configuration.get("imaging.node.object.name");
    public static final String IMAGING_APPLICATION_LABEL = Configuration.get("imaging.application.label");


    public static List<OutputMessage> launchDetection(Neo4jAL neo4jAL, String applicationContext, String language, Boolean flagNodes)
            throws Neo4jQueryException {


        // Get language
        SupportedLanguage sLanguage = SupportedLanguage.getLanguage(language);
        neo4jAL.logInfo(String.format("Starting Artemis interaction detection on language '%s'...", sLanguage.toString()));

        // Get the list of nodes prefixed by dm_tag
        //String forgedTagRequest = String.format("MATCH (o:%1$s:%2$s) WHERE any( x in o.%3$s WHERE x CONTAINS '%4$s') " +
        //        "RETURN o as node", IMAGING_OBJECT_LABEL, applicationContext, IMAGING_OBJECT_TAGS, ARTEMIS_SEARCH_PREFIX);

        String forgedRequest = String.format("MATCH (obj:%s:%s) WHERE  obj.Type CONTAINS '%s' AND obj.External=true RETURN obj as node",
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
        neo4jAL.logInfo(String.format("%d nodes were identified in %d Milliseconds.", toInvestigateNodes.size(),
                Duration.between(start, finish).toMillis()));

        FamiliesFinder ff = new FamiliesFinder(neo4jAL, toInvestigateNodes);
        List<FamilyGroup> resultList = ff.findFamilies(); // Logic of grouping

        // If the flag option is set, apply demeter tag on the objects
        for( FamilyGroup fg : resultList) {
            for(Node n : fg.getNodeList()) {
                UtilsController.applyDemeterParentTag(neo4jAL, n, fg.getCommonPrefix());
            }
        }

        neo4jAL.logInfo("Interaction detector done !");

        return resultList.stream().map(x -> new OutputMessage("Name : "+x.getCommonPrefix() + " . Number of match : "+x.getFamilySize())).collect(Collectors.toList());
    }


}
