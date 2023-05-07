package com.konstantinos.zito.services;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

import com.konstantinos.zito.model.MenuItem;
import com.konstantinos.zito.repositories.MenuItemRepository;

@Service
public class MenuItemService {

    final MenuItemRepository menuRepository;

    public MenuItemService(MenuItemRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    // Get all menu items
    public List<MenuItem> findAll() {
        return menuRepository.findAll();
    }

    // Create a new menuItem
    public MenuItem save(MenuItem menuItem) {
        return menuRepository.save(menuItem);
    }

    // Update a menuItem
    public MenuItem update(String id, MenuItem menuItem) {
        Optional<MenuItem> menuItemOptional = menuRepository.findById(id);
        if (menuItemOptional.isPresent()) {
            menuItem.setId(id);
            return menuRepository.save(menuItem);
        } else {
            return null;
        }
    }

    // Delete a menuItem
    public void delete(String id) {
        menuRepository.deleteById(id);
    }

    // Find a menuItem by ID
    public MenuItem findById(String id) {
        Optional<MenuItem> menuItemOptional = menuRepository.findById(id);
        if (menuItemOptional.isPresent()) {
            return menuItemOptional.get();
        } else {
            return null;
        }
    }
}
