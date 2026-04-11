package com.konstantinos.zito.webControllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.konstantinos.zito.model.RestaurantBranding;
import com.konstantinos.zito.model.RestaurantThemeColors;
import com.konstantinos.zito.security.JwtAuthEntryPoint;
import com.konstantinos.zito.security.JwtTokenFilter;
import com.konstantinos.zito.security.JwtTokenUtil;
import com.konstantinos.zito.services.RestaurantBrandingService;
import com.konstantinos.zito.webControllers.OAuth2FailureHandler;
import com.konstantinos.zito.webControllers.OAuth2SuccessHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RestaurantBrandingController.class)
@AutoConfigureMockMvc(addFilters = false)
class RestaurantBrandingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestaurantBrandingService restaurantBrandingService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtTokenFilter jwtTokenFilter;

    @MockBean
    private JwtAuthEntryPoint jwtAuthEntryPoint;

    @MockBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockBean
    private OAuth2FailureHandler oAuth2FailureHandler;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getBranding_returnsBrandingForRestaurantId() throws Exception {
        String restaurantId = "restaurant-123";

        RestaurantThemeColors lightTheme = new RestaurantThemeColors();
        lightTheme.setPrimary("#1E88E5");
        lightTheme.setPrimaryForeground("#FFFFFF");

        RestaurantThemeColors darkTheme = new RestaurantThemeColors();
        darkTheme.setPrimary("#90CAF9");
        darkTheme.setPrimaryForeground("#0D47A1");

        RestaurantBranding branding = new RestaurantBranding();
        branding.setId(restaurantId);
        branding.setName("Test Restaurant");
        branding.setLogoUrl("https://example.com/logo.png");
        branding.setFaviconUrl("https://example.com/favicon.png");
        branding.setDescription("Test restaurant branding");
        branding.setLightTheme(lightTheme);
        branding.setDarkTheme(darkTheme);

        when(restaurantBrandingService.getBranding(restaurantId)).thenReturn(branding);

        mockMvc.perform(get("/api/v1/restaurants/{restaurantId}/branding", restaurantId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(restaurantId))
                .andExpect(jsonPath("$.name").value("Test Restaurant"))
                .andExpect(jsonPath("$.logoUrl").value("https://example.com/logo.png"))
                .andExpect(jsonPath("$.faviconUrl").value("https://example.com/favicon.png"))
                .andExpect(jsonPath("$.lightTheme.primary").value("#1E88E5"))
                .andExpect(jsonPath("$.darkTheme.primaryForeground").value("#0D47A1"));
    }
}
