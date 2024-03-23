package com.konstantinos.zito.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.konstantinos.zito.model.TableLayout;
import java.util.Optional;

public interface TableLayoutRepository extends MongoRepository<TableLayout, String> {
    Optional<TableLayout> findById(String id);
}