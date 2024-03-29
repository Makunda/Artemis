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

import com.castsoftware.artemis.detector.utils.trees.ALeaf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CobolFrameworkTreeLeaf extends ALeaf {

  private List<CobolFrameworkTreeLeaf> children;
  private double[] detectionResults;
  private Integer depth;
  private boolean framework;

  public CobolFrameworkTreeLeaf(String name, String fullName) {
    super(fullName, name);
    this.children = Collections.synchronizedList(new ArrayList<>());
    this.framework = false;
    this.detectionResults = new double[0];
    this.depth = 0;
  }

  public CobolFrameworkTreeLeaf(CobolFrameworkTreeLeaf clone) {
    super(clone.fullName, clone.name);
    this.children = clone.getChildren();
    this.framework = clone.framework;
    this.detectionResults = clone.getDetectionResults();
    this.depth = clone.getDepth();
  }

  public double[] getDetectionResults() {
    return detectionResults;
  }

  public void setDetectionResults(double[] detectionResults) {
    this.detectionResults = detectionResults;
  }

  public Integer getDepth() {
    return depth;
  }

  public void setDepth(Integer depth) {
    this.depth = depth;
  }

  public boolean isFramework() {
    return framework;
  }

  public void setFramework(boolean framework) {
    this.framework = framework;
  }

  public String getName() {
    return name;
  }

  public List<CobolFrameworkTreeLeaf> getChildren() {
    return children;
  }

  public void addLeaf(CobolFrameworkTreeLeaf leaf) {
    this.children.add(leaf);
  }

  public void removeChildLeafByName(String toRemove) {
    List<CobolFrameworkTreeLeaf> newChildren = new ArrayList<>();

    for(CobolFrameworkTreeLeaf l : this.children) {
      if(!l.getFullName().equals(toRemove)) newChildren.add(l);
    }

    this.children = newChildren;
  }

  /**
   * Merge the list of nodes between two leaf
   * @param leaf Leaf to merge
   */
  public void mergeNodes(CobolFrameworkTreeLeaf leaf) {
    this.idNodes.addAll(leaf.getIdNodes());
  }

  public String getChildrenAsString() {
    return this.children.stream().map(ALeaf::getFullName).collect(Collectors.joining(", "));
  }

  public void resetChildren() {
    this.children = new ArrayList<>();
  }
}
