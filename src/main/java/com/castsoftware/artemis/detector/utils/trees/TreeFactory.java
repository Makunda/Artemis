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

package com.castsoftware.artemis.detector.utils.trees;

import com.castsoftware.artemis.config.detection.LanguageProp;
import com.castsoftware.artemis.detector.utils.trees.cobol.CobolFrameworkTree;
import com.castsoftware.artemis.detector.utils.trees.java.JavaFrameworkTree;
import com.castsoftware.artemis.global.SupportedLanguage;
import org.neo4j.graphdb.Node;

import java.util.List;

public class TreeFactory {

	/**
	 * Create a JAVA tree
	 * @param nodeList List of nodes to insert
	 * @return A Java Tree
	 */
	public static JavaFrameworkTree createJavaTree(LanguageProp languageProp, List<Node> nodeList) {
		JavaFrameworkTree fmt = new JavaFrameworkTree(languageProp);
		fmt.recursiveObjectsInsert(nodeList);
		return fmt;
	}

	/**
	 * Create a Cobol tree
	 * @param nodeList List of nodes to insert
	 * @return A Cobol Tree
	 */
	public static CobolFrameworkTree createCobolTree(LanguageProp languageProp, List<Node> nodeList) {
		CobolFrameworkTree fmt = new CobolFrameworkTree(languageProp);
		fmt.recursiveObjectsInsert(nodeList);
		return fmt;
	}


	/**
	 * Create a tree from a list of nodes
	 * @param nodeList Node list to use
	 */
	public static ATree createTree(LanguageProp languageProp, List<Node> nodeList) {
		switch (languageProp.getReferenceLanguage()) {
			case JAVA:
				return createJavaTree(languageProp, nodeList);
			case COBOL:
				return createCobolTree(languageProp, nodeList);
			default:
				throw new Error(String.format("No option available for language '%s'.", languageProp.getName()));
		}
	}
}
