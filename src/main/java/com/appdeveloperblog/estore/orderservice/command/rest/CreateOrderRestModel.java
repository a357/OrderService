package com.appdeveloperblog.estore.orderservice.command.rest;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateOrderRestModel {
    private final String productId;
    private final String addressId;
    private final int quantity;
}

