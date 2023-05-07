package com.konstantinos.zito.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.konstantinos.zito.model.RestaurantUser;

public interface UserRepository extends MongoRepository<RestaurantUser, String> {
    Optional<RestaurantUser> findByUsername(String username);

    Boolean existsByUsername(String username);
}