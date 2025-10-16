package org.nicetu.spb.orderservice.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicetu.spb.orderservice.exception.payload.ExceptionMessage;
import org.nicetu.spb.orderservice.exception.wrapper.CartNotFoundException;
import org.nicetu.spb.orderservice.exception.wrapper.JwtAuthenticationException;
import org.nicetu.spb.orderservice.exception.wrapper.OrderNotFoundException;
import org.nicetu.spb.orderservice.exception.wrapper.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@ControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(value = {
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            JwtAuthenticationException.class
    })
    public ResponseEntity<ExceptionMessage> handleValidationException(final Exception e) {
        log.info("**ApiExceptionHandler controller, handle validation exception*\n");

        String message;
        if (e instanceof MethodArgumentNotValidException methodArgException) {
            message = "*" + Objects.requireNonNull(methodArgException.getBindingResult().getFieldError()).getDefaultMessage() + "!**";
        } else if (e instanceof HttpMessageNotReadableException) {
            message = "*Invalid JSON format!**";
        } else {
            message = "*" + e.getMessage() + "!**";
        }

        return new ResponseEntity<>(
                ExceptionMessage.builder()
                        .message(message)
                        .httpStatus(HttpStatus.BAD_REQUEST)
                        .timestamp(ZonedDateTime.now(ZoneId.systemDefault()))
                        .build(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {
            CartNotFoundException.class,
            OrderNotFoundException.class,
            ProductNotFoundException.class
    })
    public ResponseEntity<ExceptionMessage> handleApiRequestException(final RuntimeException e) {
        log.info("**ApiExceptionHandler controller, handle API request*\n");

        return new ResponseEntity<>(
                ExceptionMessage.builder()
                        .message("#### " + e.getMessage() + "! ####")
                        .httpStatus(HttpStatus.BAD_REQUEST)
                        .timestamp(ZonedDateTime.now(ZoneId.systemDefault()))
                        .build(), HttpStatus.BAD_REQUEST);
    }
}