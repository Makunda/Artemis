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

package com.castsoftware.artemis.detector.plainAnalyzers.java;

import com.castsoftware.artemis.config.detection.DetectionParameters;
import com.castsoftware.artemis.controllers.UtilsController;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.detector.plainAnalyzers.ADetector;
import com.castsoftware.artemis.detector.utils.trees.java.JavaFrameworkTree;
import com.castsoftware.artemis.detector.utils.trees.java.JavaFrameworkTreeLeaf;
import com.castsoftware.artemis.detector.utils.functionalMaps.java.OldJavaFunctionalModule;
import com.castsoftware.artemis.detector.utils.trees.ATree;
import com.castsoftware.artemis.detector.utils.DetectorTypeMapper;
import com.castsoftware.artemis.exceptions.google.GoogleBadResponseCodeException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.exceptions.nlp.NLPBlankInputException;
import com.castsoftware.artemis.global.SupportedLanguage;
import com.castsoftware.artemis.modules.nlp.model.NLPResults;
import com.castsoftware.artemis.modules.nlp.parser.GoogleResult;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaFramework;
import com.castsoftware.artemis.modules.repositories.maven.MavenPackage;
import com.castsoftware.artemis.modules.sof.utils.SofUtilities;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Java Framework detector */
public class JavaOldDetector extends ADetector {

  private JavaFrameworkTree externalTree;
  private JavaFrameworkTree internalTree;
  private String corePrefix;

  /**
   * Detector for the Java
   *
   * @param neo4jAL Neo4j Access Layer
   * @param application Name of the application
   * @throws IOException
   * @throws Neo4jQueryException
   */
  public JavaOldDetector(Neo4jAL neo4jAL, String application, DetectionParameters detectionParameters)
      throws IOException, Neo4jQueryException {
    super(neo4jAL, application, SupportedLanguage.JAVA, detectionParameters);
    this.externalTree = getExternalBreakdown();
    this.internalTree = getInternalBreakDown();
    this.corePrefix = "";
  }

  @Override
  public JavaFrameworkTree getExternalBreakdown() throws Neo4jQueryException {
    // Filter nodes for java
    // Get node in Java Classes
    List<Node> nodeList = getNodesByExternality(true);

    nodeList.removeIf(n -> !n.hasProperty("Level") || !n.getProperty("Level").equals("Java Class"));
    neo4jAL.logInfo("Java breakdown on : " + nodeList.size());
    externalTree = new JavaFrameworkTree(languageProperties);
    return externalTree;
  }

  @Override
  public ATree getInternalBreakdown() throws Neo4jQueryException {
    return getInternalBreakDown();
  }


  /**
   * Analyze a tree of Java Classes
   * @param tree Tree to analyze
   * @param type Type of investigation ( internal / external )
   * @return The list of Framework found
   */
  private List<FrameworkNode> analyzeFrameworkTree(JavaFrameworkTree tree, String type) {
    // Initialize the list
    Queue<JavaFrameworkTreeLeaf> queue = new ConcurrentLinkedQueue<>();
    queue.add(tree.getRoot());

    // Iterate over the Tree
    JavaFrameworkTreeLeaf it;
    List<JavaFrameworkTreeLeaf> frameworkList = new ArrayList<>();
    Iterator<JavaFrameworkTreeLeaf> iterator = queue.iterator();
    while(iterator.hasNext()){
      it = iterator.next();
      neo4jAL.logInfo(String.format("Exploring leaf: %s ", it.getName()));
      if (it.getDepth() > 1) {
        // Process & query of pattern

        // Query Pythia to see if Framework apply and apply it to the leaf

        // If no results create a new Framework
        if (it.getDepth() == 3) {
          PythiaFramework pf = DetectorTypeMapper.frameworkLeafToPythia(it, this.language);
        }
      }

      queue.addAll(it.getChildren()); // Add the children
    }
    return new ArrayList<>();
  }

