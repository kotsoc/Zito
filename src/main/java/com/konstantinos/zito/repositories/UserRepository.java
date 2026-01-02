package com.konstantinos.zito.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.konstantinos.zito.model.RestaurantUser;

public interface UserRepository extends MongoRepository<RestaurantUser, String> {
    Optional<RestaurantUser> findByEmail(String email);

    Boolean existsByEmail(String email);
}