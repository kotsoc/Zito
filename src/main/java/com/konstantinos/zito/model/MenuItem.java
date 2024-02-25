package com.konstantinos.zito.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "menuItem")
public class MenuItem {

    @Id
    private String id;

    @Indexed
    private String name;
    @Indexed
    private String type;
    private String description;
    private double price;
    private String imageUrl;

    public MenuItem() {
    }

    public MenuItem(String name, String type, String description, double price, String imageUrl) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
    }

    // getters and setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String urlString) {
        this.imageUrl = urlString;
    }
}
