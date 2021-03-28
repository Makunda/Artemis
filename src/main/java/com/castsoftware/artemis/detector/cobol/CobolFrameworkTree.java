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

package com.castsoftware.artemis.detector.cobol;

import com.castsoftware.artemis.detector.ATree;
import org.neo4j.graphdb.Node;

public class CobolFrameworkTree extends ATree {

  private static String PACKAGE_DELIMITER = "";

  private CobolFrameworkTreeLeaf root;

  public CobolFrameworkTree() {
    this.root = new CobolFrameworkTreeLeaf("", "");
  }

  /**
   * Recursively insert the package in the tree
   *
   * @param leaf Leaf to insert the package
   * @param fullName Name of the program to be inserted
   */
  private void recInsert(
          CobolFrameworkTreeLeaf leaf, String fullName, Node n, Integer depth) {

    try {
      // Ignore fullName under 3
      if(fullName.length() < 2 + depth) return;
      // If the split contains for than one element continue

      String name = fullName.substring(0, 2 + depth);
      CobolFrameworkTreeLeaf matchingLeaf = null;

      // Check if a package already exist or create it
      for (CobolFrameworkTreeLeaf clf : leaf.getChildren()) {
        if (clf.getName().equals(name)) {
          matchingLeaf = clf; // Matching package found
          break;
        }
      }

      // If a matching leaf wasn't found, create a new one
      if (matchingLeaf == null) {
        matchingLeaf = new CobolFrameworkTreeLeaf(name, fullName);
        // Add the leaf to the tree
        leaf.addLeaf(matchingLeaf);
      }

      matchingLeaf.setDepth(depth);
      matchingLeaf.addOneChild();
      matchingLeaf.addNode(n);


      recInsert(matchingLeaf, fullName, n, depth + 1);
    } catch (Exception e) {
      // Ignore
    }
  }

  /**
   * Insert a package in the tree
   *
   * @param fullName Name of the program to  insert
   */
  public void insert(String fullName, Node n) {
    this.recInsert(root, fullName, n, 1);
  }

  public String getDelimiterLeaves() {
    return PACKAGE_DELIMITER;
  }

  /**
   * Get the root of the tree
   *
   * @return
   */
  public CobolFrameworkTreeLeaf getRoot() {
    return root;
  }

  /** Print the tree */
  public void print() {

  }

  /**
   * Recursive tree print
   *
   * @param fl
   * @param level
   */
  private void printTree(CobolFrameworkTreeLeaf fl, int level) {

  }
}
