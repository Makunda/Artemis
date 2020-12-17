package com.castsoftware.artemis.datasets;

public enum FrameworkType {
    FRAMEWORK("Framework"),
    NOT_FRAMEWORK("NotFramework"),
    TO_INVESTIGATE("ToInvestigate"),
    NOT_KNOWN("NotKnown");

    private String value;

    @Override
    public String toString() {
        return this.value;
    }

    /**
     * Get the Framework type based on the String provided
     * @param type
     * @return
     */
    public static FrameworkType getType(String type) {
        for(FrameworkType ft : FrameworkType.values()) {
            if(type.equals(ft.toString())) {
                return ft;
            }
        }
        return NOT_KNOWN;
    }

    FrameworkType(String value) {
        this.value = value;
    }
}
