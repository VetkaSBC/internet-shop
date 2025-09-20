package org.nicetu.spb.userservice.exception.wrapper;

public class EmailNotFoundException extends RuntimeException {
    public EmailNotFoundException() {
        super();
    }

    public EmailNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailNotFoundException(String message) {
        super(message);
    }

    public EmailNotFoundException(Throwable cause) {
        super(cause);
    }
}
