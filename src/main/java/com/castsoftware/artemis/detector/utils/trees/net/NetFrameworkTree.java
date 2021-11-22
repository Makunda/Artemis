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

package com.castsoftware.artemis.detector.utils.trees.net;

import com.castsoftware.artemis.config.detection.LanguageProp;
import com.castsoftware.artemis.detector.utils.trees.ATree;
import org.neo4j.graphdb.Node;

import java.util.List;

public class NetFrameworkTree extends ATree {


  private final NetFrameworkTreeLeaf root;

  public NetFrameworkTree(LanguageProp languageProp) {
    super(languageProp);
    this.root = new NetFrameworkTreeLeaf("", "");
  }

  /**
   * Recursively insert the package in the tree
   *
   * @param leaf Leaf to insert the package
   * @param remainingPackage Name of the package to insert
   */
  private void recInsert(
          NetFrameworkTreeLeaf leaf, String remainingPackage, String fullName, Node n, Integer depth) {

    String[] splitPackageName = remainingPackage.split("\\.", 2);

    // If the split contains for than one element continue
    String name = splitPackageName[0];

    if (fullName.isEmpty()) {
      fullName = name;
    } else {
      fullName = String.join(".", fullName, name);
    }

    NetFrameworkTreeLeaf matchingLeaf = null;

    // Check if a package already exist or create it
    for (NetFrameworkTreeLeaf clf : leaf.getChildren()) {
      if (clf.getName().equals(name)) {
        matchingLeaf = clf; // Matching package found
        break;
      }
    }

    // If a matching leaf wasn't found, create a new one
    if (matchingLeaf == null) {
      matchingLeaf = new NetFrameworkTreeLeaf(name, fullName);
      // Add the leaf to the tree
      leaf.addLeaf(matchingLeaf);
    }

    matchingLeaf.setDepth(depth);
    matchingLeaf.addNode(n);

    if (splitPackageName.length > 1) {
      recInsert(matchingLeaf, splitPackageName[1], fullName, n, depth + 1);
    }
  }

  /**
   * Insert a package in the tree
   *
   * @param packageName Full name of the package to insert
   */
  public void insert(String packageName, Node node) {
    this.recInsert(root, packageName, "", node, 0);
  }

  public String getDelimiterLeaves() {
    return "\\.";
  }

  /**
   * Get the root of the tree
   *
   * @return Get the root
   */
  public NetFrameworkTreeLeaf getRoot() {
    return root;
  }

  /** Print the tree */
  public void print() {
    printTree(root, 0);
  }

  /**
   * Insert the nodes in the tree recursively 
   * @param nodeList List of node to insert
   */
  @Override
  public void recursiveObjectsInsert(List<Node> nodeList) {
    // Create a framework tree
    String fullName;
    for (Node n : nodeList) {
      fullName = n.getProperty("FullName").toString();
      this.insert(fullName, n);
    }
  }

  /**
   * Recursive tree print
   *
   * @param fl Leaf to display
   * @param level Level to use for shifting
   */
  private void printTree(NetFrameworkTreeLeaf fl, int level) {
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
    for (NetFrameworkTreeLeaf clf : fl.getChildren()) {
      printTree(clf, level + 1);
    }
  }
}
