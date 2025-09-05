package com.konstantinos.zito.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.konstantinos.zito.model.Order;

public interface OrderRepository extends MongoRepository<Order, UUID> {
    List<Order> findByWaiterName(String waiterName);

    List<Order> findByTableNumber(int table);

    Optional<Order> findById(int orderId);
}