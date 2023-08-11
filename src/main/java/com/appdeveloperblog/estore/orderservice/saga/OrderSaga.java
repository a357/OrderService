package com.appdeveloperblog.estore.orderservice.saga;

import com.appdeveloperblog.estore.core.commands.CancelProductReservationCommand;
import com.appdeveloperblog.estore.core.commands.ProcessPaymentCommand;
import com.appdeveloperblog.estore.core.commands.ReserveProductCommand;
import com.appdeveloperblog.estore.core.events.PaymentProcessedEvent;
import com.appdeveloperblog.estore.core.events.ProductReservationCanceledEvent;
import com.appdeveloperblog.estore.core.events.ProductReservedEvents;
import com.appdeveloperblog.estore.core.model.User;
import com.appdeveloperblog.estore.core.query.FetchUserPaymentDetailsQuery;
import com.appdeveloperblog.estore.orderservice.command.commands.ApproveOrderCommand;
import com.appdeveloperblog.estore.orderservice.command.commands.RejectOrderCommand;
import com.appdeveloperblog.estore.orderservice.core.events.OrderApprovedEvent;
import com.appdeveloperblog.estore.orderservice.core.events.OrderCreatedEvent;
import com.appdeveloperblog.estore.orderservice.core.events.OrderRejectEvent;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Saga
public class OrderSaga {
    private static final String ASSOCIATION_PROPERTIES = "orderId";
    @Autowired
    private transient CommandGateway commandGateway;
    @Autowired
    private transient QueryGateway queryGateway;

    private final Logger LOGGER = LoggerFactory.getLogger(OrderSaga.class);

    @StartSaga
    @SagaEventHandler(associationProperty = ASSOCIATION_PROPERTIES)
    public void handle(OrderCreatedEvent orderCreatedEvent) {
        ReserveProductCommand reserveProductCommand = ReserveProductCommand.builder()
                .orderId(orderCreatedEvent.getOrderId())
                .productId(orderCreatedEvent.getProductId())
                .userId(orderCreatedEvent.getUserId())
                .quantity(orderCreatedEvent.getQuantity())
                .build();

        LOGGER.info(String.format("Order created event handled for orderId:%s and productId:%s", orderCreatedEvent.getOrderId(), orderCreatedEvent.getProductId()));

        commandGateway.send(reserveProductCommand, (commandMessage, commandResultMessage) -> {
            if (commandResultMessage.isExceptional()) {
                //Start compensating transaction
                RejectOrderCommand rejectOrderCommand = new RejectOrderCommand(orderCreatedEvent.getOrderId(),
                        commandResultMessage.exceptionResult().getMessage());

                commandGateway.send(rejectOrderCommand);
            }
        });
    }

    @SagaEventHandler(associationProperty = ASSOCIATION_PROPERTIES)
    public void handle(ProductReservedEvents productReservedEvents) {
        //process user payment
        LOGGER.info(String.format("product reserved event handled for orderId:%s and productId:%s",
                productReservedEvents.getOrderId(), productReservedEvents.getProductId()));

        var fetchUserPaymentDetailsQuery = new FetchUserPaymentDetailsQuery(productReservedEvents.getUserId());

        User userPaymentDetails = null;

        try {
            userPaymentDetails = queryGateway
                    .query(fetchUserPaymentDetailsQuery, ResponseTypes.instanceOf(User.class))
                    .join();
        } catch (Exception exception) {
            LOGGER.error(exception.getMessage());
            cancelProductReservation(productReservedEvents, exception.getMessage());
            return;
        }

        if (userPaymentDetails == null) {
            cancelProductReservation(productReservedEvents, "Couldn't fetch user payment details");
            return;
        }

        LOGGER.info(String.format("Successfully fetch user payment details for user %s", userPaymentDetails.getFirstName()));

        ProcessPaymentCommand processPaymentCommand = ProcessPaymentCommand.builder()
                .orderId(productReservedEvents.getOrderId())
                .paymentId(UUID.randomUUID().toString())
                .paymentDetails(userPaymentDetails.getPaymentDetails())
                .build();

        String result = null;
        try {
            result = commandGateway.sendAndWait(processPaymentCommand, 10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
            cancelProductReservation(productReservedEvents, ex.getMessage());
            return;
        }

        if (result == null) {
            LOGGER.info("The ProcessPaymentCommand resulted in NULL. Initiation a compensation transaction");
            cancelProductReservation(productReservedEvents, "Couldn't process user payment with provided payments details");
        }

    }

    @SagaEventHandler(associationProperty = ASSOCIATION_PROPERTIES)
    public void handle(PaymentProcessedEvent paymentProcessedEvent) {
        commandGateway.send(new ApproveOrderCommand(paymentProcessedEvent.getOrderId()));// 1
    }

    @EndSaga
    @SagaEventHandler(associationProperty = ASSOCIATION_PROPERTIES)
    public void handle(OrderApprovedEvent orderApprovedEvent) {
        LOGGER.info("Order is approved. Order with id: " + orderApprovedEvent.getOrderId());//3

        // we use @EndSaga or programatically SagaLIfecycle.end() if we need call it depend on condition
        // after that Saga don't accept new event
        //SagaLifecycle.end();
    }

    @SagaEventHandler(associationProperty = ASSOCIATION_PROPERTIES)
    public void handle(ProductReservationCanceledEvent productReservationCanceledEvent) {
        var rejectOrderCommand = new RejectOrderCommand(productReservationCanceledEvent.getOrderId(),
                productReservationCanceledEvent.getReason());

        commandGateway.send(rejectOrderCommand);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = ASSOCIATION_PROPERTIES)
    public void handle(OrderRejectEvent orderRejectEvent) {
        LOGGER.info("Successfully rejected order with id:" + orderRejectEvent.getOrderId());
    }

    private void cancelProductReservation(ProductReservedEvents productReservedEvents, String reason) {
        var cancelProductReservationCommand = CancelProductReservationCommand.builder()
                .orderId(productReservedEvents.getOrderId())
                .productId(productReservedEvents.getProductId())
                .userId(productReservedEvents.getUserId())
                .quantity(productReservedEvents.getQuantity())
                .reason(reason)
                .build();

        commandGateway.send(cancelProductReservationCommand);
    }
}
