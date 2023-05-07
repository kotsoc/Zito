package com.konstantinos.zito.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.konstantinos.zito.model.Table;
import java.util.Optional;

public interface TableRepository extends MongoRepository<Table, String> {
    Optional<Table> findByNumber(int number);
}