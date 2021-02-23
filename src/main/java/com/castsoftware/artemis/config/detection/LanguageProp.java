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

package com.castsoftware.artemis.config.detection;

import java.util.List;

public class LanguageProp {
  String name;
  Boolean onlineSearch = false;
  Boolean interactionDetector = false;
  String packageDelimiter = "";
  List<String> repositorySearch;
  List<String> objectsInternalType;
  String modelFileName;

  public LanguageProp() {}

  public LanguageProp(
      String name,
      Boolean onlineSearch,
      Boolean interactionDetector,
      String packageDelimiter,
      List<String> repositorySearch,
      List<String> objectsInternalType,
      String modelFileName) {
    this.name = name;
    this.onlineSearch = onlineSearch;
    this.interactionDetector = interactionDetector;
    this.packageDelimiter = packageDelimiter;
    this.repositorySearch = repositorySearch;
    this.objectsInternalType = objectsInternalType;
    this.modelFileName = modelFileName;
  }

  public String getName() {
    return name;
  }

  public Boolean getOnlineSearch() {
    return onlineSearch;
  }

  public List<String> getObjectsInternalType() {
    return objectsInternalType;
  }

  public Boolean getInteractionDetector() {
    return interactionDetector;
  }

  public String getPackageDelimiter() {
    return packageDelimiter;
  }

  public List<String> getRepositorySearch() {
    return repositorySearch;
  }

  public String getModelFileName() {
    return modelFileName;
  }

  @Override
  public String toString() {
    return "LanguageProp{"
        + "name='"
        + name
        + '\''
        + ", onlineSearch="
        + onlineSearch
        + ", interactionDetector="
        + interactionDetector
        + ", packageDelimiter='"
        + packageDelimiter
        + '\''
        + ", repositorySearch="
        + String.join(" - ", repositorySearch)
        + ", objectsInternalType="
        + objectsInternalType
        + ", modelFileName='"
        + modelFileName
        + '\''
        + '}';
  }
}
