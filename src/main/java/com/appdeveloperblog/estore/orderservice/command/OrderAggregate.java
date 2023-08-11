package com.appdeveloperblog.estore.orderservice.command;

import com.appdeveloperblog.estore.orderservice.command.commands.ApproveOrderCommand;
import com.appdeveloperblog.estore.orderservice.command.commands.CreateOrderCommand;
import com.appdeveloperblog.estore.orderservice.command.commands.RejectOrderCommand;
import com.appdeveloperblog.estore.orderservice.core.enums.OrderStatus;
import com.appdeveloperblog.estore.orderservice.core.events.OrderApprovedEvent;
import com.appdeveloperblog.estore.orderservice.core.events.OrderCreatedEvent;
import com.appdeveloperblog.estore.orderservice.core.events.OrderRejectEvent;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

@Aggregate
public class OrderAggregate {
    @AggregateIdentifier
    private String orderId;
    private String userId;
    private String productId;
    private int quantity;
    private String addressId;
    private OrderStatus orderStatus;

    public OrderAggregate() {
    }

    @CommandHandler
    public OrderAggregate(CreateOrderCommand createOrderCommand) {
        OrderCreatedEvent event = new OrderCreatedEvent();
        BeanUtils.copyProperties(createOrderCommand, event);
        AggregateLifecycle.apply(event);
    }

    @CommandHandler
    public void handle(ApproveOrderCommand approveOrderCommand) {
        AggregateLifecycle.apply(new OrderApprovedEvent(approveOrderCommand.getOrderId()));//2
    }

    @CommandHandler
    public void handle(RejectOrderCommand rejectOrderCommand) {
        var orderRejectEvent = new OrderRejectEvent(rejectOrderCommand.getOrderId(), rejectOrderCommand.getReason());
        AggregateLifecycle.apply(orderRejectEvent);
    }

    @EventSourcingHandler
    public void on(OrderCreatedEvent orderCreatedEvent) {
        this.orderId = orderCreatedEvent.getOrderId();
        this.userId = orderCreatedEvent.getUserId();
        this.productId = orderCreatedEvent.getProductId();
        this.quantity = orderCreatedEvent.getQuantity();
        this.addressId = orderCreatedEvent.getAddressId();
        this.orderStatus = orderCreatedEvent.getOrderStatus();
    }

    @EventSourcingHandler
    public void on(OrderApprovedEvent orderApprovedEvent) {
        this.orderStatus = orderApprovedEvent.getStatus();
    }

    @EventSourcingHandler
    public void on(OrderRejectEvent orderRejectEvent) {
        this.orderStatus = orderRejectEvent.getStatus();
    }
}
