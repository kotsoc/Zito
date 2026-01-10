package com.konstantinos.zito.services;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        
        // Cache now consistently uses ID as the String key
        this.menuItemCache = CacheBuilder.newBuilder()
                .expireAfterAccess(12, TimeUnit.HOURS)
                .maximumSize(1000)
                .build(new CacheLoader<String, MenuItem>() {
                    @Override
                    public MenuItem load(String id) {
                        return findByIdInternal(id);
                    }
                });
    }

    // --- MenuItem Actions ---

    public List<MenuItem> findAllItems() {
        var allItems = menuItemRepository.findAll();
        // Populate cache with ID-based keys
        allItems.forEach(item -> menuItemCache.put(item.getId(), item));
        return allItems;
    }
    
    public Map<String, MenuItem> getMenuItemMap() {
        var allItems = menuItemRepository.findAll();
        var menuMap = new HashMap<String, MenuItem>();
        allItems.forEach(item -> menuMap.put(item.getId(), item));
        return menuMap;
    }

    public MenuItem findItem(String id) throws ExecutionException {
        return menuItemCache.get(id);
    }

    public MenuItem saveItems(MenuItem menuItem) {
        var savedItem = menuItemRepository.save(menuItem);
        menuItemCache.put(savedItem.getId(), savedItem);
        return savedItem;
    }

    public List<MenuItem> saveAllItems(List<MenuItem> menuItems) {
        var savedItems = menuItemRepository.saveAll(menuItems);
        menuItemCache.putAll(savedItems.stream()
                .collect(Collectors.toMap(MenuItem::getId, Function.identity())));
        return savedItems;
    }

    public MenuItem UpdateItem(String id, MenuItem menuItem) {
        MenuItem oldItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new MenuItemNotFoundException("Item not found with id: " + id));
        
        // Use ID for verification instead of name for better reliability
        if (id.equals(menuItem.getId())) {
            oldItem.setDescription(menuItem.getDescription());
            oldItem.setName(menuItem.getName());
            oldItem.setGrossPrice(menuItem.getGrossPrice());
            oldItem.setNetPrice(menuItem.getNetPrice());
            oldItem.setCategoryId(menuItem.getCategoryId());
            
            MenuItem updated = menuItemRepository.save(oldItem);
            menuItemCache.put(id, updated);
            return updated;
        } else {
            throw new MenuItemNotFoundException("Mismatching IDs during update");
        }
    }

    public void deleteItem(String id) {
        menuItemRepository.deleteById(id);
        menuItemCache.invalidate(id);
    }

    public BigDecimal getItemPrice(String id) {
        try {
            return menuItemCache.get(id).getGrossPrice();
        } catch (Exception e) {
            return BigDecimal.valueOf(-1);
        }
    }

    public int getItemType(String id) {
        try {
            return menuItemCache.get(id).getCategoryId();
        } catch (Exception e) {
            return -1;
        }
    }

    private MenuItem findByIdInternal(String id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new MenuItemNotFoundException("MenuItem not found: " + id));
    }

       // Get all categories
    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }

    // --- Category Actions (Now using String IDs) --

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
