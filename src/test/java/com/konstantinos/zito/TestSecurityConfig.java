package com.konstantinos.zito;

import static org.mockito.Mockito.mock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import org.springframework.security.core.userdetails.UserDetailsService;

import com.konstantinos.zito.security.JwtAuthEntryPoint;
import com.konstantinos.zito.security.JwtTokenFilter;
import com.konstantinos.zito.webControllers.OAuth2SuccessHandler;


@TestConfiguration
@EnableMethodSecurity 
public class TestSecurityConfig {

    // @Bean
    // SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
    //     http
    //         .csrf(csrf -> csrf.disable())
    //         .authorizeHttpRequests(auth -> auth
    //             .requestMatchers(
    //                 "/user/signin",
    //                 "/user/register",
    //                 "/user/refresh"
    //             ).permitAll()
    //             .anyRequest().authenticated()
    //         );
          
    //     return http.build();
    // }


    @Bean public UserDetailsService userDetailsService() { return mock(UserDetailsService.class); }
    // @Bean public JwtTokenFilter jwtTokenFilter() { return mock(JwtTokenFilter.class); }
    @Bean public JwtAuthEntryPoint jwtAuthEntryPoint() { return mock(JwtAuthEntryPoint.class); }
    @Bean public OAuth2SuccessHandler oAuth2SuccessHandler() { return mock(OAuth2SuccessHandler.class); }
}