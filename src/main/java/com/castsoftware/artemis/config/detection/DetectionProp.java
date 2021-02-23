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

package com.castsoftware.artemis.config.detection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DetectionProp {

	private List<String> knownUtilities = new ArrayList<>();

	private List<String> potentiallyMissing  = new ArrayList<>();

	private List<String> inOtherApplication = new ArrayList<>();

	private List<String> unknownUtilities = new ArrayList<>();

	private List<String> unknownNonUtilities = new ArrayList<>();

	private List<String> knownNotUtilities = new ArrayList<>();

	private List<String> patternFullNameToExclude = new ArrayList<>();
	private List<String> patternObjectType = new ArrayList<>();

	@JsonProperty("arrangement")
	private void unpackArrangement(Map<String, Object> arrangement) {
		try {
			knownUtilities = (List<String>) arrangement.get("known_utilities");
		} catch (ClassCastException e) {
			System.err.println("Failed to get the value of arrangement.known_utilities");
			System.err.println(e.getMessage());
		}

		try {
			potentiallyMissing = (List<String>) arrangement.get("potentially_missing_code");
		} catch (ClassCastException e) {
			System.err.println("Failed to get the value of arrangement.potentially_missing_code");
			System.err.println(e.getMessage());
		}

		try {
			unknownNonUtilities = (List<String>) arrangement.get("unknown_non_utilities");
		} catch (ClassCastException e) {
			System.err.println("Failed to get the value of arrangement.unknown_non_utilities");
			System.err.println(e.getMessage());
		}

		try {
			inOtherApplication = (List<String>) arrangement.get("in_other_applications");
		} catch (ClassCastException e) {
			System.err.println("Failed to get the value of arrangement.in_other_applications");
			System.err.println(e.getMessage());
		}

		try {
			unknownUtilities = (List<String>) arrangement.get("unknown_utilities");
		} catch (ClassCastException e) {
			System.err.println("Failed to get the value of arrangement.unknown_utilities");
			System.err.println(e.getMessage());
		}

		try {
			knownNotUtilities = (List<String>) arrangement.get("known_not_utilities");
		} catch (ClassCastException e) {
			System.err.println("Failed to get the value of arrangement.known_not_utilities");
			System.err.println(e.getMessage());
		}

	}

	@JsonProperty("to_exclude")
	private void unpackToExclude(Map<String, Object> arrangement) {
		try {
			patternFullNameToExclude = (List<String>) arrangement.get("regex_object_fullName");
		} catch (ClassCastException e) {
			System.err.println("Failed to get the value of to_exclude.regex_object_fullName");
			System.err.println(e.getMessage());
		}

		try {
			patternObjectType = (List<String>) arrangement.get("regex_object_type");
		} catch (ClassCastException e) {
			System.err.println("Failed to get the value of to_exclude.regex_object_type");
			System.err.println(e.getMessage());
		}

	}

	public List<String> getKnownUtilities() {
		return knownUtilities;
	}

	public List<String> getPotentiallyMissing() {
		return potentiallyMissing;
	}

	public List<String> getInOtherApplication() {
		return inOtherApplication;
	}

	public List<String> getUnknownUtilities() {
		return unknownUtilities;
	}

	public List<String> getKnownNotUtilities() {
		return knownNotUtilities;
	}

	public List<String> getPatternFullNameToExclude() {
		return patternFullNameToExclude;
	}

	public List<String> getPatternObjectTypeToExclude() {
		return patternObjectType;
	}

	public List<String> getUnknownNonUtilities() {
		return unknownNonUtilities;
	}

	public DetectionProp(){

	}
}
