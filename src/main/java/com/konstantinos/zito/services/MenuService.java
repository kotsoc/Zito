package com.konstantinos.zito.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.konstantinos.zito.model.Category;
import com.konstantinos.zito.model.MenuItem;
import com.konstantinos.zito.model.exceptions.MenuItemNotFoundException;
import com.konstantinos.zito.repositories.CategoryRepository;
import com.konstantinos.zito.repositories.MenuItemRepository;

@Service
public class MenuService {
    
    final CategoryRepository categoryRepository;
    final MenuItemRepository menuItemRepository;
    final LoadingCache<String, MenuItem> menuItemCache;

    public MenuService(MenuItemRepository menuItemRepository, CategoryRepository categoryRepository) {
        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
        menuItemCache = CacheBuilder.newBuilder()
                .expireAfterAccess(12, TimeUnit.HOURS)
                .maximumSize(1000)
                .build(CacheLoader.from((name) -> {
                    return findByName(name);
                }));
    }

    // Get all menu items - will trigger a db call
    public List<MenuItem> findAllItems() {
        var allItems = menuItemRepository.findAll();
        allItems.forEach(item -> menuItemCache.put(item.getName(), item));
        return allItems;
    }

    // Find a menuItem by name
    public MenuItem findItem(String name) throws ExecutionException {
        return menuItemCache.get(name);
    }

    // Create a new menuItem
    public List<MenuItem> saveAllItems(List<MenuItem> menuItems) {
        var item = menuItemRepository.saveAll(menuItems);
        menuItemCache.putAll(menuItems.stream().collect(Collectors.toMap(MenuItem::getName, Function.identity())));
        return item;
    }

    // Update a menuItem
    public MenuItem UpdateItem(String name, MenuItem menuItem) {
        // Retrieve the MenuItem object with the given id from the database
        MenuItem oldItem = menuItemRepository.findByName(name).orElseThrow(MenuItemNotFoundException::new);
        if (name.equals(menuItem.getName())) {
            oldItem.setDescription(menuItem.getDescription());
            oldItem.setName(menuItem.getName());
            oldItem.setPrice(menuItem.getPrice());
            oldItem.setCategoryId(menuItem.getCategoryId());
            menuItemCache.put(menuItem.getName(), oldItem);
            return menuItemRepository.save(oldItem);
        } else {
            throw new MenuItemNotFoundException("mismatching names");
        }
    }

    // Delete a menuItem
    public void deleteItem(String id) {
        menuItemRepository.deleteById(id);
    }

    // Find a menuItem price by name
    public BigDecimal getItemPrice(String name) {
        try {
            return menuItemCache.get(name).getPrice();
        } catch (Exception e) {
            return BigDecimal.valueOf(-1);
        }
    }

    // Find a menuItem price by name
    public int getItemType(String name) {
        try {
            return menuItemCache.get(name).getCategoryId();
        } catch (Exception e) {
            return -1;
        }
    }

    // internal supplier function
    private MenuItem findByName(String name) {
        Optional<MenuItem> menuItemOptional = menuItemRepository.findByName(name);
        if (menuItemOptional.isPresent()) {
            return menuItemOptional.get();
        } else {
            return new MenuItem();
        }
    }

       // Get all categories
    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }

    // Find a category by name
    public Optional<Category> findCategory(String name) {
        return categoryRepository.findByName(name);
    }

    // Find a category by id
    public Optional<Category> findCategoryById(int id) {
        return categoryRepository.findById(id);
    }

    // Create a new category
    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    // Create multiple categories
    public List<Category> saveAllCategories(List<Category> categories) {
        return categoryRepository.saveAll(categories);
    }

    // Update a category
    public Category updateCategory(String name, Category category) {
        Category oldCategory = categoryRepository.findByName(name)
            .orElseThrow(() -> new RuntimeException("Category not found"));
        if (name.equals(category.getName())) {
            oldCategory.setDescription(category.getDescription());
            oldCategory.setName(category.getName());
            return categoryRepository.save(oldCategory);
        } else {
            throw new RuntimeException("Mismatching category names");
        }
    }

    // Delete a category by id
    public void deleteCategory(int id) {
        categoryRepository.deleteById(id);
    }
}
