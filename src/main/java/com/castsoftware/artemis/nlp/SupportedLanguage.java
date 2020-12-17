package com.castsoftware.artemis.nlp;

public enum SupportedLanguage {
    COBOL("Cobol"),
    ALL("All");

    private String value;

    @Override
    public String toString() {
        return this.value;
    }

    /**
     * Get the Language based on the String provided
     * @param type
     * @return
     */
    public static SupportedLanguage getLanguage(String type) {
        for(SupportedLanguage ft : SupportedLanguage.values()) {
            if(type.equals(ft.toString())) {
                return ft;
            }
        }

        return ALL;
    }

    SupportedLanguage(String value) {
        this.value = value;
    }
}
