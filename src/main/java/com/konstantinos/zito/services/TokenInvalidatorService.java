package com.konstantinos.zito.services;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class TokenInvalidatorService {
    private Set<String> tokenBlacklist = ConcurrentHashMap.newKeySet();
    
    public void invalidateToken(String Token) {
        tokenBlacklist.add(Token);
    }

    public boolean isInvalid(String Token) {
        return tokenBlacklist.contains(Token);
    }
}
