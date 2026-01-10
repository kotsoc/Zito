package com.konstantinos.zito;

import java.math.BigDecimal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;

// Jackson configuration for handling String as decimals
@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Force all BigDecimals to be serialized as Strings
        mapper.configOverride(BigDecimal.class)
              .setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));
        return mapper;
    }
}
