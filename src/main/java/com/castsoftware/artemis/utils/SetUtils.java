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

package com.castsoftware.artemis.utils;

import java.util.HashSet;
import java.util.Set;

public class SetUtils {

	// Function merging two sets using addAll()
	public static <T> Set<T> mergeSet(Set<T> a, Set<T> b)
	{

		// Creating an empty set
		Set<T> mergedSet = new HashSet<T>();

		// add the two sets to be merged
		// into the new set
		mergedSet.addAll(a);
		mergedSet.addAll(b);

		// returning the merged set
		return mergedSet;
	}

}
