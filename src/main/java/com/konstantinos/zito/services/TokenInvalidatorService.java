package com.konstantinos.zito.services;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import com.konstantinos.zito.repositories.InvalidatedTokenRepository;
import com.konstantinos.zito.model.InvalidatedToken;

@Service
public class TokenInvalidatorService {
    private final InvalidatedTokenRepository repository;
    private Set<String> tokenBlacklist = ConcurrentHashMap.newKeySet();

    public TokenInvalidatorService(InvalidatedTokenRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        repository.findAll().forEach(token -> tokenBlacklist.add(token.getId()));
    }

    public void invalidateToken(String Token) {
        if (Token != null && !Token.isEmpty()) {
            tokenBlacklist.add(Token);
            repository.save(new InvalidatedToken(Token));
        }
    }

    public boolean isInvalid(String Token) {
        if (Token == null || Token.isEmpty()) return true;
        return tokenBlacklist.contains(Token);
    }
}
