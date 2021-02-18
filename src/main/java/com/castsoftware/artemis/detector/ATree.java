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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public abstract class ATree {
  public abstract String getDelimiterLeaves();

  public List<ALeaf> flatten() {
    long id = 0L;
    List<ALeaf> leafList = new ArrayList<>();
    leafList.add(getRoot());

    this.print();

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

  public abstract ALeaf getRoot();

  public abstract void print();
}
