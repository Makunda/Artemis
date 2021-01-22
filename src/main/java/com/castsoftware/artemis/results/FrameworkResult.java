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

import com.castsoftware.artemis.datasets.FrameworkNode;

public class FrameworkResult {
    public String name;
    public String description;
    public String category;
    public String type;
    public String internalType = "";
    public String location = "";
    public String discoveryDate = "";
    public Double percentageOfDetection = .0;

    public FrameworkResult(String name, String description, String category, String type) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.type = type;
    }

    public FrameworkResult(FrameworkNode fn) {
        this.name = fn.getName();
        this.category = fn.getCategory();
        this.description = fn.getDescription();
        this.type = fn.getCategory();
        this.internalType = fn.getInternalType();
        this.location = fn.getInternalType();
        this.discoveryDate = fn.getDiscoveryDate();
        this.percentageOfDetection = fn.getPercentageDetection();
    }
}
