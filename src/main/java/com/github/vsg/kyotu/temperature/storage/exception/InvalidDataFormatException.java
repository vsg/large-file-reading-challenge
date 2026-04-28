package com.github.vsg.kyotu.temperature.storage.exception;

public class InvalidDataFormatException extends RuntimeException {

    public InvalidDataFormatException(String message) {
        this(message, null);
    }

    public InvalidDataFormatException(String message, Throwable cause) {
        super(formatMessage(message, cause), cause);
    }
    
    private static String formatMessage(String message, Throwable cause) {
        if (cause == null) {
            return message;
        }
        return String.format("%s. Cause: [%s: %s]", message, cause.getClass(), cause.getMessage());
    }

}
