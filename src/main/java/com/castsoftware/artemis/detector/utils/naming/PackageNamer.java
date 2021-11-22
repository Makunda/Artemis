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

package com.castsoftware.artemis.detector.utils.naming;

import com.castsoftware.artemis.detector.utils.trees.ALeaf;
import com.castsoftware.artemis.global.SupportedLanguage;

public class PackageNamer {


	/**
	 * Capitalize the first letter
	 *
	 * @param name Name to capitalize
	 * @return Capitalized name
	 */
	private static String capitalizeFirstLetter(String name) {
		if (name == null || name.isBlank()) return "";

		try {
			return name.substring(0, 1).toUpperCase() + name.substring(1);
		} catch (Exception e) {
			return name;
		}
	}

	/**
	 * Get the Java name of a package
	 * @param frameworkLeaf Framework Leaf to flag
	 * @return The name of the Level 5
	 */
	public static String getJavaName(ALeaf frameworkLeaf) {
		try {
			String imagingName = "API ";

			String[] split = frameworkLeaf.getFullName().split("\\."); // Split on package name

			if (split.length == 0 || split.length == 1)
				return imagingName + frameworkLeaf.getFullName(); // Cannot split
			if (split.length == 2)
				return imagingName + " " + capitalizeFirstLetter(split[1]); // Only the Company name
			return imagingName + " " + capitalizeFirstLetter(split[1]) + " " + split[2];
		} catch (Exception ignored) {
			return frameworkLeaf.getFullName();
		}
	}

	/**
	 * Get the name of a .Net Package
	 * @param frameworkLeaf Framework leaf to flag
	 * @return The name of the Level 5
	 */
	public static String getNetName(ALeaf frameworkLeaf) {
		try {
			String imagingName = "API ";

			String[] split = frameworkLeaf.getFullName().split("\\."); // Split on package name

			if (split.length == 0 || split.length == 1)
				return imagingName + "" + frameworkLeaf.getFullName(); // Cannot split
			else return String.format("API %s %s", capitalizeFirstLetter(split[0]), split[1]); // Only the Company name
		} catch (Exception ignored) {
			return frameworkLeaf.getFullName();
		}
	}

	/**
	 * Get the Cobol Framework
	 * @param frameworkLeaf Framework leaf
	 * @return
	 */
	public static String getCobol(ALeaf frameworkLeaf) {
		return frameworkLeaf.getFullName();
	}

	/**
	 * Get name by language
	 * @param leaf Leaf to flag
	 * @param language Language
	 * @return Get the name of the package
	 */
	public static String getNameByLanguage(ALeaf leaf, SupportedLanguage language) {
		switch (language) {
			case JAVA:
				return getJavaName(leaf);
			case NET:
				return getNetName(leaf);
			case COBOL:
				return  getCobol(leaf);
			default:
				return leaf.getFullName();
		}
	}
}
