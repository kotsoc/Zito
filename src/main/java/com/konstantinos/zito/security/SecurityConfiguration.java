package com.konstantinos.zito.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import com.konstantinos.zito.webControllers.OAuth2FailureHandler;
import com.konstantinos.zito.webControllers.OAuth2SuccessHandler;

import jakarta.servlet.FilterRegistration;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile("!dev")
public class SecurityConfiguration {

    private UserDetailsService userDetailsService;
    private JwtTokenFilter tokenFilter;

    public SecurityConfiguration(UserDetailsService userDetailsService, JwtTokenFilter tokenFilter) {
        this.userDetailsService = userDetailsService;
        this.tokenFilter = tokenFilter;    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthEntryPoint authEntryPoint,
            OAuth2SuccessHandler oAuth2SuccessHandler, OAuth2FailureHandler oAuth2FailureHandler) throws Exception {
        // JWT will still run before
        http.csrf(csrf -> csrf.disable());

        http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        // Disable default security context saving - stop oauth from making sessons
        http.securityContext(securityContext ->
          securityContext.requireExplicitSave(false)
        );

        http
            .exceptionHandling(ex -> ex
            .authenticationEntryPoint(authEntryPoint))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(
                    "/actuator/**",
                    "/*.png",
                    "/index.html",
                    "/manifest.json",
                    "/favicon.ico",
                    "/static/**",
                    "/css/*",
                    "/js/*",
                    "/user/signin",
                    "/user/signup",
                    "/user/refresh",
                    "/swagger-ui/**",
                    "/v3/api-docs*/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/menu-items", "/api/v1/categories")
                .permitAll()
                .anyRequest().authenticated());

        // Add your existing DaoAuthenticationProvider
        http.authenticationProvider(authenticationProvider());

        // Add JWT filter
        http.addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);

        // --- NEW: OAuth2 Login ---
        http.oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler));
                
        return http.build();
    }

    // Used by Spring Security if CORS is enabled.
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // config.setAllowCredentials(true);
        config.setAllowedOrigins(Arrays.asList("http://localhost:8081", "http://localhost:3000", "http://localhost:8081"));
        config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization","Cache-Control","Content-Type","refreshToken"));
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean =
        new FilterRegistrationBean<>(new CorsFilter(source));

        bean.setOrder(Ordered.HIGHEST_PRECEDENCE); // 🔑 critical
        return bean;
    }

    @Bean
    public AuthenticationManager customAuthenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http
                .getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }
}