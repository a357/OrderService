package com.appdeveloperblog.estore.orderservice.command;

import com.appdeveloperblog.estore.orderservice.core.Groups;
import com.appdeveloperblog.estore.orderservice.core.entity.OrderLookupEntity;
import com.appdeveloperblog.estore.orderservice.core.events.OrderCreatedEvent;
import com.appdeveloperblog.estore.orderservice.core.repository.OrderLookupRepository;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ProcessingGroup(Groups.ORDER_GROUP)
public class OrderLookupEventHandler {
    private final OrderLookupRepository repository;

    @Autowired
    public OrderLookupEventHandler(OrderLookupRepository repository) {
        this.repository = repository;
    }

    @EventHandler
    public void on(OrderCreatedEvent event) {
        //repository.save(new OrderLookupEntity(event.getOrderId(), event.getProductId()));
    }
}
