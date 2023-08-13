package com.appdeveloperblog.estore.orderservice.command.rest;

import com.appdeveloperblog.estore.orderservice.command.commands.CreateOrderCommand;
import com.appdeveloperblog.estore.orderservice.core.OrderSummary;
import com.appdeveloperblog.estore.orderservice.query.FindOrderQuery;
import io.axoniq.axonserver.connector.query.SubscriptionQueryResult;
import jakarta.validation.Valid;
import org.axonframework.commandhandling.gateway.CommandGateway;
import static org.axonframework.messaging.responsetypes.ResponseTypes.*;

import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrdersCommandController {
    public static final String USER_ID = "27b95829-4f3f-4ddf-8983-151ba010e35b";

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private QueryGateway queryGateway;

    @PostMapping
    public OrderSummary create(@Valid @RequestBody CreateOrderRestModel model) {

        String orderId = UUID.randomUUID().toString();
        var createOrderCommand = CreateOrderCommand.builder()
                .orderId(orderId)
                .userId(USER_ID)
                .productId(model.getProductId())
                .quantity(model.getQuantity())
                .addressId(model.getAddressId())
                .orderStatus(null)
                .build();



        /**
         * use interceptor for check if such orderAlreadyExists
         * @links com.appdeveloperblog.estore.orderservice.orderservice.command.rest.interceptor.CreateOrderCommandInterceptor
         * */

        var responseType = instanceOf(OrderSummary.class);
        var query = new FindOrderQuery(orderId);
        try (var queryResult = queryGateway.subscriptionQuery(query, responseType, responseType)) {
            commandGateway.sendAndWait(createOrderCommand);
            return queryResult.updates().blockFirst();
        }
    }
}
