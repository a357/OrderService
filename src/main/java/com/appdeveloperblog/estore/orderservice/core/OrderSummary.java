package com.appdeveloperblog.estore.orderservice.core;

import com.appdeveloperblog.estore.orderservice.core.enums.OrderStatus;
import lombok.Value;

@Value
public class OrderSummary {
    String orderId;
    OrderStatus orderStatus;
    String message;
}
