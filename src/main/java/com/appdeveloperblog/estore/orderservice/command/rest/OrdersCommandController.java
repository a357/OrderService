package com.appdeveloperblog.estore.orderservice.command.rest;

import com.appdeveloperblog.estore.orderservice.command.commands.CreateOrderCommand;
import jakarta.validation.Valid;
import org.axonframework.commandhandling.gateway.CommandGateway;
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

    @PostMapping
    public String create(@Valid @RequestBody CreateOrderRestModel model) {
        var createOrderCommand = CreateOrderCommand.builder()
                .orderId(UUID.randomUUID().toString())
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
        return commandGateway.sendAndWait(createOrderCommand);
    }
}
