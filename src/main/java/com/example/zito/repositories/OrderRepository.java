package com.example.zito.repositories;

import com.example.zito.model.Order;
import com.example.zito.model.Waiter;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByWaiter(Waiter waiter);

    List<Order> findByTable(int table);
}