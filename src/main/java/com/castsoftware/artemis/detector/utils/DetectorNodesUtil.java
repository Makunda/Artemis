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

package com.castsoftware.artemis.detector.utils;

import com.castsoftware.artemis.config.detection.LanguageProp;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.modules.pythia.models.api.PythiaImagingFramework;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DetectorNodesUtil {

	/**
	 * Get the list of internal objects
	 * @param neo4jAL Neo4j Access layer
	 * @param languageProp Language property
	 * @param application Name of the application
	 * @return The list of internal objects in the application
	 */
	public static List<Node> getInternalObjects(Neo4jAL neo4jAL, LanguageProp languageProp, String application) throws Neo4jQueryException {
		List<String> categories = languageProp.getObjectsInternalType();
		List<Node> internalNodes = new ArrayList<>();

		String forgedRequest =
				String.format(
						"MATCH (obj:Object:`%s`) WHERE  obj.InternalType in $internalTypes AND obj.External=false RETURN obj as node",
						application);
		Map<String, Object> params = Map.of("internalTypes", categories);
		Result res = neo4jAL.executeQuery(forgedRequest, params);

		while (res.hasNext()) {
			Map<String, Object> resMap = res.next();
			Node node = (Node) resMap.get("node");
			internalNodes.add(node);
		}

		return internalNodes;
	}

	/**
	 * Get the list of external objects in the application
	 * @param neo4jAL Neo4j Access layer
	 * @param languageProp Language properties
	 * @param application Name of the application
	 * @return The list of external objects in the application
	 */
	public static List<Node> getExternalObjects(Neo4jAL neo4jAL, LanguageProp languageProp, String application) throws Neo4jQueryException {
		List<String> categories = languageProp.getObjectsInternalType();
		List<Node> externalNodes = new ArrayList<>();

		String forgedRequest =
				String.format(
						"MATCH (obj:Object:`%s`) WHERE  obj.InternalType in $internalTypes AND obj.External=false RETURN obj as node",
						application);
		Map<String, Object> params = Map.of("internalTypes", categories);
		Result res = neo4jAL.executeQuery(forgedRequest, params);

		while (res.hasNext()) {
			Map<String, Object> resMap = res.next();
			Node node = (Node) resMap.get("node");
			externalNodes.add(node);
		}

		return externalNodes;
	}


	/**
	 * Get the nodes by pattern and externality
	 * @param neo4jAL Neo4j Access Layer
	 * @param application Name of the application
	 * @param pattern Pattern name
	 * @param external External name
	 * @return The list of nodes matching the pattern and externality in the application
	 */
	public static  List<Node> getNodesByPatternAndExternality(Neo4jAL neo4jAL, String application, String pattern, Boolean external) throws Neo4jQueryException {
		String request =
				String.format(
						"MATCH (o:Object:`%s`) "
								+ "WHERE o.External=$external AND o.FullName STARTS WITH $pattern "
								+ "RETURN DISTINCT  o as node",
						application);
		Map<String, Object> params = Map.of("external", external, "pattern", pattern);
		try {
			Result results = neo4jAL.executeQuery(request, params);
			return  results.stream().map( x -> ((Node) x.get("node"))).collect(Collectors.toList());

		} catch (Neo4jQueryException e) {
			neo4jAL.logError(
					String.format("Failed to retrieve nodes with pattern %s.", pattern), e);
			throw e;
		}
	}

	/**
	 * Flag nodes with imaging framework
	 * @param pythiaFramework Pythia framework to use
	 * @param external
	 */
	public static void flagNodesWithImagingFramework(Neo4jAL neo4jAL, String application, PythiaImagingFramework pythiaFramework, Boolean external) {
		try {
			List<Node> toFlagNodes = DetectorNodesUtil.getNodesByPatternAndExternality(neo4jAL, application, pythiaFramework.getPattern(), external);
			int numFlagged = 0;
			for (Node n : toFlagNodes) {
				tagNodeWithImagingFramework(neo4jAL, n, pythiaFramework);
				numFlagged++;
			}

			neo4jAL.logInfo(
					String.format(
							"%d nodes have been identified by pattern '%s' as framework : %s",
							numFlagged, pythiaFramework.getPattern(), pythiaFramework.name));
		} catch (Neo4jQueryException e) {
			neo4jAL.logError(
					String.format("Failed to flag nodes using inputs pythia on pattern %s.", pythiaFramework.getPattern()), e);
		}
	}

	/**
	 * Tag a node with an Imaging Framework Node
	 * @param n Node to tag
	 * @param imagingFramework Imaging framework detected
	 */
	public static void tagNodeWithImagingFramework(Neo4jAL neo4jAL, Node n, PythiaImagingFramework imagingFramework) throws Neo4jQueryException {
		DetectorPropertyUtil.applyNodeProperty(n, DetectionCategory.KNOWN_UTILITY);
		DetectorPropertyUtil.applyTaxonomyProperty(n, imagingFramework.getFormattedTaxonomy());
		DetectorPropertyUtil.applyFrameworkName(neo4jAL, n, imagingFramework.getName());
		DetectorPropertyUtil.applyDescriptionProperty(neo4jAL, n, imagingFramework.getDescription());
	}
}
