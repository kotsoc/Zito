package com.example.zito.repositories;

import com.example.zito.model.Waiter;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface WaiterRepository extends MongoRepository<Waiter, String> {
    Optional<Waiter> findByName(String name);
}