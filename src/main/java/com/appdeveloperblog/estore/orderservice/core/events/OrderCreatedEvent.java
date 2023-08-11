package com.appdeveloperblog.estore.orderservice.core.events;

import com.appdeveloperblog.estore.orderservice.core.enums.OrderStatus;
import lombok.Data;

@Data
public class OrderCreatedEvent{
    private String orderId;
    private String userId;
    private String productId;
    private int quantity;
    private String addressId;
    private OrderStatus orderStatus;
}