  @Override
  public void  extractFrameworks() throws IOException, Neo4jQueryException {
    neo4jAL.logInfo("Now extract known utilities for Java");
    // Init properties
    List<FrameworkNode> listFramework = new ArrayList<>();

    neo4jAL.logInfo("Extract External utilities");
    // Build a tree based on the nodes to investigate
    JavaFrameworkTree externals = this.getExternalBreakdown();
    listFramework.addAll(analyzeFrameworkTree(externals, "external"));

    // Internal match for java
    JavaFrameworkTree internalTree = getInternalBreakDown();
    listFramework.addAll(getInternalCandidates(internalTree));

    listFramework.forEach(this::addFrameworkToResults);
  }

  /**
   * Get the logic of the application
   *
   * @param tree
   * @return
   */
  public List<FrameworkNode> getInternalCandidates(JavaFrameworkTree tree) {
    neo4jAL.logInfo("Extract Internal utilities");
    String bestMatch = "";
    Integer biggestBranch = 0;
    List<OldJavaFunctionalModule> functionalModules = new ArrayList<>();

    List<JavaFrameworkTreeLeaf> toVisit = new ArrayList<>();
    for (JavaFrameworkTreeLeaf ftl : tree.getRoot().getChildren()) {
      toVisit.addAll(ftl.getChildren());
    }

    // Todo review the logic
    for (JavaFrameworkTreeLeaf treeLeaf : toVisit) {
      try {
        OldJavaFunctionalModule fm =
            new OldJavaFunctionalModule(
                neo4jAL,
                application,
                treeLeaf.getFullName(),
                languageProperties,
                treeLeaf.getDepth());
        if (fm.isOnlyUsed()) {
          functionalModules.add(fm);
        }

        if (treeLeaf.getCount() >= biggestBranch) {
          biggestBranch = treeLeaf.getCount().intValue();
          bestMatch = treeLeaf.getFullName();
        }
      } catch (Neo4jQueryException e) {
        neo4jAL.logError("Failed to create architecture view", e);
      }
    }

    // Get name of the company
    neo4jAL.logInfo(String.format("The logic core seems to be located under '%s'", bestMatch));

    String companyName = "";
    if (bestMatch.split("\\.").length > 1) {
      companyName = "of " + bestMatch.split("\\.")[1];
      corePrefix = bestMatch.split("\\.")[1];
    }

    // Convert the functional modules found to FrameworkNodes
    List<FrameworkNode> frameworkNodes = new ArrayList<>();
    for (OldJavaFunctionalModule fm : functionalModules) {
      String pattern = fm.getIdentifier() + "\\.*";
      Boolean isRegex = true;

      FrameworkNode fn =
          new FrameworkNode(
              neo4jAL,
              fm.getIdentifier(),
              pattern,
              isRegex,
              new SimpleDateFormat("dd-MM-yyyy").format(new Date()),
              "Internal Match " + application,
              "Internal framework " + companyName,
              1L);
      fn.setInternalTypes(languageProperties.getObjectsInternalType());
      fn.setCategory(String.format("Internal frameworks %s", companyName));
      fn.setFrameworkType(FrameworkType.TO_INVESTIGATE);

      frameworkNodes.add(fn);
    }

    return frameworkNodes;
  }

