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

package com.castsoftware.artemis.controllers.api;

import com.castsoftware.artemis.config.detection.DetectionProp;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.detector.ADetector;
import com.castsoftware.artemis.detector.ATree;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.nlp.SupportedLanguage;
import com.castsoftware.artemis.results.LeafResult;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BreakdownController {

  /**
   * Get the breakdown of the structure of an application, flatten as a list
   *
   * @param neo4jAL Neo4j Access Layer
   * @param application Name of the application
   * @param language Language to be used
   * @return
   * @throws IOException
   * @throws Neo4jQueryException
   */
  public static List<LeafResult> getBreakDown(Neo4jAL neo4jAL, String application, String language, Boolean externality)
      throws IOException, Neo4jQueryException {
    ATree tree = getBreakDownAsTree(neo4jAL, application, language, externality);
    if(tree == null) return Collections.emptyList();

    return tree.flatten().stream()
        .map(x -> new LeafResult(x, tree.getDelimiterLeaves()))
        .collect(Collectors.toList());
  }

  /**
   * Get the breakdown of the structure of an application as tree
   *
   * @param neo4jAL Neo4j Access Layer
   * @param application Name of the application
   * @param language Language to be used
   * @param externality Externality of the breakdown
   * @return
   * @throws IOException
   * @throws Neo4jQueryException
   */
  public static ATree getBreakDownAsTree(Neo4jAL neo4jAL, String application, String language, Boolean externality)
      throws IOException, Neo4jQueryException {

    if (!SupportedLanguage.has(language)) return null;
    SupportedLanguage sl = SupportedLanguage.getLanguage(language);

    ADetector aDetector = ADetector.getDetector(neo4jAL, application, sl);
    ATree tree = null;

    if(externality) {
      tree = aDetector.getExternalBreakdown();
    } else {
      tree = aDetector.getInternalBreakdown();
    }

    if (tree == null) return null;

    return tree;
  }
}
