package com.konstantinos.zito.model;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "menuitems")
public class MenuItem {

    @Id
    private String id;

    @Indexed
    private String name;

    private String description;
    private BigDecimal netPrice;
    private BigDecimal grossPrice; // OPTIONAL
    private int categoryId;
    private String categoryName;
    private Boolean available;
    private String imageUrl;
    private BigDecimal taxRate;

    public MenuItem() {
    }

    public MenuItem(String name, int categoryId, String categoryName, String description, BigDecimal netPrice, BigDecimal grossPrice, String imageUrl) {
        this.name = name;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.description = description;
        this.grossPrice = grossPrice;
        this.netPrice = netPrice;
        this.imageUrl = imageUrl;
    }

    public void setId(String id) {
        this.id = id;
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

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getGrossPrice() {
        return grossPrice;
    }

    public void setGrossPrice(BigDecimal price) {
        this.grossPrice = price;
    }
    public BigDecimal getNetPrice() {
        return netPrice;
    }

    public void setNetPrice(BigDecimal price) {
        this.netPrice = price;
    }
    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String urlString) {
        this.imageUrl = urlString;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }
    
    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }
}
