package com.konstantinos.zito.security;

import java.util.ArrayList;

import org.springframework.context.annotation.Bean;

import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("local-dev")
public class LocalDevSecurityConf implements AuthenticationManager {

    @org.springframework.beans.factory.annotation.Value("${zito.app.cors.allowed-origins}")
    private java.util.List<String> allowedOrigins;

    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<org.springframework.web.filter.CorsFilter> corsFilter() {
        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(java.util.Arrays.asList("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(java.util.Arrays.asList("Authorization","Cache-Control","Content-Type","refreshToken"));
        config.setAllowCredentials(true);
        
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        org.springframework.boot.web.servlet.FilterRegistrationBean<org.springframework.web.filter.CorsFilter> bean =
        new org.springframework.boot.web.servlet.FilterRegistrationBean<>(new org.springframework.web.filter.CorsFilter(source));

        bean.setOrder(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()); // Disable CSRF for dev too
        http.cors(org.springframework.security.config.Customizer.withDefaults());
        
        http.authorizeHttpRequests(authz -> authz
                .anyRequest().authenticated()
        ).httpBasic(withDefaults());

        return http.build();
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        System.out.println("edwwww " + authentication.getCredentials());
        if (authentication.getPrincipal().toString().equals("babas")
                && authentication.getCredentials().toString().equals("babas")) {
            var authoritiesList = new ArrayList<SimpleGrantedAuthority>();
            authoritiesList.add(new SimpleGrantedAuthority("admin"));
            return new UsernamePasswordAuthenticationToken(authentication.getPrincipal(),
                    authentication.getCredentials(), authoritiesList);
        }
        return authentication;
    }  

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(8);
    }

}
