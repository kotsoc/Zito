package com.konstantinos.zito.webControllers;

import org.springframework.web.bind.annotation.RestController;

import com.konstantinos.zito.model.Category;
import com.konstantinos.zito.services.MenuService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/v1/categories")
public class CategoryController {
    
    final MenuService menuItemService;

    CategoryController(MenuService menuItemService) {
        this.menuItemService = menuItemService;
    }

    /*
     * Get all categories
     */
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok().body(menuItemService.findAllCategories());
    }

    /*
     * Get category by name
     */
    @GetMapping("/{name}")
    public ResponseEntity<Category> getCategoryByName(@PathVariable String name) {
        return menuItemService.findCategory(name)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /*
     * Get category by id
     */
    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable int id) {
        return menuItemService.findCategoryById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

     /*
     * Create a new category
     */
    @PostMapping
    public ResponseEntity<Category>PostCategory(@RequestBody Category category) {
        Category saved = menuItemService.saveCategory(category);
          if (saved != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }

    /*
     * Update a category
     */
    @PutMapping("/{name}")
    public ResponseEntity<Category> updateCategory(@PathVariable String name, @RequestBody Category category) {
        try {
            Category updated = menuItemService.updateCategory(name, category);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /*
     * Delete a category
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable int id) {
        menuItemService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}