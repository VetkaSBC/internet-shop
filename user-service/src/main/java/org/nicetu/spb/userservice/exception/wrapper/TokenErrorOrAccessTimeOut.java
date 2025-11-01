package org.nicetu.spb.userservice.exception.wrapper;

public class TokenErrorOrAccessTimeOut extends RuntimeException {
    public TokenErrorOrAccessTimeOut(String message) {
        super(message);
    }
}
