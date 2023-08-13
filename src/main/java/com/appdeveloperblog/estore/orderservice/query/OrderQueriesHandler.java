package com.appdeveloperblog.estore.orderservice.query;

import com.appdeveloperblog.estore.orderservice.core.OrderSummary;
import com.appdeveloperblog.estore.orderservice.core.repository.OrderRepository;
import org.apache.logging.log4j.util.Strings;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class OrderQueriesHandler {
    OrderRepository repository;

    public OrderQueriesHandler(OrderRepository repository) {
        this.repository = repository;
    }

    @QueryHandler
    public OrderSummary findOrder(FindOrderQuery findOrderQuery) {
        var orderEntity = repository.findByOrderId(findOrderQuery.getOrderId());
        return new OrderSummary(orderEntity.getOrderId(), orderEntity.getOrderStatus(), Strings.EMPTY);
    }
}
