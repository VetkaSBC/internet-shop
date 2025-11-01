package org.nicetu.spb.orderservice.exception.wrapper;

import java.io.Serial;

public class CartNotFoundException extends RuntimeException{
    @Serial
    private static final long serialVersionUID = 1L;

    public CartNotFoundException(String message) {
        super(message);
    }
}
