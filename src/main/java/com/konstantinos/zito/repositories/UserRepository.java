package com.konstantinos.zito.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.konstantinos.zito.model.RestaurantUser;

public interface UserRepository extends MongoRepository<RestaurantUser, UUID> {
    Optional<RestaurantUser> findByEmail(String email);

    Optional<RestaurantUser> findById(UUID id);

    Boolean existsByEmail(String email);

    void deleteByEmail(String email);
}