package com.konstantinos.zito.services;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.konstantinos.zito.model.RestaurantBranding;
import com.konstantinos.zito.model.RestaurantThemeColors;
import com.konstantinos.zito.repositories.RestaurantBrandingRepository;

@Service
public class RestaurantBrandingService {

    private final RestaurantBrandingRepository restaurantBrandingRepository;

    public RestaurantBrandingService(RestaurantBrandingRepository restaurantBrandingRepository) {
        this.restaurantBrandingRepository = restaurantBrandingRepository;
    }

    public RestaurantBranding getBranding(String restaurantId) {
        Optional<RestaurantBranding> savedBranding = restaurantBrandingRepository.findById(restaurantId);
        return savedBranding.orElseGet(() -> buildMockBranding(restaurantId));
    }

    private RestaurantBranding buildMockBranding(String restaurantId) {
        RestaurantBranding branding = new RestaurantBranding();
        branding.setId(restaurantId);
        branding.setName("Sample Restaurant "+ restaurantId);
        branding.setLogoUrl("https://example.com/logo.png");
        branding.setFaviconUrl("https://example.com/favicon.png");
        branding.setDescription("A sample restaurant branding response.");

        RestaurantThemeColors lightTheme = new RestaurantThemeColors();
        lightTheme.setPrimary("#1E88E5");
        lightTheme.setPrimaryForeground("#FFFFFF");
        lightTheme.setSecondary("#FFB300");
        lightTheme.setSecondaryForeground("#000000");
        lightTheme.setAccent("#8E24AA");
        lightTheme.setAccentForeground("#FFFFFF");
        lightTheme.setBackground("#FFFFFF");
        lightTheme.setForeground("#212121");
        lightTheme.setCard("#F5F5F5");
        lightTheme.setCardForeground("#212121");
        lightTheme.setMuted("#EEEEEE");
        lightTheme.setMutedForeground("#616161");
        lightTheme.setBorder("#E0E0E0");
        lightTheme.setDestructive("#D32F2F");
        lightTheme.setDestructiveForeground("#FFFFFF");

        RestaurantThemeColors darkTheme = new RestaurantThemeColors();
        darkTheme.setPrimary("#90CAF9");
        darkTheme.setPrimaryForeground("#0D47A1");
        darkTheme.setSecondary("#FFD54F");
        darkTheme.setSecondaryForeground("#212121");
        darkTheme.setAccent("#CE93D8");
        darkTheme.setAccentForeground("#4A148C");
        darkTheme.setBackground("#121212");
        darkTheme.setForeground("#E0E0E0");
        darkTheme.setCard("#1E1E1E");
        darkTheme.setCardForeground("#E0E0E0");
        darkTheme.setMuted("#2C2C2C");
        darkTheme.setMutedForeground("#B0BEC5");
        darkTheme.setBorder("#424242");
        darkTheme.setDestructive("#EF5350");
        darkTheme.setDestructiveForeground("#FFFFFF");

        branding.setLightTheme(lightTheme);
        branding.setDarkTheme(darkTheme);
        return branding;
    }
}
