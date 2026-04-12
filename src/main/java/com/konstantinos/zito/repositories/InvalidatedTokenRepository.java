package com.konstantinos.zito.repositories;

import com.konstantinos.zito.model.InvalidatedToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvalidatedTokenRepository extends MongoRepository<InvalidatedToken, String> {
}
