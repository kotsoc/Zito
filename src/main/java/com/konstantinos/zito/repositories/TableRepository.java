package com.konstantinos.zito.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.konstantinos.zito.model.Table;

public interface TableRepository extends MongoRepository<Table, String> {
}