package com.konstantinos.zito.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.konstantinos.zito.model.MenuItem;

public interface MenuItemRepository extends MongoRepository<MenuItem, String> {
    Optional<MenuItem> findByName(String name);

    List<MenuItem> findByType(String type);
}