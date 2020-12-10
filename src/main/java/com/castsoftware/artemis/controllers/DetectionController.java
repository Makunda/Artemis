package com.castsoftware.artemis.controllers;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.DatasetManager;
import com.castsoftware.artemis.datasets.FrameworkBean;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.exceptions.dataset.InvalidDatasetException;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.exceptions.nlp.NLPIncorrectConfigurationException;
import com.castsoftware.artemis.nlp.model.NLPEngine;
import com.castsoftware.artemis.nlp.model.NLPCategory;
import com.castsoftware.artemis.nlp.model.NLPConfidence;
import com.castsoftware.artemis.nlp.model.NLPResults;
import com.castsoftware.artemis.nlp.parser.GoogleParser;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class DetectionController {

    public static final String ARTEMIS_SEARCH_PREFIX = Configuration.get("artemis.tag.prefix_search");
    public static final String IMAGING_OBJECT_LABEL = Configuration.get("imaging.node.object.label");
    public static final String IMAGING_OBJECT_TAGS = Configuration.get("imaging.link.object_property.tags");
    public static final String IMAGING_OBJECT_NAME = Configuration.get("imaging.node.object.name");

    /**
     * Save NLP Results to the Artemis Database. The target database will be decided depending on the value of
     * @param dtManager Database Manager
     * @param name Name of the object to save
     * @param results Results of the NLP Engine
     * @throws InvalidDatasetException
     */
    private static void saveNLPResult(DatasetManager dtManager, String name, NLPResults results) throws InvalidDatasetException {
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
        String strDate = dateFormat.format(date);

        FrameworkType fType = null;

        if(results.getConfidence() == NLPConfidence.NOT_CONFIDENT) { // If the confidence score is not high enough it
            // will be added on the to investigate dt
            fType = FrameworkType.TO_INVESTIGATE;
        } else if(results.getCategory() == NLPCategory.FRAMEWORK) { // Detected as a framework
            fType = FrameworkType.FRAMEWORK;
        } else { // Detected as a not framework
            fType = FrameworkType.NOT_FRAMEWORK;
        }

        FrameworkBean fb = new FrameworkBean(name, strDate, "No location discovered", "", 1);
        dtManager.addInput(fType, fb);
    }

    /**
     * Get the list of detected framework inside the provided list of node
     * @param neo4jAL Neo4j Access Layer
     * @param toInvestigateNodes List of node that will be investigated
     * @param language Language of the application
     * @return The list of detected framework
     * @throws IOException
     * @throws MissingFileException A file in missing in the Artemis workspace
     * @throws NLPIncorrectConfigurationException The NLP engine failed to start due to a bad configuration
     */
    private static List<String> getFrameworkList(Neo4jAL neo4jAL, List<Node> toInvestigateNodes, String language)
            throws IOException, MissingFileException, NLPIncorrectConfigurationException {
        DatasetManager dtManager = DatasetManager.getManager();

        GoogleParser gp = new GoogleParser();

        // Make sure the nlp as trained, train it otherwise
        NLPEngine nlpEngine = new NLPEngine(neo4jAL.getLogger());
        if(!nlpEngine.checkIfModelExists()) {
            nlpEngine.train();
        }

        List<String> frameworkList = new ArrayList<>();

        // Extract  object's name
        for(Node n : toInvestigateNodes) {
            // Ignore object without a name property
            if(!n.hasProperty(IMAGING_OBJECT_NAME)) continue;
            String objectName = (String) n.getProperty(IMAGING_OBJECT_NAME);

            try {
                // Check if the framework is already known
                FrameworkType fmt = dtManager.searchForEntry(objectName);

                // If the Framework is not known, launch the NLP Detection
                if(fmt == FrameworkType.NOT_KNOWN) {
                    String requestResult = gp.request(objectName);
                    NLPResults nlpResult = nlpEngine.getNLPResult(requestResult);
                    saveNLPResult(dtManager, objectName, nlpResult);
                } else {
                    neo4jAL.logInfo(String.format("The object with name '%s' is already known by Artemis as a '%s'.", objectName, fmt.toString()));
                }

            } catch (Exception | InvalidDatasetException e) {
                String message = String.format("The object with name '%s' produced an error during execution.", objectName);
                neo4jAL.logError(message, e);
            }


        }

        return frameworkList;
    }

    /**
     * Launch the Artemis Detection against the specified application
     * @param neo4jAL Neo4J Access Layer
     * @param applicationContext Application used during the detection
     * @param language Specify the language of the application to pick the correct dt
     * @return The list of detected frameworks
     * @throws Neo4jQueryException
     * @throws IOException
     */
    public static List<String> launchDetection(Neo4jAL neo4jAL, String applicationContext, String language)
            throws Neo4jQueryException, IOException, MissingFileException, NLPIncorrectConfigurationException {
        neo4jAL.logInfo("Starting Demeter level 5 grouping...");

        // Get the list of nodes prefixed by dm_tag
        String forgedTagRequest = String.format("MATCH (o:%1$s:%2$s) WHERE any( x in o.%3$s WHERE x CONTAINS '%4$s') " +
                "RETURN o as node", IMAGING_OBJECT_LABEL, applicationContext, IMAGING_OBJECT_TAGS, ARTEMIS_SEARCH_PREFIX);

        Result res = neo4jAL.executeQuery(forgedTagRequest);

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

        List<String> frameworkList = getFrameworkList(neo4jAL, toInvestigateNodes, language);

        neo4jAL.logInfo("Cleaning Artemis tags...");
        // Once the operation is done, remove Demeter tag prefix tags
        String removeTagsQuery = String.format("MATCH (o:%1$s) WHERE EXISTS(o.%2$s)  SET o.%2$s = [ x IN o.%2$s WHERE NOT x CONTAINS '%3$s' ] RETURN COUNT(o) as removedTags;",
                applicationContext, IMAGING_OBJECT_TAGS, ARTEMIS_SEARCH_PREFIX);
        Result tagRemoveRes = neo4jAL.executeQuery(removeTagsQuery);

        if(tagRemoveRes.hasNext()) {
            Long nDel = (Long) tagRemoveRes.next().get("removedTags");
            neo4jAL.logInfo( "# " + nDel + " artemis 'search tags' were removed from the database.");
        }

        neo4jAL.logInfo("Cleaning Done !");

        return frameworkList;
    }

    /**
     * Train the NLP engine of Artemis
     * @param neo4jAL Neo4j Access Layer
     * @throws IOException
     * @throws NLPIncorrectConfigurationException
     */
    public static void trainArtemis(Neo4jAL neo4jAL) throws IOException, NLPIncorrectConfigurationException {
        NLPEngine nlpEngine = new NLPEngine(neo4jAL.getLogger());
        nlpEngine.train();
    }
}
