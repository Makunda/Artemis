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

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.database.Neo4jTypeManager;
import org.neo4j.graphdb.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class ALeaf {

  protected static final String IMAGING_OBJECT_TAGS =
          Configuration.get("imaging.link.object_property.tags");
  protected static final String IMAGING_OBJECT_NAME = Configuration.get("imaging.node.object.name");

  protected static final String IMAGING_LEVEL_PROP = Configuration.get("imaging.node.object.level");
  protected static final String IMAGING_MODULE_PROP = Configuration.get("imaging.node.object.module");
  protected static final String IMAGING_SUBSET_PROP = Configuration.get("imaging.node.object.subset");


  protected static final String IMAGING_OBJECT_FULL_NAME =
          Configuration.get("imaging.node.object.fullName");
  protected static final String IMAGING_APPLICATION_LABEL =
          Configuration.get("imaging.application.label");
  protected static final String IMAGING_INTERNAL_TYPE =
          Configuration.get("imaging.application.InternalType");


  protected Long id;
  protected Long parentId;

  protected Long count;
  protected String name;
  protected String fullName;

  /** Imaging properties **/
  protected Set<String> objectTypes = new HashSet<>();
  protected Set<String> levels = new HashSet<>();
  protected Set<String> modules = new HashSet<>();
  protected Set<String> subset = new HashSet<>();

  public ALeaf(String fullName, String name) {
    this.id = -1L;
    this.parentId = -1L;
    this.name = name;
    this.fullName = fullName;
    this.count = 0L;
  }

  public void addOneChild() {
    this.count += 1;
  }

  public void addObjectType(String objectType) {
    this.objectTypes.add(objectType);
  }

  public List<String> getObjectTypes() {
    return new ArrayList<>(this.objectTypes);
  }

  public void setObjectTypes(Set<String> types) {
    this.objectTypes = types;
  }

  public void addLevel(String objectType) {
    this.levels.add(objectType);
  }

  public List<String> getLevels() {
    return new ArrayList<>(this.levels);
  }

  public List<String> getModules() {
    return new ArrayList<>(this.modules);
  }

  public List<String> getSubsets() {
    return new ArrayList<>(this.subset);
  }

  /**
   * Add a list of modules to the Leaf
   * @param modules Modules to add
   */
  public void addModules(List<String> modules) {
    this.modules.addAll(modules);
  }

  /**
   * Add a list of subset to the Leaf
   * @param subsets Subset to add
   */
  public void addSubset(List<String> subsets) {
    this.subset.addAll(subsets);
  }

  public void setLevels(Set<String> types) {
    this.levels = types;
  }

  public Long getCount() {
    return count;
  }

  public void setCount(Long count) {
    this.count = count;
  }

  public String getName() {
    return name;
  }

  public String getFullName() {
    return fullName;
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
      if (leaf.getId().equals(id)) return true;
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

  /**
   * Process the node in the leaf
   * @param n Node to add in the leaf
   */
  public void addNode(Node n) {

    if (n.hasProperty(IMAGING_INTERNAL_TYPE))
    {
      String internalType = (String) n.getProperty(IMAGING_INTERNAL_TYPE);
      this.addObjectType(internalType);
    }

    if(n.hasProperty(IMAGING_LEVEL_PROP)) {
      String level = (String) n.getProperty(IMAGING_LEVEL_PROP);
      this.addLevel(level);
    }

    if(n.hasProperty(IMAGING_MODULE_PROP)) {
      List<String> modules = Neo4jTypeManager.getAsStringList(n, IMAGING_MODULE_PROP);
      this.addModules(modules);
    }

    if(n.hasProperty(IMAGING_SUBSET_PROP)) {
      List<String> subset = Neo4jTypeManager.getAsStringList(n, IMAGING_MODULE_PROP);
      this.addSubset(subset);
    }

  }
}
