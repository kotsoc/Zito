package com.konstantinos.zito.webControllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.konstantinos.zito.model.RestaurantBranding;
import com.konstantinos.zito.services.RestaurantBrandingService;

@RestController
@CrossOrigin
@RequestMapping("/api/v1/restaurants")
public class RestaurantBrandingController {

    private final RestaurantBrandingService restaurantBrandingService;

    public RestaurantBrandingController(RestaurantBrandingService restaurantBrandingService) {
        this.restaurantBrandingService = restaurantBrandingService;
    }

    @GetMapping("/{restaurantId}/branding")
    public ResponseEntity<RestaurantBranding> getBranding(@PathVariable String restaurantId) {
        RestaurantBranding branding = restaurantBrandingService.getBranding(restaurantId);
        return ResponseEntity.ok(branding);
    }
}
