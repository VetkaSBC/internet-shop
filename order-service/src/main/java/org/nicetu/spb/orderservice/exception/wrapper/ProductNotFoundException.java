package org.nicetu.spb.orderservice.exception.wrapper;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String message) {
        super(message);
    }
}
