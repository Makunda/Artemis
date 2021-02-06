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

package com.castsoftware.artemis.controllers.api;

import com.castsoftware.artemis.config.LanguageConfiguration;
import com.castsoftware.artemis.config.LanguageProp;

import java.util.List;
import java.util.stream.Collectors;

public class LanguageController {

  /**
   * Get the list of the languages by Artemis
   *
   * @return
   */
  public static List<String> getSupportedLanguages() {
    LanguageConfiguration lc = LanguageConfiguration.getInstance();
    return lc.getLanguageMap().values().stream()
        .map(LanguageProp::getName)
        .collect(Collectors.toList());
  }
}
