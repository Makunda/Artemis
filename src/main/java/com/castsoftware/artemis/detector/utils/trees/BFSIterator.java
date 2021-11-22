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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class BFSIterator implements Iterator<ALeaf>{

	private ATree tree;

	private ALeaf currentPosition;
	private List<ALeaf> nextItems;

	/**
	 * Build the iterator
	 * @param tree Tree to parse
	 */
	public BFSIterator(ATree tree) {
		this.tree = tree;
		this.currentPosition = tree.getRoot();

		// Initialize the list of items
		this.nextItems = new ArrayList<>();
		this.nextItems.addAll(this.currentPosition.getChildren());
	}

	@Override
	public boolean hasNext() {
		return this.nextItems.size() > 0;
	}

	@Override
	public ALeaf next() {
		if(this.nextItems.size() == 0) return null;

		// Get the first Item and add add children to list
		this.currentPosition = this.nextItems.remove(0);
		this.nextItems.addAll(this.currentPosition.getChildren());

		return this.currentPosition;
	}

	@Override
	public void remove() {
		return; // Not implemented
	}

	@Override
	public void forEachRemaining(Consumer<? super ALeaf> action) {
		return; // Not implemented
	}
}
