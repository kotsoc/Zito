package com.konstantinos.zito.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.konstantinos.zito.model.RestaurantBranding;

public interface RestaurantBrandingRepository extends MongoRepository<RestaurantBranding, String> {
}
