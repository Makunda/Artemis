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

import com.castsoftware.artemis.config.detection.DetectionParameters;
import com.castsoftware.artemis.controllers.UtilsController;
import com.castsoftware.artemis.controllers.api.FrameworkController;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.detector.ADetector;
import com.castsoftware.artemis.detector.java.utils.FrameworkTree;
import com.castsoftware.artemis.detector.java.utils.FrameworkTreeLeaf;
import com.castsoftware.artemis.detector.java.utils.FunctionalModule;
import com.castsoftware.artemis.detector.utils.ATree;
import com.castsoftware.artemis.detector.utils.DetectorTypeMapper;
import com.castsoftware.artemis.exceptions.google.GoogleBadResponseCodeException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.exceptions.nlp.NLPBlankInputException;
import com.castsoftware.artemis.modules.nlp.SupportedLanguage;
import com.castsoftware.artemis.modules.nlp.model.NLPCategory;
import com.castsoftware.artemis.modules.nlp.model.NLPConfidence;
import com.castsoftware.artemis.modules.nlp.model.NLPResults;
import com.castsoftware.artemis.modules.nlp.parser.GoogleResult;
import com.castsoftware.artemis.modules.pythia.exceptions.PythiaException;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaFramework;
import com.castsoftware.artemis.modules.repositories.maven.Maven;
import com.castsoftware.artemis.modules.repositories.maven.MavenPackage;
import com.castsoftware.artemis.modules.sof.utils.SofUtilities;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/** Java Framework detector */
public class JavaDetector extends ADetector {

  private FrameworkTree externalTree;
  private FrameworkTree internalTree;
  private String corePrefix;

  /**
   * Detector for the Java
   *
   * @param neo4jAL Neo4j Access Layer
   * @param application Name of the application
   * @throws IOException
   * @throws Neo4jQueryException
   */
  public JavaDetector(Neo4jAL neo4jAL, String application, DetectionParameters detectionParameters)
      throws IOException, Neo4jQueryException {
    super(neo4jAL, application, SupportedLanguage.JAVA, detectionParameters);
    this.externalTree = getExternalBreakdown();
    this.internalTree = getInternalBreakDown();
    this.corePrefix = "";
  }

