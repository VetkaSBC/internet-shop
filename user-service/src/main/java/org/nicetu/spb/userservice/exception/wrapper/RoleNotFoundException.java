package org.nicetu.spb.userservice.exception.wrapper;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(String message) {
        super(message);
    }
}
