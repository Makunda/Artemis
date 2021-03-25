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

import com.castsoftware.artemis.detector.ALeaf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CobolFrameworkTreeLeaf extends ALeaf {

  private String fullName;
  private List<CobolFrameworkTreeLeaf> children;
  private double[] detectionResults;
  private Integer depth;
  private boolean framework;

  public CobolFrameworkTreeLeaf(String name, String fullName) {
    super(name);
    this.fullName = fullName;
    this.children = Collections.synchronizedList(new ArrayList<>());
    this.framework = false;
    this.detectionResults = new double[0];
    this.depth = 0;
  }

  public String getFullName() {
    return fullName;
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
}
