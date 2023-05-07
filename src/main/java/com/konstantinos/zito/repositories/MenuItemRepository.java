package com.konstantinos.zito.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.konstantinos.zito.model.MenuItem;

public interface MenuItemRepository extends MongoRepository<MenuItem, String> {
}