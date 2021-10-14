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

import com.castsoftware.artemis.config.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Map.entry;

public class KeywordsManager {

  private static final String COBOL_KEYWORDS = Configuration.get("artemis.cobol.keywords");
  private static final String DEFAULT_DELIMITER = "|";

  private static final Map<SupportedLanguage, String> languageMap =
      Map.ofEntries(entry(SupportedLanguage.COBOL, COBOL_KEYWORDS));

  /**
   * Get the keywords related to the languages
   *
   * @param language
   * @return Keywords as a list
   */
  public static List<String> getKeywords(SupportedLanguage language) {
    String keywords = languageMap.get(language);
    return Arrays.asList(keywords.split(DEFAULT_DELIMITER));
  }

  /**
   * Get the number of Keyword matches
   *
   * @param language Language selected
   * @param text Text
   * @return
   */
  public static int getNumMatchKeywords(SupportedLanguage language, String text) {
    if (!languageMap.containsKey(language)) return 0;

    String pattern = languageMap.get(language);
    int i = 0;
    Pattern p = Pattern.compile(pattern);
    Matcher m = p.matcher(text);
    while (m.find()) {
      i++;
    }

    return i;
  }
}
