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

package com.castsoftware.artemis.sof;

import com.castsoftware.artemis.config.detection.LanguageConfiguration;
import com.castsoftware.artemis.config.detection.LanguageProp;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.nlp.SupportedLanguage;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;

import java.util.List;
import java.util.stream.Collectors;

public class SystemOfFramework {

  private Neo4jAL neo4jAL;
  private SupportedLanguage language;
  private String application;
  private List<FrameworkNode> frameworkNodeList;

  /**
   * Create a instance of System of Frameworks, to detect if the framework detected in your
   * application, is present somewhere else
   *
   * @param neo4jAL Neo4j Access Layer
   * @param language Language to detect
   * @param application Name of the Application
   * @param frameworkNodeList List of frameworks detected in the application
   */
  public SystemOfFramework(
      Neo4jAL neo4jAL,
      SupportedLanguage language,
      String application,
      List<FrameworkNode> frameworkNodeList) {
    this.neo4jAL = neo4jAL;
    this.language = language;
    this.application = application;
    this.frameworkNodeList = frameworkNodeList;
  }

  /** Run this instance of System of Framework to show the discovered links in your applications */
  public void run() {
    // Parse detected object in the application
    RelationshipType relationshipType = RelationshipType.withName("PRESENT_IN");
    LanguageProp lp = LanguageConfiguration.getInstance().getLanguageProperties(this.language);
    String internalTypeString =
        lp.getObjectsInternalType().stream().collect(Collectors.joining(", ", "\"", "\""));

    String request =
        "MATCH (o:Object)<-[]-(l:Level5) WHERE NOT '"
            + this.application
            + "' in LABELS(o) AND o.Name='%s' "
            + "and o.InternalType IN ["
            + internalTypeString
            + "] RETURN [x IN LABELS(o) WHERE x <> 'VZ' and x <> 'Object'][0] as application, l as level;";
    for (FrameworkNode fNode : frameworkNodeList) {
      try {
        Result res = neo4jAL.executeQuery(String.format(request, fNode.getName()));
        if (res.hasNext()) {
          String application = (String) res.next().get("application");
          Node level = (Node) res.next().get("level");

          neo4jAL.logInfo(
              String.format(
                  "Framework link found, will create link between object %s and application %s",
                  application, level.getProperty("Name")));
          // Create a link to the level
          Node targetApplicationNode = createSofObject(application);
          level.createRelationshipTo(targetApplicationNode, relationshipType);
        }
      } catch (Neo4jQueryException e) {
        neo4jAL.logError(
            "An error occurred parsing the applications to find similar frameworks.", e);
      }
    }
  }

  /**
   * Create an
   *
   * @param targetApplication Name of the Targeted application
   * @return The nod
   * @throws Neo4jQueryException
   */
  public Node createSofObject(String targetApplication) throws Neo4jQueryException {
    Label levelLabel = Label.label("Level5");
    Label applicationLabel = Label.label(application);

    Node node = neo4jAL.createNode(levelLabel);
    node.addLabel(applicationLabel);

    node.setProperty("Color", "rgb(233,66,53)");
    node.setProperty("Concept", true);
    node.setProperty("Count", 0L);
    node.setProperty(
        "FullName", "Services##Logic Services##Business Logic##Adobe##" + targetApplication);
    node.setProperty("Color", "rgb(233,66,53)");

    return node;
  }
}
