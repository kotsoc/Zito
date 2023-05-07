package com.konstantinos.zito.webControllers;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.konstantinos.zito.model.MenuItem;
import com.konstantinos.zito.repositories.MenuItemRepository;
import com.konstantinos.zito.services.MenuItemService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/menuItems")
public class MenuItemController {

    final MenuItemService menuItemService;

    MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    /*
     * Get all menu items
     */
    @GetMapping
    public ResponseEntity<List<MenuItem>> getMenuItems() {
        return ResponseEntity.ok(menuItemService.findAll());
    }

    /*
     * Get all menu items
     */
    @GetMapping("/{name}")
    public ResponseEntity<MenuItem> getMenuItemByName(@PathVariable String name) {
        try {
            return ResponseEntity.ok(menuItemService.getItem(name));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /*
     * Create a new menuItem
     */
    @PostMapping
    public ResponseEntity<List<MenuItem>> PostMenuItems(@Valid @RequestBody List<MenuItem> item) {
        var res = menuItemService.saveAll(item);
        if (res != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(res);
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }

    /*
     * Update a menuItem
     */
    @PutMapping("/{name}")
    public ResponseEntity<MenuItem> UpdateMenuItem(@PathVariable String name, @RequestBody MenuItem item) {
        try {
            return ResponseEntity.ok(menuItemService.update(name, item));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /*
     * Deletes a Menu item
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<MenuItem> DeleteMenuItem(@PathVariable String id) {
        menuItemService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
