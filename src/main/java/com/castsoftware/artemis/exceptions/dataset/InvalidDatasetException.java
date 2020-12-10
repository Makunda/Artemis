package com.castsoftware.artemis.exceptions.dataset;

import com.castsoftware.artemis.exceptions.ExtensionException;

public class InvalidDatasetException extends ExtensionException {
    private static final long serialVersionUID = 5538686331898382229L;

    private static final String MESSAGE_PREFIX = "Error, the dataset seems to be corrupted : ";
    private static final String CODE_PREFIX = "DATS_CR_";

    public InvalidDatasetException(String message, String path, Throwable cause, String code) {
        super(MESSAGE_PREFIX.concat(message).concat(". Path : ").concat(path), cause, CODE_PREFIX.concat(code));
    }

    public InvalidDatasetException(String message, String code) {
        super(MESSAGE_PREFIX.concat(message), CODE_PREFIX.concat(code));
    }

    public InvalidDatasetException(String path, Throwable cause, String code) {
        super(MESSAGE_PREFIX.concat(path), cause, CODE_PREFIX.concat(code));
    }
}
