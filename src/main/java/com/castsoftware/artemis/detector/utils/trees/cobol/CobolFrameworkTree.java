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

package com.castsoftware.artemis.detector.utils.trees.cobol;

import com.castsoftware.artemis.config.detection.LanguageProp;
import com.castsoftware.artemis.detector.utils.trees.ALeaf;
import com.castsoftware.artemis.detector.utils.trees.ATree;
import com.castsoftware.artemis.detector.utils.trees.java.JavaFrameworkTreeLeaf;
import org.neo4j.graphdb.Node;

import java.util.List;

public class CobolFrameworkTree extends ATree {

  private static final String PACKAGE_DELIMITER = "";

  private final CobolFrameworkTreeLeaf root;

  public CobolFrameworkTree(LanguageProp languageProp) {
    super(languageProp);
    this.root = new CobolFrameworkTreeLeaf("", "");
  }

  /**
   * Get the greatest common prefix between two string
   * @param a String A
   * @param b String B
   * @return the longest prefix found
   */
  private String greatestCommonPrefix(String a, String b) {
    int minLength = Math.min(a.length(), b.length());
    for (int i = 0; i < minLength; i++) {
      if (a.charAt(i) != b.charAt(i)) {
        return a.substring(0, i);
      }
    }
    return a.substring(0, minLength);
  }

  /**
   * Recursively insert the package in the tree
   *
   * @param leaf Leaf to insert the package
   * @param fullName Name of the program to be inserted
   */
  private void recInsert(CobolFrameworkTreeLeaf leaf, String fullName, Node n, Integer depth) {
    try {

      // Check if a package already exist or create it
      int longestPrefix = 0;
      String longestCommonPrefix = "";

      String commonPrefix = "";
      CobolFrameworkTreeLeaf matchingLeaf = null;

      // Get best match
      for (CobolFrameworkTreeLeaf clf : leaf.getChildren()) {
        commonPrefix = this.greatestCommonPrefix(clf.getFullName(), fullName);
        if(longestPrefix < commonPrefix.length() ) {
          matchingLeaf = clf;
          longestPrefix = commonPrefix.length();
          longestCommonPrefix = commonPrefix;
        }
      }

      CobolFrameworkTreeLeaf newLeaf = new CobolFrameworkTreeLeaf(fullName, fullName);
      newLeaf.addNode(n);
      newLeaf.setDepth(depth + 1);

      // If no match add it to the current leaf
      // If a matching leaf wasn't found, create a new one
      if (matchingLeaf == null) {
        // Add the leaf to the tree
        leaf.addNode(n);
        leaf.addLeaf(newLeaf);
      } else if ( matchingLeaf.getName().equals(fullName) || leaf.getName().equals(longestCommonPrefix)) {
        matchingLeaf.addNode(n);
      } else if( matchingLeaf.getName().equals(longestCommonPrefix)) {
        matchingLeaf.addNode(n);
        recInsert(matchingLeaf, fullName, n, depth + 1); // Continue to insert
      } else {
        // Update leaf
        matchingLeaf.setDepth(depth + 1);

        // Split the leaf in 2 part and create new node
        CobolFrameworkTreeLeaf toInsert = new CobolFrameworkTreeLeaf(longestCommonPrefix, longestCommonPrefix);
        toInsert.setDepth(depth);
        toInsert.addLeaf(newLeaf);
        toInsert.addLeaf(matchingLeaf);

        // Merge properties
        toInsert.mergeNodes(matchingLeaf);
        toInsert.mergeNodes(newLeaf);

        // Modify the leaf
        leaf.removeChildLeafByName(matchingLeaf.getFullName());
        leaf.addLeaf(toInsert);
      }
    } catch (Exception e) {
      // Failed
    }
  }

  /**
   * Insert a package in the tree
   *
   * @param fullName Name of the program to insert
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
  public void print() {}

  @Override
  public void recursiveObjectsInsert(List<Node> nodeList) {
    // Create a framework tree
    String fullName;
    for (Node n : nodeList) {
      fullName = n.getProperty("Name").toString();
      this.insert(fullName, n); // Insert name
    }
  }

  /**
   * Recursive tree print
   *
   * @param fl
   * @param level
   */
  private void printTree(CobolFrameworkTreeLeaf fl, int level) {
    System.out.print(
            "|"
                    + "__".repeat(level)
                    + " : "
                    + fl.getName()
                    + "  ::  "
                    + fl.getDepth()
                    + " :: Num children "
                    + fl.getCount()
                    + "\n");
    for (CobolFrameworkTreeLeaf clf : fl.getChildren()) {
      printTree(clf, level + 1);
    }
  }

}
