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

package com.castsoftware.artemis.detector.java;

import com.castsoftware.artemis.config.UserConfiguration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.detector.ADetector;
import com.castsoftware.artemis.exceptions.google.GoogleBadResponseCodeException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.exceptions.nlp.NLPBlankInputException;
import com.castsoftware.artemis.nlp.SupportedLanguage;
import com.castsoftware.artemis.nlp.model.NLPCategory;
import com.castsoftware.artemis.nlp.model.NLPConfidence;
import com.castsoftware.artemis.nlp.model.NLPResults;
import com.castsoftware.artemis.nlp.parser.GoogleResult;
import com.castsoftware.artemis.repositories.maven.Maven;
import com.castsoftware.artemis.repositories.maven.MavenPackage;
import com.castsoftware.artemis.sof.utils.SofUtilities;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class JavaDetector extends ADetector {

  private FrameworkTree ft;

  /**
   * Detector for the Java
   *
   * @param neo4jAL Neo4j Access Layer
   * @param application Name of the application
   * @throws IOException
   * @throws Neo4jQueryException
   */
  public JavaDetector(Neo4jAL neo4jAL, String application) throws IOException, Neo4jQueryException {
    super(neo4jAL, application, SupportedLanguage.JAVA);
    this.ft = new FrameworkTree();
  }

  public FrameworkNode assignResult(String name, NLPResults results, String internalType) {
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    Date date = Calendar.getInstance().getTime();
    String strDate = dateFormat.format(date);

    FrameworkType fType = null;

    if (results.getConfidence()
        == NLPConfidence.NOT_CONFIDENT) { // If the confidence score is not high enough it
      // will be added on the to investigate dt
      fType = FrameworkType.TO_INVESTIGATE;
    } else if (results.getCategory() == NLPCategory.FRAMEWORK) { // Detected as a framework
      fType = FrameworkType.FRAMEWORK;
    } else { // Detected as a not framework
      fType = FrameworkType.NOT_FRAMEWORK;
    }

    // Retrieve highest detection score
    double[] prob = results.getProbabilities();
    double detectionScore = 0.0;
    if (prob.length > 0) {
      detectionScore = prob[0];
    }

    FrameworkNode fb =
        new FrameworkNode(
            neo4jAL,
            name,
            strDate,
            "No location discovered",
            "",
            1L,
            detectionScore,
            new Date().getTime());
    fb.setFrameworkType(fType);
    fb.setInternalType(internalType);

    // Save the Node to the local database

    return fb;
  }

  @Override
  public List<FrameworkNode> launch()
      throws IOException, Neo4jQueryException, Neo4jBadRequestException {
    // Get Dynamic parameters
    boolean onlineMode = Boolean.parseBoolean(UserConfiguration.get("artemis.onlineMode"));
    boolean learningMode = Boolean.parseBoolean(UserConfiguration.get("artemis.learning_mode"));

    // Init properties
    List<FrameworkNode> frameworkNodeList = new ArrayList<>();

    // Internal match
    FrameworkTree internalTree = getInternalBreakDown();
    frameworkNodeList.addAll(getInternalCandidates(internalTree));

    // checkPresenceInOthers(internalTree);

    // External Match
    FrameworkTree externalTree = getExternalBreakdown();
    frameworkNodeList.addAll(launchExternalDetection(externalTree));

    // Save
    frameworkNodeList.forEach(
        x -> {
          try {
            if (getPersistentMode()) {
              x.createNode();
            }
          } catch (Neo4jQueryException e) {
            neo4jAL.logError("Failed to create a framework node.", e);
          }
        });

    return frameworkNodeList;
  }

  /**
   * Treat the external nodes
   *
   * @return
   */
  private List<FrameworkNode> launchExternalDetection(FrameworkTree tree) {
    List<FrameworkTreeLeaf> branchStarterList = new ArrayList<>(tree.getRoot().getChildren());

    List<FrameworkTreeLeaf> packages = new ArrayList<>();
    for (FrameworkTreeLeaf ftl : branchStarterList) {
      packages.addAll(ftl.getChildren());
    }

    List<FrameworkNode> frameworkNodes = new ArrayList<>();

    for (ListIterator<FrameworkTreeLeaf> itRel = packages.listIterator(); itRel.hasNext(); ) {
      FrameworkTreeLeaf ftl = itRel.next();

      // Parse repositories
      neo4jAL.logInfo(
          String.format(
              "Querying Maven for : %s with children [ %s ] ",
              ftl.getFullName(),
                  ftl.getChildren().stream()
                      .map(FrameworkTreeLeaf::getFullName)
                      .collect(Collectors.joining(" "))));

      parseMaven(ftl.getFullName());
      if (getOnlineMode()) {
        NLPResults res = getGoogleResult(ftl.getFullName());

        if (res != null) {
          frameworkNodes.add(assignResult(ftl.getFullName(), res, "Java"));
        }
      }
    }

    return frameworkNodes;
  }

  private NLPResults getGoogleResult(String name) {
    GoogleResult gr = null;
    try {
      gr = googleParser.request(name);
      String requestResult = gr.getContent();
      NLPResults np = nlpEngine.getNLPResult(requestResult);

      if (getLearningMode()) {
        nlpSaver.writeNLPResult(np.getCategory(), requestResult);
      }

      return np;
    } catch (IOException | GoogleBadResponseCodeException | NLPBlankInputException e) {
      neo4jAL.logError("Failed to get the NLP Results for the ");
    }

    return null;
  }

  /**
   * Parse the Maven repository for Java
   *
   * @param name
   * @return
   */
  private List<MavenPackage> parseMaven(String name) {
    Maven mvn = new Maven();
    List<MavenPackage> mavenPackages = new ArrayList<>();

    try {
      mavenPackages = mvn.getMavenPackages(name);

      for (MavenPackage p : mavenPackages) {
        neo4jAL.logInfo(" Found : " + p.toString());
      }
    } catch (UnirestException e) {
      neo4jAL.logInfo(String.format("Failed to query Maven for package : %s ", name));
    }

    return mavenPackages;
  }

  /**
   * Find the presence of package in other applications
   *
   * @return
   */
  public List<Node> checkPresenceInOthers(FrameworkTree tree)
      throws Neo4jQueryException, Neo4jBadRequestException {
    // Start for n = 2
    List<Node> createdNodes = new ArrayList<>();
    List<FrameworkTreeLeaf> toVisit = Collections.synchronizedList(tree.getRoot().getChildren());

    for (ListIterator<FrameworkTreeLeaf> itRel = toVisit.listIterator(); itRel.hasNext(); ) {
      FrameworkTreeLeaf ftl = itRel.next();

      if (ftl.getDepth() < 3) {
        // Add the children
        for (FrameworkTreeLeaf l : ftl.getChildren()) itRel.add(l);
      }

      // Skip for a depth  inferior to the company name package
      if (ftl.getDepth() < 2) continue;

      Set<String> applications =
          SofUtilities.getPresenceInOtherApplications(neo4jAL, application, ftl.getFullName());
      for (String app : applications) {
        neo4jAL.logInfo(
            String.format(
                "Application interaction detected on : %s for package with name : %s",
                app, ftl.getFullName()));
        Node n = SofUtilities.createSofObject(neo4jAL, application, app);
        SofUtilities.fromLevelConceptRel(neo4jAL, application, n.getId(), "Java Class");
        createdNodes.add(n);
      }

      // DEBUG Explode Modules
    }

    return createdNodes;
  }

  /**
   * Get the number of match on the fullname for a given regex expression
   *
   * @param regex
   * @return
   * @throws Neo4jQueryException
   */
  public Long numMatchByFullName(String regex) throws Neo4jQueryException {
    // Get the package with the biggest number of objects
    String req = "MATCH (o:Object:`%s`) WHERE o.FullName=~$regex RETURN COUNT(o) as numObj";
    Map<String, Object> params = Map.of("regex", regex);

    Result res = neo4jAL.executeQuery(req, params);
    if (!res.hasNext()) return 0L;

    return (Long) res.next().get("numObj");
  }

  /**
   * Get the logic of the application
   *
   * @param tree
   * @return
   */
  public List<FrameworkNode> getInternalCandidates(FrameworkTree tree) {
    String bestMatch = "";
    Integer biggestBranch = 0;
    List<FunctionalModule> functionalModules = new ArrayList<>();

    List<FrameworkTreeLeaf> toVisit = new ArrayList<>();

    for (FrameworkTreeLeaf ftl : tree.getRoot().getChildren()) {
      toVisit.addAll(ftl.getChildren());
    }

    // Todo review the logic
    for (FrameworkTreeLeaf treeLeaf : toVisit) {
      try {
        FunctionalModule fm =
            new FunctionalModule(
                neo4jAL,
                application,
                treeLeaf.getFullName(),
                languageProperties,
                treeLeaf.getDepth());
        if (fm.isOnlyUsed()) {
          functionalModules.add(fm);
        }

        if (treeLeaf.getNumChildren() >= biggestBranch) {
          biggestBranch = treeLeaf.getNumChildren();
          bestMatch = treeLeaf.getFullName();
        }
      } catch (Neo4jQueryException e) {
        neo4jAL.logError("Failed to create architecture view", e);
      }
    }

    // Extract the logic
    /* try {
      UtilsController.applyDemeterTagRegexFullName(neo4jAL, bestMatch+".*", "Java Core Logic");
    } catch (Neo4jQueryException e) {
      neo4jAL.logInfo("Failed to extract the core logic");
    } */

    // Get name of the company
    neo4jAL.logInfo(String.format("The logic core seems to be located under '%s'", bestMatch));
    String companyName = "";
    if (bestMatch.split("\\.").length > 1) {
      companyName = "of " + bestMatch.split("\\.")[1];
    }

    // Convert the functional modules found to FrameworkNodes
    List<FrameworkNode> frameworkNodes = new ArrayList<>();
    for (FunctionalModule fm : functionalModules) {
      frameworkNodes.add(
          new FrameworkNode(
              neo4jAL,
              fm.getIdentifier(),
              new SimpleDateFormat("dd-MM-yyyy").format(new Date()),
              "Internal Match " + application,
              "Internal framework " + companyName,
              1L));
    }

    return frameworkNodes;
  }

  /**
   * Get the list of Internal nodes for the JAVA languages
   *
   * @return
   * @throws Neo4jQueryException
   */
  public List<Node> getInternalCandidates() throws Neo4jQueryException {
    List<String> categories = languageProperties.getObjectsInternalType();
    List<Node> nodeList = new ArrayList<>();
    Result res;

    if (categories.isEmpty()) {
      String forgedRequest =
          String.format(
              "MATCH (obj:%s:`%s`) WHERE  obj.Type CONTAINS 'java' AND NOT obj.Type CONTAINS 'javascript' AND obj.External=false RETURN obj as node",
              application);
      res = neo4jAL.executeQuery(forgedRequest);

      while (res.hasNext()) {
        Map<String, Object> resMap = res.next();
        Node node = (Node) resMap.get("node");
        nodeList.add(node);
      }
    } else {
      String forgedRequest =
          String.format(
              "MATCH (obj:Object:`%s`) WHERE  obj.InternalType in $internalTypes AND obj.External=false  RETURN obj as node",
              application);
      Map<String, Object> params = Map.of("internalTypes", categories);
      res = neo4jAL.executeQuery(forgedRequest, params);

      while (res.hasNext()) {
        Map<String, Object> resMap = res.next();
        Node node = (Node) resMap.get("node");
        nodeList.add(node);
      }
    }

    return nodeList;
  }

  /**
   * Get the internal breakdown of the package
   *
   * @return
   * @throws Neo4jQueryException
   */
  public FrameworkTree getInternalBreakDown() throws Neo4jQueryException {
    List<Node> nodeList = getInternalCandidates();
    FrameworkTree internalTree = new FrameworkTree();

    String fullName;
    for (Node n : nodeList) {
      if (!n.hasProperty(IMAGING_OBJECT_FULL_NAME)) continue;
      fullName = (String) n.getProperty(IMAGING_OBJECT_FULL_NAME);

      internalTree.insert(fullName);
    }

    return internalTree;
  }

  @Override
  public FrameworkTree getExternalBreakdown() {
    FrameworkTree frameworkTree = new FrameworkTree();

    // Top Bottom approach
    String fullName;
    for (Node n : toInvestigateNodes) {

      // Get node in Java Classes
      if (!n.hasProperty("Level") || ((String) n.getProperty("Level")).equals("Java Class"))
        continue;

      if (!n.hasProperty(IMAGING_OBJECT_FULL_NAME)) continue;
      fullName = (String) n.getProperty(IMAGING_OBJECT_FULL_NAME);
      frameworkTree.insert(fullName);
    }

    return frameworkTree;
  }
}
