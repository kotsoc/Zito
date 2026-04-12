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
        branding.setName("Cofineta Cafe");
        branding.setLogoUrl("https://i.postimg.cc/HskK0LHT/cofi-Logo-400.png");
        branding.setFaviconUrl("https://i.postimg.cc/HskK0LHT/cofi-Logo-400.png");
        branding.setDescription("A sample restaurant branding response.");

        RestaurantThemeColors lightTheme = new RestaurantThemeColors();
        lightTheme.setPrimary("#055E20"); // deep palm green (brand anchor)
        lightTheme.setPrimaryForeground("#FFFFFF");

        lightTheme.setSecondary("#F4A300"); // warm sun/golden food accent
        lightTheme.setSecondaryForeground("#1A1A1A");

        lightTheme.setAccent("#2EC4B6"); // fresh aqua tropical water
        lightTheme.setAccentForeground("#FFFFFF");

        lightTheme.setBackground("#FAFAF5"); // warm off-white (not cold white)
        lightTheme.setForeground("#1F2A1F"); // soft dark green-gray

        lightTheme.setCard("#FFFFFF");
        lightTheme.setCardForeground("#1F2A1F");

        lightTheme.setMuted("#E9EFE9"); // soft green-tinted gray
        lightTheme.setMutedForeground("#5C6B5C");

        lightTheme.setBorder("#D8E2D8");

        lightTheme.setDestructive("#D64545");
        lightTheme.setDestructiveForeground("#FFFFFF");

        RestaurantThemeColors darkTheme = new RestaurantThemeColors();
        darkTheme.setPrimary("#7BC96F"); // glowing palm green
        darkTheme.setPrimaryForeground("#0B1F10");

        darkTheme.setSecondary("#FFB74D"); // warm lantern/orange glow
        darkTheme.setSecondaryForeground("#1A1A1A");

        darkTheme.setAccent("#4DD0E1"); // cool tropical cyan contrast
        darkTheme.setAccentForeground("#00363A");

        darkTheme.setBackground("#0B1410"); // deep jungle green-black
        darkTheme.setForeground("#E6F2E6");

        darkTheme.setCard("#13231A"); // elevated jungle panels
        darkTheme.setCardForeground("#E6F2E6");

        darkTheme.setMuted("#1C2E24");
        darkTheme.setMutedForeground("#A7B7A7");

        darkTheme.setBorder("#2A3D31");

        darkTheme.setDestructive("#FF6B6B");
        darkTheme.setDestructiveForeground("#0B1410");

        branding.setLightTheme(lightTheme);
        branding.setDarkTheme(darkTheme);
        return branding;
    }
}
