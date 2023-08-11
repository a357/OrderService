package com.appdeveloperblog.estore.orderservice.core.repository;

import com.appdeveloperblog.estore.orderservice.core.entity.OrderLookupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderLookupRepository extends JpaRepository<OrderLookupEntity, String> {
    OrderLookupEntity findByOrderIdOrProductId(String orderId, String productId);
}
