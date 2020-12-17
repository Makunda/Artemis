package com.castsoftware.artemis.exceptions.nlp;

import com.castsoftware.artemis.exceptions.ExtensionException;

public class NLPBlankInputException extends ExtensionException  {
    private static final long serialVersionUID = 8218353918930322258L;
    private static final String MESSAGE_PREFIX = "Error, the input is not valid : ";
    private static final String CODE_PREFIX = "NLP_BI_";

    public NLPBlankInputException(String message, Throwable cause, String code) {
        super(MESSAGE_PREFIX.concat(message), cause, CODE_PREFIX.concat(code));
    }

    public NLPBlankInputException(String message, String code) {
        super(MESSAGE_PREFIX.concat(message), CODE_PREFIX.concat(code));
    }
}
