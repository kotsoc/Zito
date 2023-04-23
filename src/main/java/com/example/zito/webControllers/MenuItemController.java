package com.example.zito.webControllers;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.zito.model.MenuItem;
import com.example.zito.repositories.MenuItemRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/menuItems")
public class MenuItemController {

    // private OrderService orderService;

    @Autowired
    MenuItemRepository menuRepository;

    /*
     * Get all menu items
     */
    @GetMapping
    public ResponseEntity<List<MenuItem>> getMenuItems() {
        return ResponseEntity.ok(menuRepository.findAll());

    }

    /*
     * Create a new menuItem
     */
    @PostMapping
    public ResponseEntity<MenuItem> PostMenuItems(@Valid @RequestBody MenuItem item) {
        var res = menuRepository.save(item);
        if (res != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(res);
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }

    /*
     * Update a menuItem
     */
    @PutMapping("{id}")
    public ResponseEntity<MenuItem> UpdateMenuItem(@PathVariable String id, @RequestBody MenuItem item) {
        // Retrieve the MenuItem object with the given id from the database
        Optional<MenuItem> menuItemOptional = menuRepository.findById(id);

        // If the MenuItem object exists, update its properties and save it to the
        // database
        if (menuItemOptional.isPresent()) {
            MenuItem updatedMenuItem = menuRepository.save(item);
            return ResponseEntity.ok(updatedMenuItem);
        } else {
            // If the MenuItem object does not exist, return a 404 Not Found status
            return ResponseEntity.notFound().build();
        }
    }

    /*
     * Deletes a Menu item
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<MenuItem> DeleteMenuItem(@PathVariable String id) {
        menuRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
