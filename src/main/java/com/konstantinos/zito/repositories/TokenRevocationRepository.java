package com.konstantinos.zito.repositories;

import com.konstantinos.zito.model.TokenRevocation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TokenRevocationRepository extends MongoRepository<TokenRevocation, String> {
}
