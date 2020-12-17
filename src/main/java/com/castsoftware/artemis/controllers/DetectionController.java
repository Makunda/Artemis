package com.castsoftware.artemis.controllers;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.exceptions.dataset.InvalidDatasetException;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.exceptions.google.GoogleBadResponseCodeException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.exceptions.nlp.NLPBlankInputException;
import com.castsoftware.artemis.exceptions.nlp.NLPIncorrectConfigurationException;
import com.castsoftware.artemis.interactions.famililes.FamiliesFinder;
import com.castsoftware.artemis.interactions.famililes.FamilyGroup;
import com.castsoftware.artemis.nlp.SupportedLanguage;
import com.castsoftware.artemis.nlp.model.NLPEngine;
import com.castsoftware.artemis.nlp.model.NLPCategory;
import com.castsoftware.artemis.nlp.model.NLPConfidence;
import com.castsoftware.artemis.nlp.model.NLPResults;
import com.castsoftware.artemis.nlp.parser.GoogleParser;
import com.castsoftware.artemis.reports.ReportGenerator;
import com.castsoftware.artemis.results.FrameworkResult;
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
    public static final String IMAGING_APPLICATION_LABEL = Configuration.get("imaging.application.label");

    /**
     * Save NLP Results to the Artemis Database. The target database will be decided depending on the value of
     *
     * @param neo4jAL Neo4j access layer
     * @param name    Name of the object to save
     * @param results Results of the NLP Engine
     * @throws InvalidDatasetException
     */
    private static FrameworkNode saveNLPResult(Neo4jAL neo4jAL, ReportGenerator rg, String name, NLPResults results) throws InvalidDatasetException, Neo4jQueryException {
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
        String strDate = dateFormat.format(date);

        FrameworkType fType = null;

        if (results.getConfidence() == NLPConfidence.NOT_CONFIDENT) { // If the confidence score is not high enough it
            // will be added on the to investigate dt
            fType = FrameworkType.TO_INVESTIGATE;
        } else if (results.getCategory() == NLPCategory.FRAMEWORK) { // Detected as a framework
            fType = FrameworkType.FRAMEWORK;
        } else { // Detected as a not framework
            fType = FrameworkType.NOT_FRAMEWORK;
        }

        // Retrieve highest detection score
        double detectionScore = 0.0;
        double[] prob = results.getProbabilities();
        for (int i = 0; i < prob.length; i++) {
            if (prob[i] > detectionScore) detectionScore = prob[i];
        }

        FrameworkNode fb = new FrameworkNode(neo4jAL, name, strDate, "No location discovered", "", 1L, detectionScore);
        fb.setFrameworkType(fType);
        fb.createNode();

        rg.addFrameworkBean(fb);

        return fb;
    }

    public static void getInternalFramework(Neo4jAL neo4jAL, List<Node> candidates) throws Neo4jQueryException {

        FamiliesFinder finder = new FamiliesFinder(neo4jAL, candidates);
        List<FamilyGroup> fg = finder.findFamilies();

        for(FamilyGroup f : fg) {
            neo4jAL.logInfo(String.format("Found a %d objects family with prefix '%s'.", f.getFamilySize(), f.getCommonPrefix()));
            f.addDemeterTag(neo4jAL);
        }

    }

    /**
     * Get the list of detected framework inside the provided list of node
     *
     * @param neo4jAL            Neo4j Access Layer
     * @param toInvestigateNodes List of node that will be investigated
     * @return The list of detected framework
     * @throws IOException
     * @throws MissingFileException               A file in missing in the Artemis workspace
     * @throws NLPIncorrectConfigurationException The NLP engine failed to start due to a bad configuration
     */
    private static List<FrameworkNode> getFrameworkList(Neo4jAL neo4jAL, List<Node> toInvestigateNodes,
                                                        String reportName, SupportedLanguage language, Boolean flagNodes)
            throws IOException, Neo4jQueryException {
        int numTreated = 0;
        boolean onlineMode = Boolean.parseBoolean(Configuration.get("artemis.onlineMode"));

        GoogleParser gp = new GoogleParser(neo4jAL.getLogger());
        ReportGenerator rg = new ReportGenerator(reportName);

        //Shuffle nodes to avoid google bot busting
        Collections.shuffle(toInvestigateNodes);

        // Make sure the nlp as trained, train it otherwise
        NLPEngine nlpEngine = new NLPEngine(neo4jAL.getLogger(), language);
        if (!nlpEngine.checkIfModelExists()) {
            nlpEngine.train();
        }

        List<FrameworkNode> frameworkList = new ArrayList<>();
        List<Node> notDetected = new ArrayList<>();

        // Extract  object's name
        for (Node n : toInvestigateNodes) {
            // Ignore object without a name property
            if (!n.hasProperty(IMAGING_OBJECT_NAME)) continue;
            String objectName = (String) n.getProperty(IMAGING_OBJECT_NAME);

            try {
                // Check if the framework is already known
                FrameworkNode fb = FrameworkNode.findFrameworkByName(neo4jAL, objectName);

                // If the Framework is not known and the connection to google still possible, launch the NLP Detection
                if(fb != null) {
                    neo4jAL.logInfo(String.format("The object with name '%s' is already known by Artemis as a '%s'.", objectName, fb.getFrameworkType()));
                }

                if (fb == null && gp != null && onlineMode) {
                    String requestResult = gp.request(objectName);
                    neo4jAL.logInfo("Content of the google query : " + requestResult);
                    NLPResults nlpResult = nlpEngine.getNLPResult(requestResult);
                    fb = saveNLPResult(neo4jAL, rg, objectName, nlpResult);
                }


                // Add the framework to the list of it was detected
                if(fb != null) {
                    // If flag option is set, apply a demeter tag to the nodes considered as framework
                    if(flagNodes && (fb.getFrameworkType() == FrameworkType.FRAMEWORK))  {
                        UtilsController.applyDemeterParentTag(neo4jAL, n, " external");
                    } else {
                        notDetected.add(n);
                    }

                    // Increment the number of detection and add it to the result lists
                    fb.incrementNumberDetection();
                    frameworkList.add(fb);
                }

                numTreated++;

                if (numTreated % 100 == 0) {
                    neo4jAL.logInfo(String.format("Investigation on going. Treating node %d/%d.", numTreated, toInvestigateNodes.size()));
                }

            } catch (Exception | InvalidDatasetException | NLPBlankInputException | Neo4jQueryException | Neo4jBadNodeFormatException e) {
                String message = String.format("The object with name '%s' produced an error during execution.", objectName);
                neo4jAL.logError(message, e);
            } catch (GoogleBadResponseCodeException e) {
                neo4jAL.logError("Fatal error, the communication with Google API was refused.", e);
                gp = null; // remove the google parser
                rg.generate(); // generate the report
            }
        }

        // Launch internal framework detector on remaining nodes
        getInternalFramework(neo4jAL, notDetected);

        // Generate the report
        rg.generate();
        return frameworkList;
    }


    /**
     * Launch the Artemis Detection against the specified application
     *
     * @param neo4jAL            Neo4J Access Layer
     * @param applicationContext Application used during the detection
     * @param language           Specify the language of the application to pick the correct dt
     * @return The list of detected frameworks
     * @throws Neo4jQueryException
     * @throws IOException
     */
    public static List<FrameworkResult> launchDetection(Neo4jAL neo4jAL, String applicationContext, String language, Boolean flagNodes)
            throws Neo4jQueryException, IOException, MissingFileException, NLPIncorrectConfigurationException, GoogleBadResponseCodeException {


        // Get language
        SupportedLanguage sLanguage = SupportedLanguage.getLanguage(language);
        neo4jAL.logInfo(String.format("Starting Artemis detection on language '%s'...", sLanguage.toString()));

        // Get the list of nodes prefixed by dm_tag
        //String forgedTagRequest = String.format("MATCH (o:%1$s:%2$s) WHERE any( x in o.%3$s WHERE x CONTAINS '%4$s') " +
        //        "RETURN o as node", IMAGING_OBJECT_LABEL, applicationContext, IMAGING_OBJECT_TAGS, ARTEMIS_SEARCH_PREFIX);

        String forgedRequest = String.format("MATCH (obj:%s:%s) WHERE  obj.Type CONTAINS '%s' AND obj.External=true RETURN obj as node",
                IMAGING_OBJECT_LABEL, applicationContext, language);

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

        List<FrameworkNode> frameworkList = getFrameworkList(neo4jAL, toInvestigateNodes, applicationContext, sLanguage, flagNodes);
        List<FrameworkResult> resultList = new ArrayList<>();

        for (FrameworkNode fb : frameworkList) {
            FrameworkResult fr = new FrameworkResult(fb.getName(), fb.getDescription(), fb.getFrameworkType().toString());
            resultList.add(fr);
        }

        neo4jAL.logInfo("Cleaning Artemis tags...");
        // Once the operation is done, remove Demeter tag prefix tags
        String removeTagsQuery = String.format("MATCH (o:%1$s) WHERE EXISTS(o.%2$s)  SET o.%2$s = [ x IN o.%2$s WHERE NOT x CONTAINS '%3$s' ] RETURN COUNT(o) as removedTags;",
                applicationContext, IMAGING_OBJECT_TAGS, ARTEMIS_SEARCH_PREFIX);
        Result tagRemoveRes = neo4jAL.executeQuery(removeTagsQuery);

        if (tagRemoveRes.hasNext()) {
            Long nDel = (Long) tagRemoveRes.next().get("removedTags");
            neo4jAL.logInfo("# " + nDel + " artemis 'search tags' were removed from the database.");
        }

        neo4jAL.logInfo("Cleaning Done !");

        return resultList;
    }

    /**
     * Train the NLP engine of Artemis
     * @param neo4jAL Neo4j Access Layer
     * @throws IOException
     * @throws NLPIncorrectConfigurationException
     */
    public static void trainArtemis(Neo4jAL neo4jAL) throws IOException, NLPIncorrectConfigurationException {
        NLPEngine nlpEngine = new NLPEngine(neo4jAL.getLogger(), SupportedLanguage.ALL);
        nlpEngine.train();
    }

    /**
     * Launch a detection on all application present in the database
     * @param neo4jAL Neo4j Access Layer
     * @param language Language used for the detection
     */
    public static List<FrameworkResult> launchBulkDetection(Neo4jAL neo4jAL, String language, Boolean flagNodes) throws Neo4jQueryException, MissingFileException, IOException, NLPIncorrectConfigurationException, GoogleBadResponseCodeException {
        List<FrameworkResult> resultList = new ArrayList<>();
        List<String> appNameList = new ArrayList<>();
        List<Node> toInvestigateNodes = new ArrayList<>();

        // Get language
        SupportedLanguage sLanguage = SupportedLanguage.getLanguage(language);
        neo4jAL.logInfo(String.format("Starting Artemis bulk detection on language '%s'...", sLanguage.toString()));

        String appNameRequest = String.format("MATCH (a:%1$s) WITH a.Name as appName  MATCH (obj:%2$s) WHERE appName IN LABELS(obj)  AND  obj.Type CONTAINS '%3$s' RETURN appName, COUNT(obj) as countObj;",
                IMAGING_APPLICATION_LABEL, IMAGING_OBJECT_LABEL, language);
        neo4jAL.logInfo("Request to execute : " + appNameRequest);
        Result resAppName = neo4jAL.executeQuery(appNameRequest);
        while (resAppName.hasNext()) {
            Map<String, Object> res = resAppName.next();
            String app = (String) res.get("appName");
            Long countObj = (Long) res.get("countObj");

            neo4jAL.logInfo(String.format("Application with name '%s' contains %d potential candidates.", app, countObj));

            appNameList.add(app);
        }

        for (String name : appNameList) {
            String forgedTagRequest = String.format("MATCH (obj:%1$s) WHERE  obj.Type CONTAINS '%2$s' AND obj.External=true return obj as node", name, language);
            Result res = neo4jAL.executeQuery(forgedTagRequest);

            while (res.hasNext()) {
                Node ti = (Node) res.next().get("node");
                toInvestigateNodes.add(ti);
            }

        }

        String reportName = "Bulk_" + String.join("_", appNameList);
        List<FrameworkNode> frameworkList = getFrameworkList(neo4jAL, toInvestigateNodes, reportName, sLanguage, flagNodes);

        for (FrameworkNode fb : frameworkList) {
            FrameworkResult fr = new FrameworkResult(fb.getName(), fb.getDescription(), fb.getFrameworkType().toString());
            resultList.add(fr);
        }

        return resultList;
    }
}
