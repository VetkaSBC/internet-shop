package org.nicetu.spb.userservice.exception.wrapper;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
