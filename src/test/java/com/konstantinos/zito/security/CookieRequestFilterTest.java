package com.konstantinos.zito.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CookieRequestFilterTest {
    @ParameterizedTest
    @CsvSource({"/user/refresh,1,http://localhost:3000,true", "/user/signout,1,http://localhost:3000,true",
            "/user/refresh,,http://localhost:3000,false", "/user/signout,1,https://attacker.example,false",
            "/user/refresh,1,,true"})
    void cookieMutationsRequireCustomHeaderAndAllowedOrigin(String path, String header, String origin,
            boolean allowed) throws Exception {
        var request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        if (header != null) request.addHeader("X-CSRF-Protection", header);
        if (origin != null) request.addHeader("Origin", origin);
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        new CookieRequestFilter(List.of("http://localhost:3000"), new ObjectMapper())
                .doFilter(request, response, chain);
        if (allowed) verify(chain).doFilter(request, response);
        else {
            assertEquals(403, response.getStatus());
            verifyNoInteractions(chain);
        }
    }
}
