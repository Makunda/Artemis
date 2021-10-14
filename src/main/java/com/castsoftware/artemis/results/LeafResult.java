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

package com.castsoftware.artemis.results;

import com.castsoftware.artemis.detector.utils.ALeaf;

import java.util.List;

public class LeafResult {
  public Long id;
  public String name;
  public String fullName;
  public Long parentId;
  public Long count;
  public String delimiter;
  public List<String> objectTypes;
  public List<String> levels;
  public List<String> modules;
  public List<String> subsets;

  public LeafResult(ALeaf aLeaf, String delimiter) {
    this.id = aLeaf.getId();
    this.name = aLeaf.getName();
    this.fullName = aLeaf.getFullName();
    this.count = aLeaf.getCount();
    this.parentId = aLeaf.getParentId();
    this.delimiter = delimiter;

    this.objectTypes = aLeaf.getObjectTypes();
    this.levels = aLeaf.getLevels();
    this.modules = aLeaf.getModules();
    this.subsets = aLeaf.getSubsets();
  }
}
