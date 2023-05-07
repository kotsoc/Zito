package com.konstantinos.zito.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.konstantinos.zito.model.Order;
import com.konstantinos.zito.model.RestaurantUser;

public interface ChangedOrderRepository extends MongoRepository<Order, String> {
    List<Order> findByWaiterName(RestaurantUser waiter);

    Optional<Order> findByOrderId(int orderId);
}