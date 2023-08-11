package com.appdeveloperblog.estore.orderservice.command.rest.interceptor;

import com.appdeveloperblog.estore.orderservice.command.commands.CreateOrderCommand;
import com.appdeveloperblog.estore.orderservice.core.repository.OrderLookupRepository;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiFunction;

@Component
public class CreateOrderCommandInterceptor implements MessageDispatchInterceptor<CommandMessage<?>> {

    private final OrderLookupRepository repository;
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateOrderCommandInterceptor.class);

    public CreateOrderCommandInterceptor(OrderLookupRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public BiFunction<Integer, CommandMessage<?>, CommandMessage<?>> handle(@Nonnull List<? extends CommandMessage<?>> messages) {
        return (index, command) -> {
            //LOGGER.info("intercept command: " + command.getPayloadType());

            //if (command.getPayload() instanceof CreateOrderCommand) {
                //CreateOrderCommand cmd = (CreateOrderCommand) command.getPayload();
                //LOGGER.info("handle create order");
                //var entity = repository.findByOrderIdOrProductId(cmd.getOrderId(), cmd.getProductId());

                //if (entity != null) {
                //    throw new IllegalStateException(String.format("Order with orderId: %s and productId: %s already exists",
                //            cmd.getOrderId(), cmd.getProductId()));
                //}
            //}

            return command;
        };
    }
}
