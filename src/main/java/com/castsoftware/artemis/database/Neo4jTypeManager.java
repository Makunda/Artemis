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

import java.util.Arrays;
import java.util.List;

public class Neo4jTypeManager {

  /**
   * Get the value of a parameter as Boolean. If another type of value is detected, it will re-write the
   * parameter of the node with `False`.
   *
   * @param node Node containing the value to be extracted
   * @param property Name of the property
   * @return Value of the return
   */
  public static Boolean getAsBoolean(Node node, String property) {
    if (!node.hasProperty(property)) {
      node.setProperty(property, false);
      return false;
    }

    String val = String.valueOf(node.getProperty(property));
    return Boolean.parseBoolean(val);
  }

  /**
   * Get the value of a parameter as Double. If another value is detected, it will re-write the
   * parameter of the node.
   *
   * @param node Node containing the value to be extracted
   * @param property Name of the property
   * @return Value of the return
   */
  public static Double getAsDouble(Node node, String property) {
    Double val = .0;
    if (!node.hasProperty(property)) {
      node.setProperty(property, val);
      return val;
    }
    Object valObject = node.getProperty(property);

    if (valObject instanceof Double) {
      return (Double) valObject;
    }

    if (valObject instanceof Float) {
      Float aFloat = (Float) node.getProperty(property);
      val = aFloat.doubleValue();
      node.setProperty(property, val);
      return val;
    }

    if (valObject instanceof Long) {
      Long aLong = (Long) node.getProperty(property);
      val = aLong.doubleValue();
      node.setProperty(property, val);
    }

    if (valObject instanceof Integer) {
      Integer aInteger = (Integer) node.getProperty(property);
      val = aInteger.doubleValue();
      node.setProperty(property, val);
      return val;
    }

    if (valObject instanceof String) {
      String aString = (String) node.getProperty(property);
      val = Double.parseDouble(aString);
      node.setProperty(property, val);
    }

    return val;
  }

  /**
   * Get the value of a parameter as Long. If another value is detected, it will re-write the
   * parameter of the node.
   *
   * @param node Node containing the value to be extracted
   * @param property Name of the property
   * @return Value of the return
   */
  public static Long getAsLong(Node node, String property) {
    Long val = 0L;
    if (!node.hasProperty(property)) {
      node.setProperty(property, val);
      return val;
    }
    Object valObject = node.getProperty(property);
    if (valObject instanceof Long) {
      val = (Long) valObject;
      return val;
    }

    if (valObject instanceof Float) {
      Float aFloat = (Float) valObject;
      val = aFloat.longValue();
      node.setProperty(property, val);
      return val;
    }

    if (valObject instanceof Double) {
      Double aDouble = (Double) valObject;
      val = aDouble.longValue();
      node.setProperty(property, val);
    }

    if (valObject instanceof Integer) {
      Integer aInteger = (Integer) valObject;
      val = aInteger.longValue();
      node.setProperty(property, val);
      return val;
    }

    if (valObject instanceof String) {
      String aString = (String) valObject;
      val = Long.parseLong(aString);
      node.setProperty(property, val);
    }

    return val;
  }

  public static List<String> getAsStringList(Node node, String property) {
    String[] strings = new String[0];
    if (!node.hasProperty(property)) {
      node.setProperty(property, strings);
      return Arrays.asList(strings);
    }

    Object valObject = node.getProperty(property);

    if (valObject instanceof String[]) {
      strings = (String[]) valObject;
      return Arrays.asList(strings);
    }

    if (valObject instanceof String) {
      strings = Arrays.asList((String) valObject).toArray(new String[0]);
      node.setProperty(property, strings);
      return Arrays.asList(strings);
    }

    return Arrays.asList(strings);
  }
}
