package com.castsoftware.artemis.nlp;

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

    private static final Map<SupportedLanguage, String> languageMap =Map.ofEntries(
            entry(SupportedLanguage.COBOL, COBOL_KEYWORDS)
    );

    /**
     * Get the keywords related to the languages
     * @param language
     * @return Keywords as a list
     */
    public static List<String> getKeywords(SupportedLanguage language){
        String keywords = languageMap.get(language);
        return Arrays.asList(keywords.split(DEFAULT_DELIMITER));
    }

    /**
     * Get the number of Keyword matches
     * @param language Language selected
     * @param text Text
     * @return
     */
    public static int getNumMatchKeywords(SupportedLanguage language, String text) {
        String pattern = languageMap.get(language);
        int i = 0;
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher( text );
        while (m.find()) {
            i++;
        }

        return i;
    }
}
