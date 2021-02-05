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

package com.castsoftware.artemis.pythia;

import com.castsoftware.artemis.controllers.api.CategoryController;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.CategoryNode;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import org.json.JSONObject;

import java.util.Date;

public class PythiaUtils {

    public static FrameworkNode JSONtoFramework(Neo4jAL neo4jAL, JSONObject frameworkJson) throws Neo4jQueryException, Neo4jBadNodeFormatException {
        String name = frameworkJson.getString("name");
        String discoveryDate = frameworkJson.getString("discoveryDate");
        String location = frameworkJson.getString("location");
        String description = frameworkJson.has("description") ? frameworkJson.getString("description") : "";
        String category = frameworkJson.has("category") ? frameworkJson.getString("category") : "";
        String internalType = frameworkJson.getString("internalType");
        Long numberOfDetection = frameworkJson.has("numberOfDetection") ? frameworkJson.getLong("numberOfDetection") : 0L;
        Double percentageDetection = frameworkJson.has("percentageOfDetection") ? frameworkJson.getDouble("percentageOfDetection") : .0;
        Long timestampCreation = frameworkJson.has("creationDate") ? frameworkJson.getLong("creationDate") : new Date().getTime();

        String frameworkTypeAsString = frameworkJson.getString("type");
        FrameworkType frameworkType = FrameworkType.getType(frameworkTypeAsString);

        FrameworkNode fn =
                new FrameworkNode(
                        neo4jAL,
                        name,
                        discoveryDate,
                        location,
                        description,
                        numberOfDetection,
                        percentageDetection,
                        timestampCreation);
        CategoryNode cn = CategoryController.getOrCreateByName(neo4jAL, category);
        fn.setCategory(cn);
        fn.setInternalType(internalType);
        fn.setFrameworkType(frameworkType);

        return fn;
    }
}
