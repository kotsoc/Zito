package com.konstantinos.zito.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.konstantinos.zito.model.Order;
import com.konstantinos.zito.model.RestaurantUser;

public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByWaiter(RestaurantUser waiter);

    List<Order> findByTable(int table);
}