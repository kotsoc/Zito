package com.example.zito.repositories;

import com.example.zito.model.Waiter;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WaiterRepository extends MongoRepository<Waiter, String> {
}