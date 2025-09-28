package org.nicetu.spb.productservice.exception.wrapper;

import java.io.Serial;

public class ProductPhotoNotFoundException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ProductPhotoNotFoundException() {
        super();
    }

    public ProductPhotoNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProductPhotoNotFoundException(String message) {
        super(message);
    }

    public ProductPhotoNotFoundException(Throwable cause) {
        super(cause);
    }
}
