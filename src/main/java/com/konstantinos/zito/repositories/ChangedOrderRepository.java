package com.konstantinos.zito.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.konstantinos.zito.model.Order;

public interface ChangedOrderRepository extends MongoRepository<Order, UUID> {
    List<Order> findByWaiterId(UUID waiterId);

    Optional<Order> findById(UUID orderId);
}