  /**
   * Get the list of node for the Java language by externality
   *
   * @param externality Externality of the nodes
   * @return The list of nodes
   * @throws Neo4jQueryException If the Neo4j query failed
   */
  public List<Node> getNodesByExternality(Boolean externality) throws Neo4jQueryException {
    try {
      List<String> categories = languageProperties.getObjectsInternalType();
      List<Node> nodeList = new ArrayList<>();
      Result res;

      if (categories.isEmpty()) {
        String forgedRequest =
            String.format(
                "MATCH (obj:Object:`%s`) WHERE obj.Level='Java Class' AND obj.External=$externality RETURN obj as node",
                application);
        Map<String, Object> params = Map.of("externality", externality);
        res = neo4jAL.executeQuery(forgedRequest, params);

        while (res.hasNext()) {
          Map<String, Object> resMap = res.next();
          Node node = (Node) resMap.get("node");
          nodeList.add(node);
        }
      } else {
        String forgedRequest =
            String.format(
                "MATCH (obj:Object:`%s`) WHERE  obj.InternalType in $internalTypes  AND obj.External=$externality  RETURN obj as node",
                application);
        Map<String, Object> params =
            Map.of("internalTypes", categories, "externality", externality);
        res = neo4jAL.executeQuery(forgedRequest, params);

        while (res.hasNext()) {
          Map<String, Object> resMap = res.next();
          Node node = (Node) resMap.get("node");
          nodeList.add(node);
        }
      }

      neo4jAL.logInfo(
          String.format(
              "%d Java nodes were found with external property on '%s'",
              nodeList.size(), externality));

      return nodeList;
    } catch (Neo4jQueryException err) {
      neo4jAL.logError(
          String.format(
              "Failed to retrieve the list of the external nodes in the application %s",
              this.application),
          err);
      throw err;
    }
  }

  /**
   * Get Google Results
   *
   * @param name
   * @return
   */
  private Optional<NLPResults> getGoogleResult(String name) {
    try {
      GoogleResult gr = null;
      gr = googleParser.request(name);
      String requestResult = gr.getContent();
      NLPResults np = nlpEngine.getNLPResult(requestResult);

      if (getLearningMode()) {
        nlpSaver.writeNLPResult(np.getCategory(), requestResult);
      }

      return Optional.of(np);
    } catch (IOException | NLPBlankInputException e) {
      neo4jAL.logError(String.format("Failed to get the NLP Results for the object %s", name));
      return Optional.empty();
    } catch (GoogleBadResponseCodeException err) {
      neo4jAL.logError("Banned from Google API due to too many request. Good luck.", err);
      this.googleParser = null;
      return Optional.empty();
    }
  }


  /**
   * Save a maven result to a Framework node
   * @param pack Maven package
   * @return A framework node
   */
  private FrameworkNode saveMavenResult(MavenPackage pack) {
    String pattern = pack.getFullName() + "\\.*";
    Boolean isRegex = true;

    FrameworkNode fb =
            new FrameworkNode(
                    neo4jAL,
                    pack.getFullName(),
                    pattern,
                    isRegex,
                    new SimpleDateFormat("dd-MM-yyyy").format(new Date()),
                    "Maven " + pack.getFullName(),
                    pack.getFullName(),
                    1L,
                    1.,
                    new Date().getTime());
    fb.setFrameworkType(FrameworkType.FRAMEWORK);
    fb.setInternalTypes(languageProperties.getObjectsInternalType());

    return fb;
  }

  public void extractUnknownApp() {
    try {
      if (corePrefix.isBlank()) return;
      neo4jAL.logInfo(
          String.format(
              "Extract the unkown matching the core of the application : %s", corePrefix));

      // If the object match the Ngram
      ListIterator<Node> itNode = toInvestigateNodes.listIterator();
      while (itNode.hasNext()) {
        Node n = itNode.next();
        if (!n.hasProperty("FullName")) continue;

        String name = (String) n.getProperty("FullName");

        // The name match the nGram
        if (name.contains(corePrefix)) {
          UtilsController.applyDemeterParentTag(neo4jAL, n, ": Unknowns Application code");
          itNode.remove();
        }
      }

    } catch (Neo4jQueryException e) {
      neo4jAL.logError("Failed to retrieve the core of the application.", e);
      return;
    }
  }

