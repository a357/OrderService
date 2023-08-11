package com.appdeveloperblog.estore.orderservice.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "orderslookup")
public class OrderLookupEntity implements Serializable {
    private static final long serialVersionId = 1L;

    @Id
    public String orderId;

    @Column(unique = true)
    private String productId;

}
