package com.appdeveloperblog.estore.orderservice.core.repository;

import com.appdeveloperblog.estore.orderservice.core.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    OrderEntity findByOrderId(String orderId);
}
