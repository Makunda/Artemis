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
import com.castsoftware.artemis.detector.utils.DetectorTypeMapper;
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
	 * Log information message
	 * @param message to log
	 */
	private void log(String message) {
		neo4jAL.logInfo(String.format("JAVA STATISTICAL ANALYZER :: %s", message));
	}

	/**
	 * Log an error for the engine
	 * @param message Message to log
	 */
	private void error(String message) {
		neo4jAL.logError(String.format("JAVA STATISTICAL ANALYZER :: %s", message));
	}

	/**
	 * Log an error for the engine
	 * @param message Message to log
	 * @param err Error to log
	 */
	private void error(String message, Throwable err) {
		neo4jAL.logError(String.format("JAVA STATISTICAL ANALYZER :: %s", message), err);
	}

	/**
	 * Get the core of the application based on the size of the packages
	 * @throws Neo4jQueryException
	 */
	@Override
	public void flagCore() throws Neo4jQueryException {
		log("Starting the analyzer.");
		// Get the list of object to treat
		List<Node> nodeList = DetectorNodesUtil.getInternalObjects(neo4jAL, languageProp, applicationName);
		log(String.format("%d have been identified to build the module map.", nodeList.size()));

		// Create a treeD
		JavaFrameworkTree tree = TreeFactory.createJavaTree(languageProp, nodeList);

		// Parse the tree slice on level 3 ( company )
		ALeaf bestMatch = null;
		Integer biggestBranch = 0;
		List<ALeaf> treeSlice = tree.getSliceByDepth(1);
		log(String.format("%d modules have been identified.", treeSlice.size()));

		for (ALeaf treeLeaf : treeSlice) {
			if (treeLeaf.getCount() >= biggestBranch) {
				biggestBranch = treeLeaf.getCount().intValue();
				bestMatch = treeLeaf;
			}
		}

		if(bestMatch == null) {
			error("Failed to discover the core logic of the application. The functional module construction failed.");
			return;
		}

		// Get name of the company
		log(String.format("The logic core seems to be located under '%s'." +
				"Extraction of external class with the same name will start.", bestMatch.getFullName()));

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
			DetectorPropertyUtil.applyArtemisProperties(neo4jAL, n, DetectionCategory.MISSING_CODE, taxonomy, bestMatch.getName(), "Internal framework");
			numFlagged ++;
		}

		log(String.format("The logic core seems to be located under '%s' has been extracted. " +
				"%d nodes have been flagged.", bestMatch.getFullName(), numFlagged));

		// Create a Framework node from the detected node
		FrameworkNode fn = DetectorTypeMapper.fromFrameworkLeafToFrameworkNode(neo4jAL, bestMatch);
		fn.setFrameworkType(FrameworkType.TO_INVESTIGATE);
		fn.setDescription("This namespace has been automatically identified as a potential missing part of the application.\nPlease double check delivered files to investigate these missing items.");
		this.addFrameworkNode(fn);
	}

	@Override
	public void getUtilities() throws Exception {
		throw new Exception("Method not implemented yet");
	}
}
