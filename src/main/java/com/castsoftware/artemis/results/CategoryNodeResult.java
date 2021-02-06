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

import com.castsoftware.artemis.datasets.CategoryNode;

public class CategoryNodeResult {
  public Long id;
  public String name;
  public String iconUrl;

  public CategoryNodeResult(CategoryNode n) {
    assert n.getNode() != null : "Cannot create a CategoryNodeResult from a non-initialized node";
    this.id = n.getId();
    this.name = n.getName();
    this.iconUrl = n.getIconUrl();
  }
}
