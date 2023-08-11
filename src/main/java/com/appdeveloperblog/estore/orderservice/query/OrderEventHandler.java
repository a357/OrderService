package com.appdeveloperblog.estore.orderservice.query;

import com.appdeveloperblog.estore.orderservice.command.commands.ApproveOrderCommand;
import com.appdeveloperblog.estore.orderservice.core.Groups;
import com.appdeveloperblog.estore.orderservice.core.entity.OrderEntity;
import com.appdeveloperblog.estore.orderservice.core.events.OrderApprovedEvent;
import com.appdeveloperblog.estore.orderservice.core.events.OrderCreatedEvent;
import com.appdeveloperblog.estore.orderservice.core.events.OrderRejectEvent;
import com.appdeveloperblog.estore.orderservice.core.repository.OrderRepository;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.interceptors.ExceptionHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ProcessingGroup(Groups.ORDER_GROUP)
public class OrderEventHandler {
    private final OrderRepository repository;

    public OrderEventHandler(OrderRepository repository) {
        this.repository = repository;
    }

    @ExceptionHandler(resultType = Exception.class)
    public void handleException(Exception exception) throws Exception {
        throw exception;
    }

    @EventHandler
    public void on(OrderCreatedEvent orderCreatedEvent) {
        OrderEntity entity = new OrderEntity();
        BeanUtils.copyProperties(orderCreatedEvent, entity);
        repository.save(entity);
    }

    @EventHandler
    public void on(OrderApprovedEvent orderApprovedEvent) {
        var order = repository.findByOrderId(orderApprovedEvent.getOrderId());

        if (order == null) {
            // TODO do something else
            return;
        }

        order.setOrderStatus(orderApprovedEvent.getStatus());
        repository.save(order);
    }

    @EventHandler
    public void on(OrderRejectEvent orderRejectEvent) {
        var order = repository.findByOrderId(orderRejectEvent.getOrderId());
        order.setOrderStatus(orderRejectEvent.getStatus());
        repository.save(order);
    }
}
