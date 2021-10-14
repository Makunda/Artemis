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

package com.castsoftware.artemis.modules.nlp;

public enum SupportedLanguage {
  COBOL("Cobol"),
  JAVA("Java"),
  NET("Net"),
  ALL("All");

  private final String value;

  SupportedLanguage(String value) {
    this.value = value;
  }

  /**
   * Check the existence of a language
   *
   * @param language Language to search
   * @return
   */
  public static Boolean has(String language) {
    return getLanguage(language) != ALL;
  }

  /**
   * Get the Language based on the String provided
   *
   * @param type
   * @return
   */
  public static SupportedLanguage getLanguage(String type) {
    for (SupportedLanguage ft : SupportedLanguage.values()) {
      if (type.toLowerCase().equals(ft.toString().toLowerCase())) {
        return ft;
      }
    }
    return ALL;
  }

  @Override
  public String toString() {
    return this.value;
  }
}
