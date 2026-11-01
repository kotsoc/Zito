package com.konstantinos.zito.services;

import com.konstantinos.zito.model.TokenRevocation;
import com.konstantinos.zito.repositories.InvalidatedTokenRepository;
import com.konstantinos.zito.repositories.TokenRevocationRepository;
import com.konstantinos.zito.security.JwtTokenUtil;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class TokenInvalidatorService {
    private final InvalidatedTokenRepository legacyRepository;
    private final TokenRevocationRepository repository;
    private final JwtTokenUtil jwt;

    public TokenInvalidatorService(InvalidatedTokenRepository legacyRepository,
            TokenRevocationRepository repository, JwtTokenUtil jwt) {
        this.legacyRepository = legacyRepository;
        this.repository = repository;
        this.jwt = jwt;
    }

    public void invalidateToken(String token) {
        if (token != null && !token.isBlank()) {
            repository.save(new TokenRevocation(key(token), jwt.getExpiration(token)));
        }
    }

    public boolean isInvalid(String token) {
        return token == null || token.isBlank() || repository.existsById(key(token))
                || legacyRepository.existsById(token);
    }

    public boolean consumeRefreshToken(String token) {
        String family = jwt.getRefreshFamily(token);
        String familyKey = key("family:" + family);
        if (repository.existsById(familyKey)) return false;
        try {
            repository.insert(new TokenRevocation(key(token), jwt.getExpiration(token)));
            return true;
        } catch (DuplicateKeyException reused) {
            repository.save(new TokenRevocation(familyKey, jwt.getRefreshRevocationExpiry()));
            return false;
        }
    }

    public void revokeRefreshFamily(String token) {
        repository.save(new TokenRevocation(key("family:" + jwt.getRefreshFamily(token)),
                jwt.getRefreshRevocationExpiry()));
    }

    private String key(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
