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

package com.castsoftware.artemis.detector;

import java.util.List;

public abstract class ALeaf {
  protected Long id;
  protected Long parentId;
  protected String name;

  public ALeaf(String name) {
    this.id = -1L;
    this.parentId = -1L;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public Boolean hasChild(Long id) {
    if (id < 0) return false; // Id cannot be negative

    for (ALeaf leaf : this.getChildren()) {
      if (leaf.getId() == id) return true;
    }

    return false;
  }

  public abstract List<? extends ALeaf> getChildren();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }
}
