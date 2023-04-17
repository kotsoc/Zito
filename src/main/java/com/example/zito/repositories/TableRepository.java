package com.example.zito.repositories;

import com.example.zito.model.Table;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TableRepository extends MongoRepository<Table, String> {
}