  @Override
  public FrameworkTree getExternalBreakdown() throws Neo4jQueryException {
    // Filter nodes for java
    // Get node in Java Classes

    List<Node> nodeList = getNodesByExternality(true);

    nodeList.removeIf(n -> !n.hasProperty("Level") || !n.getProperty("Level").equals("Java Class"));
    neo4jAL.logInfo("Java breakdown on : " + nodeList.size());
    externalTree = getExternalBreakDown(nodeList);
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
  private List<FrameworkNode> analyzeFrameworkTree(FrameworkTree tree, String type) {
    // Initialize the list
    Queue<FrameworkTreeLeaf> queue = new LinkedList<>();
    queue.add(tree.getRoot());

    // Iterate over the Tree
    FrameworkTreeLeaf it;
    for (FrameworkTreeLeaf frameworkTreeLeaf : queue) {
      it = frameworkTreeLeaf;

      if (it.getDepth() > 1) {
        // Process & query of pattern

        // Query Pythia to see if Framework apply and apply it to the leaf

        // If no results create a new Framework
        if (it.getDepth() == 3) {
          PythiaFramework pf = DetectorTypeMapper.frameworkLeafToPythia(it, this.language.toString());
          this.savePythiaFramework(pf);
        }
      }

      queue.addAll(it.getChildren()); // Add the children
    }
    return new ArrayList<>();
  }

  @Override
  public List<FrameworkNode> extractUtilities() throws IOException, Neo4jQueryException {
    neo4jAL.logInfo("Now extract known utilities for Java");
    // Init properties
    List<FrameworkNode> listFramework = new ArrayList<>();

    neo4jAL.logInfo("Extract External utilities");
    FrameworkTree externals = getExternalBreakDown(toInvestigateNodes);
    listFramework.addAll(analyzeFrameworkTree(externals, "external"));

    // Internal match for java
    FrameworkTree internalTree = getInternalBreakDown();
    listFramework.addAll(getInternalCandidates(internalTree));

    return listFramework;
  }

  /**
   * Treat the external nodes
   *
   * @return
   */
  private List<FrameworkNode> exploreCandidates(FrameworkTree tree) {
    List<FrameworkTreeLeaf> branchStarterList = new ArrayList<>(tree.getRoot().getChildren());

    List<FrameworkTreeLeaf> packages = new ArrayList<>();
    for (FrameworkTreeLeaf ftl : branchStarterList) {
      packages.addAll(ftl.getChildren());
    }

    List<FrameworkNode> frameworkNodes = new ArrayList<>();

    for (FrameworkTreeLeaf ftl : packages) {
      try {
        frameworkNodes.addAll(explodeLeaf(ftl)); // Explode the modules
      } catch (Neo4jQueryException e) {
        neo4jAL.logError(
            String.format("Failed to explode the functional leaf with name %s", ftl.getFullName()));
      }
    }

    return frameworkNodes;
  }

  /**
   * Get the logic of the application
   *
   * @param tree
   * @return
   */
  public List<FrameworkNode> getInternalCandidates(FrameworkTree tree) {
    neo4jAL.logInfo("Extract Internal utilities");
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
    for (FunctionalModule fm : functionalModules) {
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
   * Get the break down of all the external Java Objects, and sort them in a tree
   *
   * @param nodeList List of node to treat and to insert in the tree
   * @return The Framework tree containing only the external objects
   */
  public FrameworkTree getExternalBreakDown(List<Node> nodeList) {
    FrameworkTree frameworkTree = new FrameworkTree();

    // Top Bottom approach
    String fullName;
    ListIterator<Node> listIterator = nodeList.listIterator();
    while (listIterator.hasNext()) {
      Node n = listIterator.next();

      // Get node in Java Class Level
      if (!n.hasProperty("Level") || !n.getProperty("Level").equals("Java Class")) {
        listIterator.remove();
        continue;
      }

      if (!n.hasProperty(IMAGING_OBJECT_FULL_NAME)) continue;

      fullName = (String) n.getProperty(IMAGING_OBJECT_FULL_NAME);
      frameworkTree.insert(fullName, n);
    }

    return frameworkTree;
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

  /**
   * Explode leaf to extract the functional modules
   *
   * @param ftl
   * @return
   */
  private List<FrameworkNode> explodeLeaf(FrameworkTreeLeaf ftl) {
    List<FrameworkNode> frameworkNodes = new ArrayList<>();

    // Get base leaf detection rate
    Optional<NLPResults> res = Optional.empty();
    Optional<MavenPackage> candidate = Optional.empty();

    // If online mode, then parse Google and Maven
    if (getOnlineMode()) {
      res = getGoogleResult(ftl.getFullName());
      candidate = parseMaven(ftl.getFullName());
    }

    // If the package on maven match the exact name or fullName, validate the Framework
    if (candidate.isPresent()
        && (candidate.get().getName().equals(ftl.getName())
            || candidate.get().getFullName().equals(ftl.getFullName()))) { // Perfect Match on maven

      FrameworkNode fb = this.saveMavenResult(candidate.get());
      return Collections.singletonList(fb);
    }

    // The Leaf has no children. It will use this last results as Framework
    if (ftl.getChildren().isEmpty()) { // No Children, take the result of the leaf
      FrameworkNode fn = assignResult(ftl.getFullName(), res.get());
      return Collections.singletonList(fn);

    } else {
      int countBetterCandidate = 0;
      Map<String, NLPResults> nlpResultsMap = new HashMap<>();

      // Explode the children, if the functional modules has no link between each other
      neo4jAL.logInfo("Entering in children of " + Arrays.toString(ftl.getChildren().toArray()));
      for (FrameworkTreeLeaf leaf : ftl.getChildren()) {
        Optional<NLPResults> resLeaf = getGoogleResult(leaf.getFullName());
        if (resLeaf.isEmpty()) continue;

        nlpResultsMap.put(ftl.getFullName(), resLeaf.get());

        // Get the children with a better detection score
        if (res.get().getProbabilities()[1] < resLeaf.get().getProbabilities()[1]) {
          countBetterCandidate++;
        }
      }

      // If there is a better confidence on more than half of the children, explode
      if (countBetterCandidate >= ftl.getChildren().size() / 2) {
        for (Map.Entry<String, NLPResults> resultsEntry : nlpResultsMap.entrySet()) {
          frameworkNodes.add(assignResult(resultsEntry.getKey(), resultsEntry.getValue()));
        }
      } else { // Keep the original leaf
        frameworkNodes = Collections.singletonList(assignResult(ftl.getFullName(), res.get()));
      }

      return frameworkNodes;
    }
  }

  @Override
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
    FrameworkTree frameworkTree = getExternalBreakDown(toInvestigateNodes);

    List<FrameworkTreeLeaf> toVisit =
        Collections.synchronizedList(frameworkTree.getRoot().getChildren());
    Map<String, Set<String>> mapPackageApp = new HashMap<>();

    for (ListIterator<FrameworkTreeLeaf> itRel = toVisit.listIterator(); itRel.hasNext(); ) {
      try {
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

  @Override
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
  public FrameworkTree getInternalBreakDown() throws Neo4jQueryException {
    List<Node> nodeList = getNodesByExternality(false);
    this.internalTree = new FrameworkTree();

    String fullName;

    for (Node n : nodeList) {
      if (!n.hasProperty(IMAGING_OBJECT_FULL_NAME)) continue;

      fullName = (String) n.getProperty(IMAGING_OBJECT_FULL_NAME);

      internalTree.insert(fullName, n);
    }

    return internalTree;
  }

  /**
   * Parse the Maven repository for Java
   *
   * @param name Name of the package to search
   * @return Optional returning the MavenPackage
   */
  private Optional<MavenPackage> parseMaven(String name) {
    Maven mvn = new Maven();
    Optional<MavenPackage> bestCandidate = Optional.empty();

    List<MavenPackage> mavenPackages = mvn.getMavenPackages(name);
    if (mavenPackages.size() > 0) {
      bestCandidate = Optional.of(mavenPackages.get(0));
    }

    return bestCandidate;
  }

  /**
   * Assign Results to a framework node
   *
   * @param name Name of the Framework
   * @param results Type of results
   * @return
   */
  public FrameworkNode assignResult(String name, NLPResults results) {
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

    String pattern = name;
    Boolean isRegex = false;

    FrameworkNode fb =
        new FrameworkNode(
            neo4jAL,
            name,
            pattern,
            isRegex,
            strDate,
            "No location discovered",
            "",
            1L,
            detectionScore,
            new Date().getTime());
    fb.setFrameworkType(fType);
    fb.setInternalTypes(languageProperties.getObjectsInternalType());

    // Save the Node to the local database

    return fb;
  }

  /**
   * Get the number of match on the fullname for a given regex expression
   *
   * @param regex Regex to be matched
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
}
