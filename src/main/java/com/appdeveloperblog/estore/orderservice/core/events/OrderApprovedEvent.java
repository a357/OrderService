package com.appdeveloperblog.estore.orderservice.core.events;

import com.appdeveloperblog.estore.orderservice.core.enums.OrderStatus;
import lombok.Value;

@Value
public class OrderApprovedEvent {
    private final String orderId;
    private final OrderStatus status = OrderStatus.APPROVED;
}
