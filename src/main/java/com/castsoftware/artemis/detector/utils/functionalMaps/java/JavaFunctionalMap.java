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

package com.castsoftware.artemis.detector.utils.functionalMaps.java;

import com.castsoftware.artemis.config.detection.LanguageProp;
import com.castsoftware.artemis.detector.utils.functionalMaps.AFunctionalMap;
import com.castsoftware.artemis.detector.utils.functionalMaps.AFunctionalModule;
import com.castsoftware.artemis.detector.utils.trees.ALeaf;
import com.castsoftware.artemis.detector.utils.trees.ATree;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.neo4j.Neo4jAL;

import java.util.ArrayList;
import java.util.List;

public class JavaFunctionalMap extends AFunctionalMap {

	/**
	 * Create a java functional map
	 *
	 * @param level        Level of the functional map
	 * @param languageProp Properties of the languages
	 */
	public JavaFunctionalMap(Integer level, LanguageProp languageProp) {
		super(level, languageProp);
	}

	/**
	 * Convert a leaf to a functional module and add it
	 * @param leaf
	 */
	public void addLeafToMap(ALeaf leaf) {
		List<Long> idNodes = new ArrayList<>(leaf.getIdNodes());
		JavaFunctionalModule javaModule = new JavaFunctionalModule(leaf.getFullName(), idNodes);
		this.modules.add(javaModule);
	}

	/**
	 * Initialize the Functional Map using a package tree
	 * @param tree Tree used for initialization
	 * @param neo4jAL Neo4j Access Layer
	 * @throws Neo4jQueryException
	 */
	@Override
	public void initializeMap(ATree tree, Neo4jAL neo4jAL) throws Neo4jQueryException {
		// Get a slice of the tree and convert the leaves to functional modules
		List<ALeaf> treeSlice = tree.getSliceByDepth(this.treeDepth);
		treeSlice.forEach(this::addLeafToMap); // Convert and add leaves

		// Map has been initialized with node
		this.computeLinks(neo4jAL); // Compute the links
	}

}
