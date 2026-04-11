package com.konstantinos.zito.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "restaurantbranding")
public class RestaurantBranding {

    @Id
    private String id;
    private String name;
    private String logoUrl;
    private String faviconUrl;
    private String description;
    private RestaurantThemeColors lightTheme;
    private RestaurantThemeColors darkTheme;

    public RestaurantBranding() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getFaviconUrl() {
        return faviconUrl;
    }

    public void setFaviconUrl(String faviconUrl) {
        this.faviconUrl = faviconUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RestaurantThemeColors getLightTheme() {
        return lightTheme;
    }

    public void setLightTheme(RestaurantThemeColors lightTheme) {
        this.lightTheme = lightTheme;
    }

    public RestaurantThemeColors getDarkTheme() {
        return darkTheme;
    }

    public void setDarkTheme(RestaurantThemeColors darkTheme) {
        this.darkTheme = darkTheme;
    }
}
