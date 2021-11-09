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

package com.castsoftware.artemis.detector.statisticalAnalyzers.java;

import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.detector.utils.DetectionCategory;
import com.castsoftware.artemis.detector.utils.DetectorNodesUtil;
import com.castsoftware.artemis.detector.utils.DetectorPropertyUtil;
import com.castsoftware.artemis.detector.utils.trees.ALeaf;
import com.castsoftware.artemis.detector.utils.trees.TreeFactory;
import com.castsoftware.artemis.detector.utils.trees.java.JavaFrameworkTree;
import com.castsoftware.artemis.detector.utils.trees.java.JavaFrameworkTreeLeaf;
import com.castsoftware.artemis.detector.utils.functionalMaps.java.OldJavaFunctionalModule;
import com.castsoftware.artemis.detector.statisticalAnalyzers.AStatisticalAnalyzer;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.global.SupportedLanguage;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;


import java.util.List;
import java.util.Map;

public class JavaStatisticalAnalyzer extends AStatisticalAnalyzer {
	/**
	 * Constructor
	 *
	 * @param neo4jAL     Neo4j Access Layer
	 * @param application Name of the application
	 */
	public JavaStatisticalAnalyzer(Neo4jAL neo4jAL, String application, SupportedLanguage language) {
		super(neo4jAL, application, language);
	}

	/**
	 * Get the core of the application based on the size of the packages
	 * @throws Neo4jQueryException
	 */
	@Override
	public void flagCore() throws Neo4jQueryException {
		// Get the list of object to treat
		List<Node> nodeList = DetectorNodesUtil.getInternalObjects(neo4jAL, languageProp, applicationName);

		// Create a treeD
		JavaFrameworkTree tree = TreeFactory.createJavaTree(languageProp, nodeList);

		// Parse the tree slice on level 3 ( companty )
		ALeaf bestMatch = null;
		Integer biggestBranch = 0;
		List<ALeaf> treeSlice = tree.getSliceByDepth(3);

		for (ALeaf treeLeaf : treeSlice) {
			if (treeLeaf.getCount() >= biggestBranch) {
				biggestBranch = treeLeaf.getCount().intValue();
				bestMatch = treeLeaf;
			}
		}

		if(bestMatch == null) {
			neo4jAL.logInfo("Failed to discover the core logic of the application. The functional module construction failed.");
			return;
		}

		// Get name of the company
		neo4jAL.logInfo(String.format("The logic core seems to be located under '%s'." +
				"Extraction of external class with the same name will start.", bestMatch));

		// Extract external classes with the same name
		String toFlag = String.format("MATCH (o:Object:%s) " +
				"WHERE o.External=true AND o.FullName STARTS WITH $pattern " +
						"RETURN o as node", this.getSanitizedApplication());

		Result flagNodes = neo4jAL.executeQuery(toFlag, Map.of("pattern", bestMatch.getFullName()));

		String taxonomy = String.format("%s##%2$s##%2$s", DetectorPropertyUtil.getDefaultTaxonomy(), bestMatch.getName());
		// Flag the node as Core logic of the application
		int numFlagged = 0;
		while(flagNodes.hasNext()) {
			Node n = (Node) flagNodes.next().get("node");
			DetectorPropertyUtil.applyArtemisProperties(neo4jAL, n, DetectionCategory.UNKNOWN_UTILITY, taxonomy, bestMatch.getName(), "Internal framework");
			numFlagged ++;
		}

		neo4jAL.logInfo(String.format("The logic core seems to be located under '%s' has been extracted. " +
				"%d nodes have been flagged.", bestMatch, numFlagged));
	}

	@Override
	public void getUtilities() throws Exception {
		throw new Exception("Method not implemented yet");
	}
}