  @Override
  public void extractOtherApps() {
    neo4jAL.logInfo("Extracting package in other applications");
    // Start for n = 2
    List<Node> createdNodes = new ArrayList<>();

    // Build new tree with remaining nodes
    JavaFrameworkTree frameworkTree = new JavaFrameworkTree(languageProperties);

    List<JavaFrameworkTreeLeaf> toVisit =
        Collections.synchronizedList(frameworkTree.getRoot().getChildren());
    Map<String, Set<String>> mapPackageApp = new HashMap<>();

    for (ListIterator<JavaFrameworkTreeLeaf> itRel = toVisit.listIterator(); itRel.hasNext(); ) {
      try {
        JavaFrameworkTreeLeaf ftl = itRel.next();

        if (ftl.getDepth() < 3) {
          // Add the children
          for (JavaFrameworkTreeLeaf l : ftl.getChildren()) itRel.add(l);
        }

        // Skip for a depth  inferior to the company name package
        if (ftl.getDepth() < 2) continue;

        Set<String> applications =
            SofUtilities.getPresenceInOtherApplications(neo4jAL, application, ftl.getFullName());
        for (String app : applications) {
          if (!mapPackageApp.containsKey(ftl.getFullName())) {
            mapPackageApp.put(ftl.getFullName(), new HashSet<>());
          }
          mapPackageApp.compute(
              ftl.getFullName(),
              (key, val) -> {
                val.add(app);
                return val;
              });
        }
      } catch (Neo4jQueryException e) {
        neo4jAL.logError("Failed to treat a node for external extraction.", e);
      }
    }

    // Map the package used
    for (Map.Entry<String, Set<String>> entry : mapPackageApp.entrySet()) {
      try {
        String nameObj =
            entry.getKey() + " presents  on [ " + String.join(", ", entry.getValue()) + " ]";
        Node n = SofUtilities.createSofObject(neo4jAL, application, nameObj);
        SofUtilities.fromLevelConceptRel(neo4jAL, application, n.getId(), "Java Class");
        createdNodes.add(n);
      } catch (Neo4jQueryException | Neo4jBadRequestException e) {
        e.printStackTrace();
      }
    }

    // Extract the objects
    ListIterator<Node> itNode = toInvestigateNodes.listIterator();
    while (itNode.hasNext()) {
      try {

        Node n = itNode.next();
        if (!n.hasProperty("Name") || !n.hasProperty("InternalType")) continue;

        String name = (String) n.getProperty("Name");
        String internalType = (String) n.getProperty("InternalType");

        String req =
            "MATCH (o:Object) WHERE NOT $appName in LABELS(o) AND o.Name=$nodeName AND o.InternalType=$internalType "
                + "RETURN [ x in LABELS(o) WHERE NOT x='Object'][0] as app";
        Map<String, Object> params =
            Map.of("appName", application, "nodeName", name, "internalType", internalType);

        Result res = neo4jAL.executeQuery(req, params);
        if (res.hasNext()) {
          UtilsController.applyDemeterParentTag(neo4jAL, n, " : Unknowns other Applications");
          itNode.remove();
        }

      } catch (Neo4jQueryException e) {
        neo4jAL.logError(
            String.format("Failed to extract node with name %s to  Unknown other applications", e));
      }
    }
  }

  public void extractUnknownNonUtilities() {
    ListIterator<Node> itNode = toInvestigateNodes.listIterator();
    while (itNode.hasNext()) {
      Node n = itNode.next();
      try {
        UtilsController.applyDemeterParentTag(neo4jAL, n, ": Unknowns Non Utilities");
      } catch (Neo4jQueryException e) {
        neo4jAL.logError(
            String.format("Failed to extract node with name %s to Unknown Non Utilities", e));
      }
      itNode.remove();
    }
  }

  /**
   * Get the internal breakdown of the package
   *
   * @return The Framework tree containing only internal objects
   * @throws Neo4jQueryException If the Neo4j Cypher request fails
   */
  public JavaFrameworkTree getInternalBreakDown() throws Neo4jQueryException {
    List<Node> nodeList = getNodesByExternality(false);
    this.internalTree = new JavaFrameworkTree(languageProperties);

    String fullName;

    for (Node n : nodeList) {
      if (!n.hasProperty(IMAGING_OBJECT_FULL_NAME)) continue;

      fullName = (String) n.getProperty(IMAGING_OBJECT_FULL_NAME);

      internalTree.insert(fullName, n);
    }

    return internalTree;
  }

}
