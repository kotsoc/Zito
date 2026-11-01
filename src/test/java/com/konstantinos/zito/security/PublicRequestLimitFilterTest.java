package com.konstantinos.zito.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PublicRequestLimitFilterTest {
    @Test
    void rejectsOversizedBodyBeforeController() throws Exception {
        var filter = new PublicRequestLimitFilter(60, 4, new ObjectMapper());
        var request = request();
        request.setContent("12345".getBytes());
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        assertEquals(413, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void boundsRequestsByRemoteAddressAndPreservesBody() throws Exception {
        var filter = new PublicRequestLimitFilter(1, 100, new ObjectMapper());
        var request = request();
        request.setContent("{}".getBytes());
        FilterChain chain = (incoming, outgoing) -> assertEquals("{}",
                new String(incoming.getInputStream().readAllBytes()));
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        var response = new MockHttpServletResponse();
        filter.doFilter(request(), response, mock(FilterChain.class));
        assertEquals(429, response.getStatus());
        assertEquals("60", response.getHeader("Retry-After"));
        var anotherIp = request();
        anotherIp.setRemoteAddr("127.0.0.2");
        var allowed = new MockHttpServletResponse();
        filter.doFilter(anotherIp, allowed, mock(FilterChain.class));
        assertEquals(200, allowed.getStatus());
    }

    private MockHttpServletRequest request() {
        var request = new MockHttpServletRequest("POST", "/api/v1/orders");
        request.setServletPath("/api/v1/orders");
        return request;
    }
}
