package org.nicetu.spb.userservice.exception.wrapper;

public class EmailNotFoundException extends RuntimeException {
    public EmailNotFoundException(String message) {
        super(message);
    }
}
