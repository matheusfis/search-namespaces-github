package com.matheusfischer.githubnamespacescanner.domain.exception;

public final class ApplicationConfigurationException extends RuntimeException {

    public ApplicationConfigurationException(String message) {
        super(message);
    }

    public ApplicationConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
