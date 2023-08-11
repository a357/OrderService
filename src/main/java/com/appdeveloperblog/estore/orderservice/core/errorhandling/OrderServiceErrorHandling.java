package com.appdeveloperblog.estore.orderservice.core.errorhandling;

import org.axonframework.commandhandling.CommandExecutionException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class OrderServiceErrorHandling {

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<?> handleException(Exception exception) {
        var error = new ErrorMessage(LocalDateTime.now(), exception.getMessage());
        return new ResponseEntity<>(error, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }



    @ExceptionHandler(value = CommandExecutionException.class)
    public ResponseEntity<?> handleCommandExecutionException(CommandExecutionException exception) {
        var error = new ErrorMessage(LocalDateTime.now(), exception.getMessage());
        return new ResponseEntity<>(error, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
