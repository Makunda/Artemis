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
import org.neo4j.graphdb.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public abstract class ATree {

  protected LanguageProp languageProp;

  public abstract String getDelimiterLeaves();

  /**
   * Initialize a tree
   * @param languageProp Language of the tree
   */
  public ATree(LanguageProp languageProp) {
    this.languageProp = languageProp;
  }

  public abstract ALeaf getRoot();

  public abstract void print();

  /**
   * Insert recursively a list of node in the tree
   * @param nodeList List of node to insert
   */
  public abstract void recursiveObjectsInsert(List<Node> nodeList);

  /**
   * Get a slice of the tree for a specific depth
   * @param depth Depth of the slice
   * @return The list of Framework Leaf
   */
  public List<ALeaf> getSliceByDepth(int depth) {
    List<ALeaf> returnList = new ArrayList<>();

    // Verify the validity of the tree
    ALeaf root = this.getRoot();
    if(root == null ) {
      return returnList; // Null root if tree not initialized, return
    }

    // Iterate through the tree
    List<ALeaf> iterationList = new ArrayList<>(root.getChildren());
    ALeaf element;
    while(!iterationList.isEmpty()) {
      element = iterationList.remove(0);
      int elementDepth = element.getDepth();

      if(elementDepth == depth) {
        // Check depth, if correct add to functional module
        returnList.add(element);
      } else if(elementDepth < depth) {
        // If depth superior continue the investigation
        iterationList.addAll(element.getChildren());
      }
      // Otherwise if depth lower than, continue
    }

    return returnList;
  }

  /**
   * Flatten a tree in a list of ALeaf with a reference on their parents
   *
   * @return The List of leaf in the tree
   */
  public List<ALeaf> flatten() {
    long id = 0L;
    List<ALeaf> leafList = new ArrayList<>();
    leafList.add(getRoot());

    // Assign IDs
    for (ListIterator<ALeaf> iLeaf = leafList.listIterator(); iLeaf.hasNext(); ) {
      ALeaf aLeaf = iLeaf.next();
      aLeaf.setId(id);

      for (ALeaf c : aLeaf.getChildren()) {
        // Only add if it's not the last leaf
        if (!c.getChildren().isEmpty()) {
          iLeaf.add(c); // Add children to list
          iLeaf.previous(); // Go one step back to iterate over it
        }
      }
      id += 1L;
    }

    // Assign parent id
    ALeaf temp;
    ALeaf aLeaf;
    for (ListIterator<ALeaf> iLeaf = leafList.listIterator(); iLeaf.hasNext(); ) {
      aLeaf = iLeaf.next();

      // Parse list and find parent
      for (ListIterator<ALeaf> it2 = leafList.listIterator(); it2.hasNext(); ) {
        temp = it2.next();
        if (temp.hasChild(aLeaf.getId())) {
          aLeaf.setParentId(temp.getId());
          break; // Exit, parent found
        }
      }
    }

    return leafList;
  }
}
