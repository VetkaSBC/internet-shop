package org.nicetu.spb.userservice.exception.wrapper;

public class PasswordNotFoundException extends RuntimeException {
    public PasswordNotFoundException(String message) {
        super(message);
    }
}
