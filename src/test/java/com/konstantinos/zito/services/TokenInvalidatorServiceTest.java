package com.konstantinos.zito.services;

import com.konstantinos.zito.model.TokenRevocation;
import com.konstantinos.zito.repositories.InvalidatedTokenRepository;
import com.konstantinos.zito.repositories.TokenRevocationRepository;
import com.konstantinos.zito.security.JwtTokenUtil;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TokenInvalidatorServiceTest {
    @Test
    void revocationUsesEntireTokenLifetimeAndStoresOnlyHash() {
        var repository = mock(TokenRevocationRepository.class);
        var jwt = mock(JwtTokenUtil.class);
        Date expiry = new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);
        when(jwt.getExpiration("token")).thenReturn(expiry);
        var service = new TokenInvalidatorService(mock(InvalidatedTokenRepository.class), repository, jwt);
        service.invalidateToken("token");
        var record = ArgumentCaptor.forClass(TokenRevocation.class);
        verify(repository).save(record.capture());
        assertEquals(expiry, record.getValue().getExpiresAt());
        assertNotEquals("token", record.getValue().getId());
    }

    @Test
    void duplicateConsumptionRevokesRefreshFamily() {
        var repository = mock(TokenRevocationRepository.class);
        var jwt = mock(JwtTokenUtil.class);
        when(jwt.getRefreshFamily("token")).thenReturn("family");
        when(repository.insert(any(TokenRevocation.class)))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("reused"));
        var service = new TokenInvalidatorService(mock(InvalidatedTokenRepository.class), repository, jwt);
        assertFalse(service.consumeRefreshToken("token"));
        verify(repository).save(any(TokenRevocation.class));
    }
}
