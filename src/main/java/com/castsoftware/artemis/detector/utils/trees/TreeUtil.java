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

import java.util.List;
import java.util.Optional;

public class TreeUtil {

	/**
	 * Get the biggest leaf in a slice of the tree
	 * @param tree Tree to investigate
	 * @param depth Depth to investigate
	 */
	public static Optional<ALeaf> getBiggestModule(ATree tree, int depth) {
		List<ALeaf> slice = tree.getSliceByDepth(depth);
		if(slice.size() == 0) return Optional.empty(); // No element in the slice

		return getBiggestALeaf(slice);
	}

	/**
	 * Get the biggest leaf in a list
	 * @param slice List of nodes to explore
	 * @return The biggest leaf found in the list or Optional.empty()
	 */
	public static Optional<ALeaf> getBiggestALeaf(List<? extends ALeaf> slice) {
		if(slice.size() == 0) return Optional.empty(); // No element in the slice

		long biggestLeafSize = 0;
		ALeaf leaf = null;

		// Look for biggest leaf
		for(ALeaf itLeaf : slice) {
			System.out.printf("Aleaf %s contains %d children.", itLeaf.getFullName(), itLeaf.getCount());
			if(biggestLeafSize < itLeaf.getCount()) {
				leaf = itLeaf;
				biggestLeafSize = itLeaf.getCount();
			}
		}

		// Check if it exist
		return (leaf == null) ? Optional.empty() : Optional.of(leaf);
	}

	/**
	 * Get the difference of size between a leaf and its biggest child
	 * @param leaf Leaf to investigate
	 * @return
	 */
	public static double getLeafVariation(ALeaf leaf) {
		if(leaf == null) return 0.0; // Null leaf

		// Explore the children and relative size
		Optional<ALeaf> biggestChild = getBiggestALeaf(leaf.getChildren());
		if(biggestChild.isEmpty()) return 0.0; // No leaf beyond current candidate;

		// Variation percentage
		double percentage =  ((double) biggestChild.get().getCount() - (double) leaf.getCount()) / (double) biggestChild.get().getCount() ;
		return Math.abs(percentage);
	}

}
