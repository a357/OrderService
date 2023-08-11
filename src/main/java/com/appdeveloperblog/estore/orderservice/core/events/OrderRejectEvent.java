package com.appdeveloperblog.estore.orderservice.core.events;

import com.appdeveloperblog.estore.orderservice.core.enums.OrderStatus;
import lombok.Value;

@Value
public class OrderRejectEvent {
    String orderId;
    String reason;
    OrderStatus status = OrderStatus.REJECTED;
}
