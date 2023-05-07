package com.konstantinos.zito.services;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.konstantinos.zito.model.MenuItem;
import com.konstantinos.zito.model.MenuItemNotFoundException;
import com.konstantinos.zito.repositories.MenuItemRepository;

@Service
public class MenuItemService {

    final MenuItemRepository menuRepository;
    final LoadingCache<String, MenuItem> menuItemsSupplier;

    public MenuItemService(MenuItemRepository menuRepository) {
        this.menuRepository = menuRepository;
        menuItemsSupplier = CacheBuilder.newBuilder()
                .expireAfterAccess(12, TimeUnit.HOURS)
                .maximumSize(1000)
                .build(CacheLoader.from((name) -> {
                    return findByName(name);
                }));
    }

    // Get all menu items - will trigger a db call
    public List<MenuItem> findAll() {
        var allItems = menuRepository.findAll();
        allItems.forEach(item -> menuItemsSupplier.put(item.getName(), item));
        return allItems;
    }

    // Create a new menuItem
    public List<MenuItem> saveAll(List<MenuItem> menuItems) {
        var item = menuRepository.saveAll(menuItems);
        menuItemsSupplier.putAll(menuItems.stream().collect(Collectors.toMap(MenuItem::getName, Function.identity())));
        return item;
    }

    // Update a menuItem
    public MenuItem update(String name, MenuItem menuItem) {
        // Retrieve the MenuItem object with the given id from the database
        MenuItem oldItem = menuRepository.findByName(name).orElseThrow(MenuItemNotFoundException::new);
        if (name == menuItem.getName()) {
            oldItem.setDescription(menuItem.getDescription());
            oldItem.setName(menuItem.getName());
            oldItem.setPrice(menuItem.getPrice());
            oldItem.setType(menuItem.getType());
            menuItemsSupplier.put(menuItem.getName(), oldItem);
            return menuRepository.save(oldItem);
        } else {
            throw new MenuItemNotFoundException("mismatching names");
        }
    }

    // Delete a menuItem
    public void delete(String id) {
        menuRepository.deleteById(id);
    }

    // Find a menuItem by name
    public MenuItem getItem(String name) {
        try {
            return menuItemsSupplier.get(name);
        } catch (Exception e) {
            return null;
        }
    }

    // Find a menuItem by name
    public double getItemPrice(String name) {
        try {
            return menuItemsSupplier.get(name).getPrice();
        } catch (Exception e) {
            return -1L;
        }
    }

    // Find a menuItem by name
    private MenuItem findByName(String name) {
        Optional<MenuItem> menuItemOptional = menuRepository.findByName(name);
        if (menuItemOptional.isPresent()) {
            return menuItemOptional.get();
        } else {
            return new MenuItem();
        }
    }
}
