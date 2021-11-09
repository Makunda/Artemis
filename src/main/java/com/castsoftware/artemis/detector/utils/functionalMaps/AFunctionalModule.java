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

import org.neo4j.graphdb.Node;

import java.util.List;

public abstract class AFunctionalModule {
	private String identifier;
	private List<Long> nodeIdList;
	private List<String> callers;
	private List<String> callees;

	public AFunctionalModule(String identifier, List<Long> nodeIdList) {
		this.identifier = identifier;
		this.nodeIdList = nodeIdList;
	}

	/**
	 * Get identifier ( Name in general ) of the functional module
	 * @return The identifier
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Get the list of node in this module
	 * @return
	 */
	public List<Long> getNodeIdList() {
		return nodeIdList;
	}

	/**
	 * Get the list of callers
	 * @return
	 */
	public List<String> getCallers() {
		return callers;
	}
	public void addCaller(String identifier) {
		this.callers.add(identifier);
	}

	/**
	 * Get the list of Callees
	 * @return
	 */
	public List<String> getCallees() {
		return callees;
	}
	public void addCallee(String identifier) {
		this.callees.add(identifier);
	}
}
