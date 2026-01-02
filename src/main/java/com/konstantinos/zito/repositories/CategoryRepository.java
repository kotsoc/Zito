package com.konstantinos.zito.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.konstantinos.zito.model.Category;

public interface CategoryRepository extends MongoRepository<Category, String> {
    Optional<Category> findById(String id);
    
    Optional<Category> findByName(String name);

    Optional<Category> findBySortOrder(String type);
}