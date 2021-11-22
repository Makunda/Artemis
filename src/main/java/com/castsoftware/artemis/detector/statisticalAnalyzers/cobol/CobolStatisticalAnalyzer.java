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

package com.castsoftware.artemis.detector.statisticalAnalyzers.cobol;

import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.detector.statisticalAnalyzers.AStatisticalAnalyzer;
import com.castsoftware.artemis.detector.utils.DetectionCategory;
import com.castsoftware.artemis.detector.utils.DetectorNodesUtil;
import com.castsoftware.artemis.detector.utils.DetectorPropertyUtil;
import com.castsoftware.artemis.detector.utils.DetectorTypeMapper;
import com.castsoftware.artemis.detector.utils.trees.ALeaf;
import com.castsoftware.artemis.detector.utils.trees.ATree;
import com.castsoftware.artemis.detector.utils.trees.TreeFactory;
import com.castsoftware.artemis.detector.utils.trees.TreeUtil;
import com.castsoftware.artemis.detector.utils.trees.cobol.CobolFrameworkTree;
import com.castsoftware.artemis.detector.utils.trees.java.JavaFrameworkTree;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.global.SupportedLanguage;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CobolStatisticalAnalyzer extends AStatisticalAnalyzer {

	private static final double VARIATION_LIMIT = 30.0;
	private static final int MINIMUM_LENGTH = 3;

	/**
	 * Constructor
	 *
	 * @param neo4jAL     Neo4j Access Layer
	 * @param application Name of the application
	 */
	public CobolStatisticalAnalyzer(Neo4jAL neo4jAL, String application, SupportedLanguage language) {
		super(neo4jAL, application, language);
	}

	/**
	 * Log information message
	 * @param message to log
	 */
	private void log(String message) {
		neo4jAL.logInfo(String.format("COBOL STATISTICAL ANALYZER :: %s", message));
	}

	/**
	 * Log an error for the engine
	 * @param message Message to log
	 */
	private void error(String message) {
		neo4jAL.logError(String.format("COBOL STATISTICAL ANALYZER :: %s", message));
	}

	/**
	 * Log an error for the engine
	 * @param message Message to log
	 * @param err Error to log
	 */
	private void error(String message, Throwable err) {
		neo4jAL.logError(String.format("COBOL STATISTICAL ANALYZER :: %s", message), err);
	}

	/**
	 * Find the core of the application and return its associated leaf
	 * @param tree Tree to parse
	 * @return Leaf pointing on the core
	 */
	private Optional<ALeaf> findCore(ATree tree) {
		// Start on root and initialize list
		ALeaf currentPosition = tree.getRoot();

		// First step
		List<? extends ALeaf> toVisit = currentPosition.getChildren();
		Optional<ALeaf> core = TreeUtil.getBiggestALeaf(toVisit);

		if(core.isEmpty()) {
			// Nothing has been found
			error(String.format("Failed to discover the core of the application. " +
					"Empty initialization with first element having %d children.", toVisit.size()));
			return Optional.empty();
		}

		log(String.format("CORE INVESTIGATION :: Current cursor is under : %s (Children %d) ", core.get().getFullName(), core.get().getCount()));

		while(currentPosition != null) {
			// New pointer on core
			currentPosition = core.get();

			// Check variation on children and stop, or continue
			double variation = TreeUtil.getLeafVariation(currentPosition);

			log(String.format("CORE INVESTIGATION :: Current cursor is under : %s With a variation of %.2f . " +
					"(Children %d)", core.get().getFullName(), variation, core.get().getCount()));

			if(variation < VARIATION_LIMIT && currentPosition.getFullName().length() > MINIMUM_LENGTH) {
				// Stop the investigation and flag core
				return Optional.of(currentPosition);
			} else {
				// Investigate children
				Optional<ALeaf> newCurrent = TreeUtil.getBiggestALeaf(currentPosition.getChildren());
				if(newCurrent.isPresent()) {
					currentPosition = newCurrent.get();
				} else {
					return Optional.of(currentPosition); // end of tree
				}
			}
		}

		return Optional.empty();
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
		CobolFrameworkTree tree = TreeFactory.createCobolTree(languageProp, nodeList);
		tree.print();

		// Get Core of cobol application
		if(tree.getRoot() == null) {
			error("The internal cobol tree is empty. Stopping the investigation.");
			return;
		}

		// Find core and look for external cobol nodes with same starts
		Optional<ALeaf> core = this.findCore(tree);
		if(core.isEmpty()) {
			log("The search of the core returned no results. Analyzer will stop.");
			return;
		}

		// Flag the results
		ALeaf coreLeaf = core.get();
		log(String.format("Found the potential core of the application under %s ( with %d children ).",
				coreLeaf.getFullName(), coreLeaf.getCount()));

		// Create a Framework node from the detected node
		FrameworkNode fn = DetectorTypeMapper.fromFrameworkLeafToFrameworkNode(neo4jAL, core.get());
		fn.setFrameworkType(FrameworkType.TO_INVESTIGATE);
		fn.setDescription("This namespace has been automatically identified as a potential missing part of the application.\nPlease double check delivered files to investigate these missing items.");
		this.addFrameworkNode(fn);

	}

	@Override
	public void getUtilities() throws Exception {
		throw new Exception("Method not implemented yet");
	}

}
