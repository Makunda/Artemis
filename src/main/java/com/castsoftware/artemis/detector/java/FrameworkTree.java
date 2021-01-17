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

package com.castsoftware.artemis.detector.java;

public class FrameworkTree {

  private static String PACKAGE_DELIMITER = "\\.";

  private FrameworkTreeLeaf root;

  /**
   * Recursively insert the package in the tree
   *
   * @param leaf Leaf to insert the package
   * @param packageName Name of the package to insert
   */
  private void recInsert(FrameworkTreeLeaf leaf, String packageName) {
    String[] splitPackageName = packageName.split(PACKAGE_DELIMITER, 2);

    // If the split contains for than one element continue

    String name = splitPackageName[0];
    FrameworkTreeLeaf matchingLeaf = null;

    // Check if a package already exist or create it
    for (FrameworkTreeLeaf clf : leaf.getChildren()) {
      if (clf.getName().equals(name)) {
        matchingLeaf = clf; // Matching package found
        break;
      }
    }

    // If a matching leaf wasn't found, create a new one
    if (matchingLeaf == null) {
      matchingLeaf = new FrameworkTreeLeaf(name);
      // Add the leaf to the tree
      leaf.addLeaf(matchingLeaf);
    }

    if (splitPackageName.length > 1) {
      recInsert(matchingLeaf, splitPackageName[1]);
    }
  }

  /**
   * Insert a package in the tree
   *
   * @param packageName Full name of the package to insert
   */
  public void insert(String packageName) {
    this.recInsert(root, packageName);
  }

  /**
   * Recursive tree print
   *
   * @param fl
   * @param level
   */
  private void printTree(FrameworkTreeLeaf fl, int level) {
    System.out.print("|" + "__".repeat(level) + " : " + fl.getName() + "\n");
    for (FrameworkTreeLeaf clf : fl.getChildren()) {
      printTree(clf, level + 1);
    }
  }

  /** Print the tree */
  public void print() {
    printTree(root, 0);
  }

  public FrameworkTree() {
    this.root = new FrameworkTreeLeaf("");
  }
}
