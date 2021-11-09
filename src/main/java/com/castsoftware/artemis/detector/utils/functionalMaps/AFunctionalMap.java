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

package com.castsoftware.artemis.detector.utils.functionalMaps;

import com.castsoftware.artemis.config.detection.LanguageProp;
import com.castsoftware.artemis.detector.utils.trees.ATree;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.neo4j.Neo4jAL;
import org.neo4j.graphdb.Result;

import java.util.*;

public abstract class AFunctionalMap {

	protected List<AFunctionalModule> modules;
	protected Integer treeDepth;
	protected LanguageProp languageProp;

	/**
	 * Get the list of modules in the map
	 * @return
	 */
	public List<AFunctionalModule> getModules() {
		return modules;
	}

	/**
	 * Get a module in the map using its identifier
	 * @param identifier Identifier of the module
	 * @return Optional returning the module if found
	 */
	public Optional<AFunctionalModule> getModuleByIdentifier(String identifier) {
		for(AFunctionalModule fm : this.modules) {
			if(fm.getIdentifier().equals(identifier)) return Optional.of(fm);
		}
		return Optional.empty();
	}

	/**
	 * Get the position in the module list of a module
	 * @param identifier Identifier of the module
	 * @return The position or -1
	 */
	private Integer getModuleIndexById(String identifier) {
		int position = 0;
		for(AFunctionalModule fm : this.modules) {
			if(fm.getIdentifier().equals(identifier)) return position;
			position++;
		}

		return -1;
	}

	/**
	 * Get the level of the map
	 * @return
	 */
	public Integer getTreeDepth() {
		return treeDepth;
	}

	/**
	 * Initialize the map from a list of nodes
	 * @param tree Tree used for initialization
	 */
	public abstract void initializeMap(ATree tree, Neo4jAL neo4jAL) throws Neo4jQueryException;


	public void computeLinks(Neo4jAL neo4jAL) throws Neo4jQueryException {
		// Build the index ( Passed by value, so we need to rebind afterward )
		Map<Long, String> indexMap = new HashMap<>();
		for(AFunctionalModule module : this.modules) {
			for(Long idNode : module.getNodeIdList()) {
				indexMap.put(idNode, module.getIdentifier());
			}
		}

		// Discover Callees
		String requestTo = "UNWIND $idList as idObject " +
				"MATCH (o:Object)-[]->(oth:Object) WHERE id(o)=idObject AND NOT ID(oth) IN $idList " +
				"RETURN DISTINCT ID(oth) as otherId;";

		// Discover Callers
		String requestFrom = "UNWIND $idList as idObject " +
				"MATCH (o:Object)<-[]-(oth:Object) WHERE id(o)=idObject AND NOT ID(oth) IN $idList " +
				"RETURN DISTINCT ID(oth) as otherId;";


		// Populate the modules with callers and callees
		this.modules.forEach(x -> {
			try {
				Long nodeId;
				String moduleIdentifier;

				// Populate the callees
				Result resultCallees = neo4jAL.executeQuery(requestTo, Map.of("idList", x.getNodeIdList()));
				while (resultCallees.hasNext()) {
					nodeId = (Long) resultCallees.next().get("otherId");
					moduleIdentifier = indexMap.get(nodeId);
					if(moduleIdentifier!= null && !moduleIdentifier.equals(x.getIdentifier())) {
						x.addCallee(moduleIdentifier);
					}
				}

				// Populate the callers
				Result resultCallers = neo4jAL.executeQuery(requestFrom, Map.of("idList",x.getNodeIdList()));
				while (resultCallers.hasNext()) {
					nodeId = (Long) resultCallers.next().get("otherId");
					moduleIdentifier = indexMap.get(nodeId);
					if(moduleIdentifier!= null && !moduleIdentifier.equals(x.getIdentifier())) {
						x.addCaller(moduleIdentifier);
					}
				}


			} catch (Neo4jQueryException e) {
				neo4jAL.logError(String.format("Failed to relink the modules [%s]", x.getIdentifier()), e);
			}
		});


	}

	/**
	 * Create a functional map
	 * @param level Level of the functional map
	 * @param languageProp Properties of the languages
	 */
	public AFunctionalMap(Integer level, LanguageProp languageProp) {
		this.modules = new ArrayList<>();
		this.treeDepth = level;
		this.languageProp = languageProp;
	}
}
