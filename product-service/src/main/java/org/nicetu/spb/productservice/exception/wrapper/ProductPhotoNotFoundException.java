package org.nicetu.spb.productservice.exception.wrapper;

import java.io.Serial;

public class ProductPhotoNotFoundException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ProductPhotoNotFoundException(String message) {
        super(message);
    }
}
