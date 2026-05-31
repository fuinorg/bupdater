package org.fuin.bupdater.core;

public class CoreException extends RuntimeException {

    public CoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public CoreException(String message) {
        super(message);
    }
}
