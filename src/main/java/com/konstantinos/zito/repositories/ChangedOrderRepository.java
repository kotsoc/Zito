package com.konstantinos.zito.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.konstantinos.zito.model.Order;
import com.konstantinos.zito.model.RestaurantUser;

public interface ChangedOrderRepository extends MongoRepository<Order, UUID> {
    List<Order> findByWaiterName(RestaurantUser waiter);

    Optional<Order> findById(int orderId);
}