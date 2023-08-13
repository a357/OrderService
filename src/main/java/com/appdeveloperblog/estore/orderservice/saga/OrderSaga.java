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
import com.appdeveloperblog.estore.orderservice.core.OrderSummary;
import com.appdeveloperblog.estore.orderservice.core.events.OrderApprovedEvent;
import com.appdeveloperblog.estore.orderservice.core.events.OrderCreatedEvent;
import com.appdeveloperblog.estore.orderservice.core.events.OrderRejectEvent;
import com.appdeveloperblog.estore.orderservice.query.FindOrderQuery;
import org.apache.logging.log4j.util.Strings;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Saga
public class OrderSaga {
    private static final String ASSOCIATION_PROPERTIES = "orderId";
    private static final String DEADLINE_NAME_PAYMENT_PROCESSING = "payment-processing-deadline";
    @Autowired
    private transient CommandGateway commandGateway;
    @Autowired
    private transient QueryGateway queryGateway;
    @Autowired
    private transient DeadlineManager deadlineSchedule;
    @Autowired
    private transient QueryUpdateEmitter queryUpdateEmitter;
    private String scheduleId;

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

        /*
        * TODO DEADLINE START
        * if the payment processing didn't complete in 10 sec. program will expect a different event to be triggered
        *
        * but if the payment processing did complete on time and the PaymentProcessedEvent did get called, then
        * I  can cancel this deadline for the same saga class
        * */
        scheduleId = deadlineSchedule.schedule(Duration.of(120, ChronoUnit.SECONDS),
                DEADLINE_NAME_PAYMENT_PROCESSING, productReservedEvents);

        ProcessPaymentCommand processPaymentCommand = ProcessPaymentCommand.builder()
                .orderId(productReservedEvents.getOrderId())
                .paymentId(UUID.randomUUID().toString())
                .paymentDetails(userPaymentDetails.getPaymentDetails())
                .build();

        String result = null;
        try {
            result = commandGateway.sendAndWait(processPaymentCommand);
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
        /*
        * TODO DEADLINE CANCEL
        * So if the payment processing is successful and this PaymentProcessedEvent did get called,
        * then I need to cancel the scheduled deadline.
        * */
        cancelDeadline();

        commandGateway.send(new ApproveOrderCommand(paymentProcessedEvent.getOrderId()));// 1
    }

    @EndSaga
    @SagaEventHandler(associationProperty = ASSOCIATION_PROPERTIES)
    public void handle(OrderApprovedEvent orderApprovedEvent) {
        LOGGER.info("Order is approved. Order with id: " + orderApprovedEvent.getOrderId());//3

        queryUpdateEmitter.emit(FindOrderQuery.class, query -> true,
                new OrderSummary(orderApprovedEvent.getOrderId(), orderApprovedEvent.getStatus(), Strings.EMPTY));

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

        queryUpdateEmitter.emit(FindOrderQuery.class, query -> true,
                new OrderSummary(orderRejectEvent.getOrderId(), orderRejectEvent.getStatus(), orderRejectEvent.getReason()));
    }

    @DeadlineHandler(deadlineName = DEADLINE_NAME_PAYMENT_PROCESSING)
    public void handlePaymentDeadline(ProductReservedEvents productReservedEvents) {
        LOGGER.info("Payment processing deadline took place. Sending a compensation command to cancel the product reservation");
        cancelProductReservation(productReservedEvents, "payment timeout");
    }

    private void cancelProductReservation(ProductReservedEvents productReservedEvents, String reason) {
        cancelDeadline();

        var cancelProductReservationCommand = CancelProductReservationCommand.builder()
                .orderId(productReservedEvents.getOrderId())
                .productId(productReservedEvents.getProductId())
                .userId(productReservedEvents.getUserId())
                .quantity(productReservedEvents.getQuantity())
                .reason(reason)
                .build();

        commandGateway.send(cancelProductReservationCommand);
    }

    private void cancelDeadline() {
        if (scheduleId != null) {
            deadlineSchedule.cancelAll(DEADLINE_NAME_PAYMENT_PROCESSING);
            scheduleId = null;
        }
    }
}
