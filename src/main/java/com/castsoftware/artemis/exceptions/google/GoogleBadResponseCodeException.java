package com.castsoftware.artemis.exceptions.google;

import com.castsoftware.artemis.exceptions.ExtensionException;

public class GoogleBadResponseCodeException  extends ExtensionException  {
    private static final long serialVersionUID = -257478375644618244L;
    private static final String MESSAGE_PREFIX = "Error returned by Neo4j API : ";
    private static final String CODE_PREFIX = "GOO_BR_";

    public GoogleBadResponseCodeException(String message, Throwable cause, String code) {
        super(MESSAGE_PREFIX.concat(message), cause, CODE_PREFIX.concat(code));
    }

    public GoogleBadResponseCodeException(String message, String code) {
        super(MESSAGE_PREFIX.concat(message), CODE_PREFIX.concat(code));
    }
}
