package com.appdeveloperblog.estore.orderservice;

import com.appdeveloperblog.estore.orderservice.command.rest.interceptor.CreateOrderCommandInterceptor;
import com.appdeveloperblog.estore.orderservice.config.AxonConfig;
import com.appdeveloperblog.estore.orderservice.core.Groups;
import com.appdeveloperblog.estore.orderservice.core.errorhandling.OrderServiceEventsErrorHandling;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.config.EventProcessingConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

@EnableDiscoveryClient
@SpringBootApplication
@Import({ AxonConfig.class })
public class OrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}


	@Autowired
	public void registerCreateOrderCommandInterceptor(ApplicationContext context, CommandBus commandBus) {
		commandBus.registerDispatchInterceptor(context.getBean(CreateOrderCommandInterceptor.class));
	}

	@Autowired
	public void configEventErrorHandling(EventProcessingConfigurer config) {
		config.registerListenerInvocationErrorHandler(Groups.ORDER_GROUP, conf -> new OrderServiceEventsErrorHandling());
	}

}
