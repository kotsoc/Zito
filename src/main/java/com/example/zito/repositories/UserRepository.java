package com.example.zito.repositories;

import com.example.zito.model.RestaurantUser;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<RestaurantUser, String> {
    RestaurantUser findByUsername(String username);
}