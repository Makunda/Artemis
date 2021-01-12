package com.castsoftware.artemis.detector;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.config.LanguageConfiguration;
import com.castsoftware.artemis.config.LanguageProp;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.nlp.SupportedLanguage;
import com.castsoftware.artemis.nlp.model.NLPEngine;
import com.castsoftware.artemis.nlp.parser.GoogleParser;
import com.castsoftware.artemis.reports.ReportGenerator;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

public abstract class ADetector {
    // Imaging Properties
    protected static final String ARTEMIS_SEARCH_PREFIX = Configuration.get("artemis.tag.prefix_search");
    protected static final String IMAGING_OBJECT_LABEL = Configuration.get("imaging.node.object.label");
    protected static final String IMAGING_OBJECT_TAGS = Configuration.get("imaging.link.object_property.tags");
    protected static final String IMAGING_OBJECT_NAME = Configuration.get("imaging.node.object.name");
    protected static final String IMAGING_APPLICATION_LABEL = Configuration.get("imaging.application.label");

    // Member of the detector
    protected Neo4jAL neo4jAL;
    protected String application;
    protected List<Node> toInvestigateNodes;
    protected ReportGenerator reportGenerator;
    protected List<FrameworkNode> frameworkNodeList;
    protected NLPEngine nlpEngine;

    protected GoogleParser googleParser;
    protected LanguageProp languageProperties;

    public abstract List<FrameworkNode> launch() throws IOException, Neo4jQueryException;

    public void getNodes() throws Neo4jQueryException {
        List<String> categories = languageProperties.getObjectsInternalType();
        Result res;

        if(categories.isEmpty()) {
            String forgedRequest = String.format("MATCH (obj:%s:%s) WHERE  obj.Type CONTAINS '%s' AND obj.External=true RETURN obj as node",
                    IMAGING_OBJECT_LABEL, application, languageProperties.getName());
            res = neo4jAL.executeQuery(forgedRequest);

            while (res.hasNext()) {
                Map<String, Object> resMap = res.next();
                Node node = (Node) resMap.get("node");
                toInvestigateNodes.add(node);
            }
        } else {
            for(String type : categories) {
                String forgedRequest = String.format("MATCH (obj:%s:%s) WHERE  obj.InternalType='%s' AND obj.External=true RETURN obj as node",
                        IMAGING_OBJECT_LABEL, application, type);
                res = neo4jAL.executeQuery(forgedRequest);

                while (res.hasNext()) {
                    Map<String, Object> resMap = res.next();
                    Node node = (Node) resMap.get("node");
                    toInvestigateNodes.add(node);
                }
            }
        }
    }

    /**
     * Detector for the cobol
     * @param neo4jAL Neo4j Access Layer
     * @param application Name of the application
     * @param language Language
     * @throws IOException
     * @throws Neo4jQueryException
     */
    public ADetector(Neo4jAL neo4jAL, String application, SupportedLanguage language)
            throws IOException, Neo4jQueryException {
        this.neo4jAL = neo4jAL;
        this.application = application;
        this.toInvestigateNodes = new ArrayList<>();

        //Shuffle nodes to avoid being bust by the google bot detector
        Collections.shuffle(this.toInvestigateNodes);

        // Make sure the nlp is trained, train it otherwise
        NLPEngine nlpEngine = new NLPEngine(neo4jAL.getLogger(), language);
        if (!nlpEngine.checkIfModelExists()) {
            nlpEngine.train();
        }

        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String strDate = dateFormat.format(date);

        String reportName = application+strDate;
        this.reportGenerator = new ReportGenerator(reportName);
        this.googleParser = new GoogleParser(neo4jAL.getLogger());
        this.frameworkNodeList = new ArrayList<>();

        LanguageConfiguration lc = LanguageConfiguration.getInstance();
        this.languageProperties = lc.getLanguageProperties(language.toString());

        getNodes();

    }
}
