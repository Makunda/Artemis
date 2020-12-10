package com.castsoftware.artemis.exceptions.nlp;

import com.castsoftware.artemis.exceptions.ExtensionException;

/**
 * The <code>Neo4jNoResult</code> is thrown when the NLP Configuration is not correct.
 * Neo4jNoResult
 */
public class NLPIncorrectConfigurationException extends ExtensionException {

    private static final long serialVersionUID = 8218353918930322258L;
    private static final String MESSAGE_PREFIX = "Error, the configuration of the NLP workspace is not correct : ";
    private static final String CODE_PREFIX = "NLP_BC_";

    public NLPIncorrectConfigurationException(String message, Throwable cause, String code) {
        super(MESSAGE_PREFIX.concat(message), cause, CODE_PREFIX.concat(code));
    }

    public NLPIncorrectConfigurationException(String message, String code) {
        super(MESSAGE_PREFIX.concat(message), CODE_PREFIX.concat(code));
    }

}
