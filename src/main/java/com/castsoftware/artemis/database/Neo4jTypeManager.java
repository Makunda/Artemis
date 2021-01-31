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

package com.castsoftware.artemis.database;

import org.neo4j.graphdb.Node;

public class Neo4jTypeManager {

    /**
     * Get the value of a parameter as Double. If another value is detected, it will re-write the parameter of the node.
     * @param node Node containing the value to be extracted
     * @param property Name of the property
     * @return Value of the return
     */
    public static Double getAsDouble(Node node, String property) {
        Double val = .0;
        if(!node.hasProperty(property)) {
            node.setProperty(property, val);
            return val;
        }

        try {
            val = (Double) node.getProperty(property);
            return val;
        } catch (ClassCastException ignored) {
            // Ignored
        }

        try {
            Float aFloat = (Float) node.getProperty(property);
            val = aFloat.doubleValue();
            node.setProperty(property, val);
            return val;
        } catch (ClassCastException ignored) {
            // Ignored
        }

        try {
            Long aLong = (Long) node.getProperty(property);
            val = aLong.doubleValue();
            node.setProperty(property, val);
        } catch (ClassCastException ignored) {
            // Ignored
        }

        try {
            Integer aInteger = (Integer) node.getProperty(property);
            val = aInteger.doubleValue();
            node.setProperty(property, val);
            return val;
        } catch (ClassCastException ignored) {
            // Ignored
        }

        String aString = (String) node.getProperty(property);
        val = Double.parseDouble(aString);
        node.setProperty(property, val);
        return val;
    }

    /**
     * Get the value of a parameter as Long. If another value is detected, it will re-write the parameter of the node.
     * @param node Node containing the value to be extracted
     * @param property Name of the property
     * @return Value of the return
     */
    public static Long getAsLong(Node node, String property) {
        Long val = 0L;
        if(!node.hasProperty(property)) {
            node.setProperty(property, val);
            return val;
        }

        try {
            val = (Long) node.getProperty(property);
            return val;
        } catch (ClassCastException ignored) {
            // Ignored
        }

        try {
            Float aFloat = (Float) node.getProperty(property);
            val = aFloat.longValue();
            node.setProperty(property, val);
            return val;
        } catch (ClassCastException ignored) {
            // Ignored
        }

        try {
            Double aDouble = (Double) node.getProperty(property);
            val = aDouble.longValue();
            node.setProperty(property, val);
        } catch (ClassCastException ignored) {
            // Ignored
        }

        try {
            Integer aInteger = (Integer) node.getProperty(property);
            val = aInteger.longValue();
            node.setProperty(property, val);
            return val;
        } catch (ClassCastException ignored) {
            // Ignored
        }

        String aString = (String) node.getProperty(property);
        val = Long.parseLong(aString);
        node.setProperty(property, val);
        return val;
    }


}
