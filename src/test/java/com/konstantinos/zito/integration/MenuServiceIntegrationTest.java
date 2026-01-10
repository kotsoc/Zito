package com.konstantinos.zito.integration;

import com.konstantinos.zito.model.Category;
import com.konstantinos.zito.model.MenuItem;
import com.konstantinos.zito.repositories.CategoryRepository;
import com.konstantinos.zito.repositories.MenuItemRepository;
import com.konstantinos.zito.services.MenuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class MenuServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired private MenuService menuService;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private CategoryRepository categoryRepository;

    @BeforeEach
    void setup() {
        menuItemRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    // --- MenuItem Integration Tests ---

    @Test
    void shouldSaveAndRetrieveMenuItemViaCache() throws ExecutionException {
        // GIVEN
        String id = UUID.randomUUID().toString();
        MenuItem item = createMenuItem(id, "Classic Burger", "10.00", 1);

        // WHEN
        menuService.saveItems(item);
        
        // THEN: Verify DB state
        assertThat(menuItemRepository.existsById(id)).isTrue();

        // THEN: Verify Cache state and retrieval
        MenuItem cachedItem = menuService.findItem(id);
        assertThat(cachedItem.getName()).isEqualTo("Classic Burger");
        assertThat(menuService.getItemPrice(id)).isEqualByComparingTo("10.00");
    }

    @Test
    void shouldUpdateMenuItemAndSyncCache() throws ExecutionException {
        // GIVEN
        String id = UUID.randomUUID().toString();
        menuService.saveItems(createMenuItem(id, "Old Name", "5.00", 1));

        // WHEN
        MenuItem updatedData = createMenuItem(id, "Old Name", "7.50", 1);
        updatedData.setDescription("Updated Description");
        menuService.UpdateItem(id, updatedData);

        // THEN
        MenuItem result = menuService.findItem(id);
        assertThat(result.getGrossPrice()).isEqualByComparingTo("7.50");
        assertThat(result.getDescription()).isEqualTo("Updated Description");
    }

    @Test
    void shouldReturnDefaultValuesWhenItemNotFound() {
        String fakeId = "non-existent-id";
        
        assertThat(menuService.getItemPrice(fakeId)).isEqualByComparingTo("-1");
        assertThat(menuService.getItemType(fakeId)).isEqualTo(-1);
    }

    // --- Category Integration Tests ---

    @Test
    void shouldPerformFullCategoryLifecycle() {
        // GIVEN: Save
        Category category = new Category();
        category.setId(10); // Integer ID
        category.setName("Drinks");
        category.setDescription("Soft drinks and water");
        
        menuService.saveCategory(category);

        // WHEN: Find by Name
        Optional<Category> found = menuService.findCategory("Drinks");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(10);

        // WHEN: Update
        category.setDescription("Cold beverages");
        menuService.updateCategory("Drinks", category);

        // THEN: Verify Update
        assertThat(categoryRepository.findByName("Drinks").get().getDescription())
                .isEqualTo("Cold beverages");

        // WHEN: Delete
        menuService.deleteCategory(10);
        assertThat(menuService.findCategoryById(10)).isEmpty();
    }

    @Test
    void shouldReturnMenuItemMapKeyedById() {
        // GIVEN
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        menuService.saveAllItems(List.of(
            createMenuItem(id1, "Item 1", "1.00", 1),
            createMenuItem(id2, "Item 2", "2.00", 2)
        ));

        // WHEN
        Map<String, MenuItem> itemMap = menuService.getMenuItemMap();

        // THEN
        assertThat(itemMap).hasSize(2);
        assertThat(itemMap).containsKey(id1);
        assertThat(itemMap).containsKey(id2);
    }

    // --- Bulk Category Operations ---

    @Test
    void shouldSaveAndFindAllCategories() {
        // GIVEN
        Category cat1 = createCategory(1, "Starters", "Small plates");
        Category cat2 = createCategory(2, "Main Course", "Large plates");
        
        // WHEN
        menuService.saveAllCategories(List.of(cat1, cat2));
        List<Category> allCategories = menuService.findAllCategories();

        // THEN
        assertThat(allCategories).hasSize(2);
        assertThat(allCategories).extracting(Category::getName)
                .containsExactlyInAnyOrder("Starters", "Main Course");
    }

    @Test
    void shouldFindCategoryByVariousMethods() {
        // GIVEN
        Category cat = createCategory(5, "Wine", "Red and White");
        menuService.saveCategory(cat);

        // THEN: Find by ID (Integer)
        Optional<Category> byId = menuService.findCategoryById(5);
        assertThat(byId).isPresent();
        assertThat(byId.get().getName()).isEqualTo("Wine");

        // THEN: Find by Name (String)
        Optional<Category> byName = menuService.findCategory("Wine");
        assertThat(byName).isPresent();
        assertThat(byName.get().getId()).isEqualTo(5);
    }

    // --- MenuItem Integration (Ensuring ID consistency) ---

    @Test
    void shouldHandleBulkMenuItemSaveAndMap() {
        // GIVEN
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        
        List<MenuItem> items = List.of(
            createMenuItem(id1, "Espresso", "2.50", 1),
            createMenuItem(id2, "Cappuccino", "3.50", 1)
        );

        // WHEN
        menuService.saveAllItems(items);
        Map<String, MenuItem> itemMap = menuService.getMenuItemMap();

        // THEN
        assertThat(itemMap).hasSize(2);
        assertThat(itemMap.get(id1).getName()).isEqualTo("Espresso");
        assertThat(menuService.getItemPrice(id2)).isEqualByComparingTo("3.50");
    }

    @Test
    void shouldDeleteMenuItemAndInvalidateCache() throws ExecutionException {
        // GIVEN
        String id = UUID.randomUUID().toString();
        menuService.saveItems(createMenuItem(id, "Temp Item", "1.00", 1));
        
        // Ensure it's in cache
        assertThat(menuService.findItem(id)).isNotNull();

        // WHEN
        menuService.deleteItem(id);

        // THEN: Database is empty
        assertThat(menuItemRepository.existsById(id)).isFalse();
        
        // THEN: Price retrieval returns -1 (Cache was invalidated)
        assertThat(menuService.getItemPrice(id)).isEqualByComparingTo("-1");
    }

    // --- Helpers ---

    private Category createCategory(int id, String name, String desc) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        c.setDescription(desc);
        return c;
    }

    private MenuItem createMenuItem(String id, String name, String price, int catId) {
        MenuItem item = new MenuItem();
        item.setId(id);
        item.setName(name);
        item.setGrossPrice(new BigDecimal(price));
        item.setCategoryId(catId);
        item.setAvailable(true);
        return item;
    }
}