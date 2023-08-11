package com.appdeveloperblog.estore.orderservice.core.errorhandling;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@AllArgsConstructor
@Data
public class ErrorMessage {
    private final LocalDateTime timestamp;
    private final String message;
}
