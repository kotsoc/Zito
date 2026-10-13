package com.konstantinos.zito.security;

import com.konstantinos.zito.services.TokenInvalidatorService;
import com.konstantinos.zito.webControllers.OAuth2FailureHandler;
import com.konstantinos.zito.webControllers.OAuth2SuccessHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A stub isolates the production filter chain from unrelated controller/data-model defects.
@WebMvcTest(controllers = ManagementAuthorizationTest.Endpoints.class,
        properties = "zito.app.cors.allowed-origins=http://localhost:3000")
@Import({SecurityConfiguration.class, JwtTokenFilter.class, JwtAuthEntryPoint.class,
        ManagementAuthorizationTest.Endpoints.class})
class ManagementAuthorizationTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private JwtTokenUtil jwtTokenUtil;
    @MockitoBean private TokenInvalidatorService tokenInvalidatorService;
    @MockitoBean private OAuth2SuccessHandler successHandler;
    @MockitoBean private OAuth2FailureHandler failureHandler;
    @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;

    static Stream<Arguments> mutations() {
        return Stream.of("/api/v1/menu-items", "/api/v1/menu-items/item-1",
                        "/api/v1/categories", "/api/v1/categories/1",
                        "/api/v1/table", "/api/v1/table/1", "/api/v1/table/layout",
                        "/api/v1/table/layout/layout-1")
                .flatMap(path -> Stream.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)
                        .map(method -> Arguments.of(method, path)));
    }

    @ParameterizedTest
    @MethodSource("mutations")
    void anonymousCannotMutate(HttpMethod method, String path) throws Exception {
        mvc.perform(request(method, path)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("mutations")
    void nonManagementRolesCannotMutate(HttpMethod method, String path) throws Exception {
        for (String role : new String[]{"GUEST", "USER", "WAITER", "KITCHEN"}) {
            mvc.perform(request(method, path).with(user("customer").roles(role)))
                    .andExpect(status().isForbidden());
        }
    }

    @ParameterizedTest
    @MethodSource("mutations")
    void managementRolesCanMutate(HttpMethod method, String path) throws Exception {
        for (String role : new String[]{"ADMIN", "MANAGER"}) {
            mvc.perform(request(method, path).with(user("staff").roles(role)))
                    .andExpect(status().isNoContent());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/menu-items", "/api/v1/categories"})
    void publicMenuReadsRemainAccessible(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isNoContent());
    }

    @RestController
    static class Endpoints {
        @RequestMapping(value = {"/api/v1/menu-items", "/api/v1/menu-items/**",
                "/api/v1/categories", "/api/v1/categories/**", "/api/v1/table", "/api/v1/table/**"},
                method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                        RequestMethod.PATCH, RequestMethod.DELETE})
        ResponseEntity<Void> handle() {
            return ResponseEntity.noContent().build();
        }
    }
}
