package com.konstantinos.zito.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.konstantinos.zito.model.RestaurantUser;

public interface UserRepository extends MongoRepository<RestaurantUser, String> {
    RestaurantUser findByUsername(String username);
}